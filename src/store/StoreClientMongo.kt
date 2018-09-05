package io.marauder.store

import com.mongodb.MongoClient
import com.mongodb.client.MongoDatabase
import com.mongodb.client.gridfs.GridFSBucket
import com.mongodb.client.gridfs.GridFSBuckets
import com.mongodb.client.model.Filters
import io.marauder.tyler.models.BoundingBox
import io.marauder.tyler.models.FeatureCollection
import io.marauder.tyler.models.toID
import io.marauder.tyler.parser.mergeTiles
import io.marauder.tyler.store.StoreClient
import kotlinx.coroutines.experimental.launch
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class StoreClientMongo(db: String, host: String = "localhost", port: Int = 27017) : StoreClient {

    private val database: MongoDatabase

    init {
        val mongo = MongoClient(host, port)
        database = mongo.getDatabase(db)
    }

    override fun setTile(x: Int, y: Int, z: Int, tile: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setTile(x: Int, y: Int, z: Int, tile: ByteArray) {
        val out = ByteArrayOutputStream()
        val gzip = GZIPOutputStream(out)

        gzip.write(tile)
        gzip.close()
        launch {
            val up = getGrid().openUploadStream(toID(z, x, y).toString())
            up.write(out.toByteArray())
            up.close()
        }
    }

    override fun getTile(x: Int, y: Int, z: Int): ByteArray? =
        GZIPInputStream(getGrid().openDownloadStream(toID(z, x, y).toString())).readBytes()

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
            gzip.write(mergeTiles(checkNotNull(getTile(x, y, z)), tile, z, x, y))
            gzip.close()
            launch {
                val up = getGrid().openUploadStream(toID(z, x, y).toString())
                up.write(out.toByteArray())
                up.close()
            }
        } else {
            setTile(x, y, z, tile)
        }
    }

    override fun updateTile(x: Int, y: Int, z: Int, tile: FeatureCollection) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    private fun getGrid() : GridFSBucket = GridFSBuckets.create(database)

    private fun exists(x: Int, y: Int, z: Int) : Boolean =
            getGrid().find(Filters.eq("filename", toID(z, x, y))).count() > 0

    override fun clearStore() {
        database.drop()
    }
}