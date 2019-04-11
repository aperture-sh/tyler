package io.marauder.tyler.store

import com.mongodb.MongoClient
import com.mongodb.client.MongoDatabase
import com.mongodb.client.gridfs.GridFSBucket
import com.mongodb.client.gridfs.GridFSBuckets
import com.mongodb.client.model.Filters
import io.marauder.supercharged.models.GeoJSON
import io.marauder.supercharged.models.Tile
import io.marauder.tyler.tiling.VT
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.ImplicitReflectionSerializer
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

@ImplicitReflectionSerializer
class StoreClientMongo(db: String, host: String = "localhost", port: Int = 27017, private val vt: VT) : StoreClient {

    private val database: MongoDatabase

    init {
        val mongo = MongoClient(host, port)
        database = mongo.getDatabase(db)
    }

    override fun setTile(x: Int, y: Int, z: Int, tile: String) {
        val up = getGrid().openUploadStream(Tile.toID(z, x, y).toString())
        up.write(tile.toByteArray())
        up.close()
    }

    override fun setTile(x: Int, y: Int, z: Int, tile: ByteArray) {
        val out = ByteArrayOutputStream()
        val gzip = GZIPOutputStream(out)

        gzip.write(tile)
        gzip.close()
        val up = getGrid().openUploadStream(Tile.toID(z, x, y).toString())
        up.write(out.toByteArray())
        up.close()
    }

    override fun getTile(x: Int, y: Int, z: Int): ByteArray? =
        GZIPInputStream(getGrid().openDownloadStream(Tile.toID(z, x, y).toString())).readBytes()

    override fun getTileJson(x: Int, y: Int, z: Int) =
        GZIPInputStream(getGrid().openDownloadStream(Tile.toID(z, x, y).toString())).bufferedReader().use { it.readText() }

    override suspend fun serveTile(x: Int, y: Int, z: Int, properties: List<String>, filter: List<List<Double>>): ByteArray? {
        return if (exists(x, y, z)) getTile(x, y, z) else null
    }

    override fun deleteTile(x: Int, y: Int, z: Int) {
        setTile(x, y, z, ByteArray(0))
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
            GlobalScope.launch {
                val up = getGrid().openUploadStream(Tile.toID(z, x, y).toString())
                up.write(out.toByteArray())
                up.close()
            }
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
            val up = getGrid().openUploadStream(Tile.toID(z, x, y).toString())
            up.write(out.toByteArray())
            up.close()
        } else {
            setTile(x, y, z, vt.createTileTransform(tile, z, x, y, layer))
        }
    }


    private fun getGrid() : GridFSBucket = GridFSBuckets.create(database)

    private fun exists(x: Int, y: Int, z: Int) : Boolean =
            getGrid().find(Filters.eq("filename", Tile.toID(z, x, y))).count() > 0

    override fun clearStore() {
        database.drop()
    }
}
