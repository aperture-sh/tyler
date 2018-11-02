package io.marauder.tyler

import com.google.gson.Gson
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.content.*
import io.ktor.locations.*
import io.ktor.features.*
import org.slf4j.event.*
import java.time.*
import io.ktor.gson.*
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.marauder.store.StoreClientFS
import io.marauder.store.StoreClientMongo
import io.marauder.tyler.models.FeatureCollection
import io.marauder.tyler.parser.Tiler
import io.marauder.tyler.parser.Tiler2
import io.marauder.tyler.parser.projectFeatures
import io.marauder.tyler.store.StoreClient
import io.marauder.tyler.store.StoreClientSQLite
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import java.io.File
import java.io.InputStreamReader
import kotlin.system.measureTimeMillis

fun main(args: Array<String>): Unit = io.ktor.server.netty.DevelopmentEngine.main(args)

/*fun main(args: Array<String>) {
    //io.ktor.server.netty.main(args) // Manually using Netty's DevelopmentEngine
    embeddedServer(
            Netty, watchPaths = listOf("tyler"), port = 8080,
            module = Application::module
    ).start(wait = true)
    }*/

    lateinit var store: StoreClient;

    fun Application.module() {
        when (environment.config.propertyOrNull("ktor.application.store.type")?.getString() ?: "sqlite") {
            "sqlite" -> store = StoreClientSQLite(environment.config.propertyOrNull("ktor.application.store.sqlite.db")?.getString()
                    ?: "./storage")
            "mongo" -> store = StoreClientMongo(
                    environment.config.propertyOrNull("ktor.application.store.mongo.db")?.getString() ?: "marauder",
                    environment.config.propertyOrNull("ktor.application.store.mongo.host")?.getString() ?: "localhost",
                    environment.config.propertyOrNull("ktor.application.store.mongo.port")?.getString()?.toInt()
                            ?: 27017
            )
            "fs" -> {
                val dir = environment.config.propertyOrNull("ktor.application.store.fs.folder")?.getString() ?: "./tree"
                File(dir).mkdirs()
                store = StoreClientFS(dir)
            }
        }

//    install(Locations) {
//    }

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

        install(DataConversion)

        install(DefaultHeaders) {
            header("X-Engine", "Ktor") // will send this header with each response
        }

        install(io.ktor.websocket.WebSockets) {
            pingPeriod = Duration.ofSeconds(15)
            timeout = Duration.ofSeconds(15)
            maxFrameSize = Long.MAX_VALUE
            masking = false
        }

        install(ContentNegotiation) {
            gson {
            }
        }


        routing {
            post("/") {
                val input = Gson().fromJson(InputStreamReader(call.receiveStream()), FeatureCollection::class.java)
                GlobalScope.launch(newSingleThreadContext("tiling-process-1")) {
                    //TODO: start tiling in independent thread
                    if (call.parameters["clear"] != null) {
                        println("clear")
                        store.clearStore()
                    }


                    val time = measureTimeMillis {
                            val tyler = Tiler2(store, 0, 15)
                            tyler.tiler(projectFeatures(input))
                    }
                    println("time: $time")
                }

                call.respondText("tiling started", contentType = ContentType.Text.Plain)
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

                call.respondText("clearing store started", contentType = ContentType.Text.Plain)
            }


            static("/static") {
                resources("static")
            }

            install(StatusPages) {
                exception<AuthenticationException> { _ ->
                    call.respond(HttpStatusCode.Unauthorized)
                }
                exception<AuthorizationException> { _ ->
                    call.respond(HttpStatusCode.Forbidden)
                }

            }

            /*webSocket("/myws/echo") {
            send(Frame.Text("Hi from server"))
            while (true) {
                val frame = incoming.receive()
                if (frame is Frame.Text) {
                    send(Frame.Text("Client said: " + frame.readText()))
                }
            }
        }*/


        }
    }

    class AuthenticationException : RuntimeException()
    class AuthorizationException : RuntimeException()
