package io.marauder.tyler

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.features.*
import org.slf4j.event.*
import java.time.*
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.marauder.supercharged.Projector
import io.marauder.supercharged.models.Feature
import io.marauder.supercharged.models.GeoJSON
import io.marauder.tyler.store.StoreClientFS
import io.marauder.tyler.tiling.Tyler
import io.marauder.tyler.store.StoreClientMongo
import io.marauder.tyler.store.StoreClientSQLite
import io.marauder.tyler.tiling.VT
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.JSON
import kotlinx.serialization.json.JsonParsingException
import kotlinx.serialization.parse
import java.io.File

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

    @ImplicitReflectionSerializer
    fun Application.module() {
        val minZoom = environment.config.propertyOrNull("ktor.application.min_zoom")?.getString()?.toInt() ?: 2
        val maxZoom = environment.config.propertyOrNull("ktor.application.max_zoom")?.getString()?.toInt() ?: 15
        val baseLayer = environment.config.propertyOrNull("ktor.application.base_layer")?.getString() ?: "io.marauder.tyler"
        val extend = environment.config.propertyOrNull("ktor.application.extend")?.getString()?.toInt() ?: 4096
        val buffer = environment.config.propertyOrNull("ktor.application.buffer")?.getString()?.toInt() ?: 64
        val chunkInsert = environment.config.propertyOrNull("ktor.application.chunk_insert")?.getString()?.toInt() ?: 500_000
        val maxInsert = environment.config.propertyOrNull("ktor.application.max_insert")?.getString()?.toInt() ?: Int.MAX_VALUE
        val threads = environment.config.propertyOrNull("ktor.application.threads")?.getString()?.toInt() ?: 2

        val vt = VT(extend, buffer, baseLayer)

        val store = when (environment.config.propertyOrNull("ktor.application.store.type")?.getString() ?: "sqlite") {
            "mongo" -> StoreClientMongo(
                    environment.config.propertyOrNull("ktor.application.store.mongo.db")?.getString() ?: "marauder",
                    environment.config.propertyOrNull("ktor.application.store.mongo.host")?.getString() ?: "localhost",
                    environment.config.propertyOrNull("ktor.application.store.mongo.port")?.getString()?.toInt()
                            ?: 27017,
                    vt
            )
            "fs" -> {
                val dir = environment.config.propertyOrNull("ktor.application.store.fs.folder")?.getString() ?: "./tree"
                File(dir).mkdirs()
                StoreClientFS(dir, vt)
            }
            // else take sqlite
            else -> StoreClientSQLite(environment.config.propertyOrNull("ktor.application.store.sqlite.db")?.getString()
                    ?: "./storage", vt)
        }

        install(Compression) {
            gzip {
                priority = 1.0
            }
            deflate {
                priority = 10.0
                minimumSize(1024) // condition
            }
        }

        install(CallLogging) {
            level = Level.INFO
            filter { call -> call.request.path().startsWith("/") }
        }

        install(DefaultHeaders) {
            header("X-Engine", "Ktor") // will send this header with each response
        }

        install(io.ktor.websocket.WebSockets) {
            pingPeriod = Duration.ofSeconds(15)
            timeout = Duration.ofSeconds(15)
            maxFrameSize = Long.MAX_VALUE
            masking = false
        }

        routing {
            post("/file") {
                val input = JSON.plain.parse<GeoJSON>(call.receiveText())

                GlobalScope.launch(newSingleThreadContext("tiling-process-1")) {
                    val projector = Projector()
                    val tyler = Tyler(store, minZoom, maxZoom, maxInsert, chunkInsert, threads, extend, buffer)
                    tyler.tiler(projector.projectFeatures(input))
                }

                call.respondText("Features Accepted", contentType = ContentType.Text.Plain, status = HttpStatusCode.Accepted)
            }

            post("/") {
                val features = mutableListOf<Feature>()
                call.receiveStream().bufferedReader().useLines { lines ->
                    lines.forEach { features.add(JSON.plain.parse(it)) }
                }
                val geojson = GeoJSON(features = features)
                GlobalScope.launch {
                    val projector = Projector()
                    val tyler = Tyler(store, minZoom, maxZoom, maxInsert, chunkInsert, threads, extend, buffer)
                    val neu = projector.projectFeatures(geojson)
                    tyler.tiler(neu)
                }

                call.respondText("Features Accepted", contentType = ContentType.Text.Plain, status = HttpStatusCode.Accepted)
            }

            get("/{z}/{x}/{y_type}") {
                val y_list = call.parameters["y_type"]!!.split('.')

                call.respondBytes(contentType = ContentType.Application.GZip) {
                    store.serveTile(call.parameters["x"]!!.toInt(), y_list[0].toInt(), call.parameters["z"]!!.toInt())
                            ?: ByteArray(0)
                }
            }

            delete("/") {
                GlobalScope.launch {
                    store.clearStore()
                }

                call.respondText("clearing store started", contentType = ContentType.Text.Plain, status = HttpStatusCode.Accepted)
            }

            static("/static") {
                resources("static")
            }

            install(StatusPages) {
                exception<OutOfMemoryError> {
                    call.respond(status = HttpStatusCode.InternalServerError, message = "Out of memory: reduce file size")
                }

                exception<JsonParsingException> {
                    call.respond(status = HttpStatusCode.InternalServerError, message = "Json Parsing Issue: Check file format")
                }

            }
        }
    }
