package io.marauder.store

import io.marauder.tyler.models.BoundingBox
import io.marauder.tyler.models.FeatureCollection
import io.marauder.tyler.store.StoreClient
import java.io.File

class StoreClientFS(val folder: String) : StoreClient {

    init {

    }

    override fun setTile(x: Int, y: Int, z: Int, tile: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setTile(x: Int, y: Int, z: Int, tile: ByteArray) {
        File("$folder/$z/$x").mkdirs()
        File("$folder/$z/$x/$y.mvt").writeBytes(tile)
    }

    override fun getTile(x: Int, y: Int, z: Int): ByteArray? {
        val file = File("${folder}/$z/$x/$y.mvt")
        return file.readBytes()
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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun updateTile(x: Int, y: Int, z: Int, tile: FeatureCollection) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun clearStore() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}