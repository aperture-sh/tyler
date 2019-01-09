package io.marauder.tyler.store

import io.marauder.models.GeoJSON
import io.marauder.tyler.BoundingBox
import io.marauder.tyler.tiling.createTileTransform
import io.marauder.tyler.tiling.mergeTilesInject
import kotlinx.serialization.ImplicitReflectionSerializer
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.GZIPOutputStream

@ImplicitReflectionSerializer
class StoreClientFS(val folder: String) : StoreClient {

    init {

    }

    override fun setTile(x: Int, y: Int, z: Int, tile: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun serveTile(x: Int, y: Int, z: Int, properties: List<String>, filter: List<BoundingBox>): ByteArray? {
        return getTile(x, y, z)
    }

    override fun deleteTile(x: Int, y: Int, z: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun updateTile(x: Int, y: Int, z: Int, tile: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun updateTile(x: Int, y: Int, z: Int, tile: ByteArray) {
        if (exists(x, y, z)) {
            val out = ByteArrayOutputStream()
            val gzip = GZIPOutputStream(out)
            gzip.write(mergeTilesInject(checkNotNull(getTile(x, y, z)), tile))
            gzip.close()
            File("$folder/$z/$x/$y.mvt").writeBytes(out.toByteArray())
        } else {
            setTile(x, y, z, tile)
        }
    }

    override fun updateTile(x: Int, y: Int, z: Int, tile: GeoJSON) {
        if (exists(x, y, z)) {
            val out = ByteArrayOutputStream()
            val gzip = GZIPOutputStream(out)
            gzip.write(mergeTilesInject(checkNotNull(getTile(x, y, z)), tile))
            gzip.close()
            File("$folder/$z/$x/$y.mvt").writeBytes(out.toByteArray())
        } else {
            setTile(x, y, z, createTileTransform(tile, z, x, y))
        }
    }

    override fun clearStore() {
        File(folder).deleteRecursively()
    }

    private fun exists(x: Int, y: Int, z: Int) : Boolean =
            File("$folder/$z/$x/$y.mvt").exists()
}