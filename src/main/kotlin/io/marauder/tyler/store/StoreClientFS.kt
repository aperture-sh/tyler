package io.marauder.tyler.store

import io.marauder.charged.models.GeoJSON
import io.marauder.tyler.tiling.VT
import kotlinx.serialization.ImplicitReflectionSerializer
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.GZIPOutputStream

@ImplicitReflectionSerializer
class StoreClientFS(private val folder: String, private val vt: VT) : StoreClient {

    override fun setTile(x: Int, y: Int, z: Int, tile: String) {
        File("$folder/$z/$x/$y.json").writeBytes(tile.toByteArray())
    }

    override fun setTile(x: Int, y: Int, z: Int, tile: ByteArray) {
        File("$folder/$z/$x").mkdirs()
        val out = ByteArrayOutputStream()
        val gzip = GZIPOutputStream(out)
        gzip.write(tile)
        gzip.close()
        File("$folder/$z/$x/$y.mvt").writeBytes(out.toByteArray())
    }

    override fun getTile(x: Int, y: Int, z: Int): ByteArray? {
        val file = File("${folder}/$z/$x/$y.mvt")
        return if (file.exists()) file.readBytes() else null
    }

    override fun getTileJson(x: Int, y: Int, z: Int): String? {
        val file = File("${folder}/$z/$x/$y.json")
        return if (file.exists()) file.readText() else null
    }

    override suspend fun serveTile(x: Int, y: Int, z: Int, properties: List<String>, filter: List<List<Double>>): ByteArray? {
        return getTile(x, y, z)
    }

    override fun deleteTile(x: Int, y: Int, z: Int) {
        File("${folder}/$z/$x/$y.mvt").delete()
    }

    override fun updateTile(x: Int, y: Int, z: Int, tile: String, layer: String) {
        throw NotImplementedError("No update for tiles in String format")
    }

    override fun updateTile(x: Int, y: Int, z: Int, tile: ByteArray) {
        if (exists(x, y, z)) {
            val out = ByteArrayOutputStream()
            val gzip = GZIPOutputStream(out)
            gzip.write(vt.mergeTiles(checkNotNull(getTile(x, y, z)), tile))
            gzip.close()
            File("$folder/$z/$x/$y.mvt").writeBytes(out.toByteArray())
        } else {
            setTile(x, y, z, tile)
        }
    }

    override fun updateTile(x: Int, y: Int, z: Int, tile: GeoJSON, layer: String) {
        if (exists(x, y, z)) {
            val out = ByteArrayOutputStream()
            val gzip = GZIPOutputStream(out)
            gzip.write(vt.mergeTiles(checkNotNull(getTile(x, y, z)), tile, layer))
            gzip.close()
            File("$folder/$z/$x/$y.mvt").writeBytes(out.toByteArray())
        } else {
            setTile(x, y, z, vt.createTileTransform(tile, z, x, y, layer))
        }
    }

    override fun clearStore() {
        File(folder).deleteRecursively()
    }

    private fun exists(x: Int, y: Int, z: Int) : Boolean =
            File("$folder/$z/$x/$y.mvt").exists()
}