package io.marauder.tyler.store

import com.mongodb.MongoClient
import com.mongodb.client.MongoDatabase
import com.mongodb.client.gridfs.GridFSBucket
import com.mongodb.client.gridfs.GridFSBuckets
import com.mongodb.client.model.Filters
import io.marauder.models.GeoJSON
import io.marauder.tyler.BoundingBox
import io.marauder.tyler.tiling.VT
import io.marauder.tyler.toID
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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setTile(x: Int, y: Int, z: Int, tile: ByteArray) {
        val out = ByteArrayOutputStream()
        val gzip = GZIPOutputStream(out)

        gzip.write(tile)
        gzip.close()
//        GlobalScope.launch {
            val up = getGrid().openUploadStream(toID(z, x, y).toString())
            up.write(out.toByteArray())
            up.close()
//        }
    }

    override fun getTile(x: Int, y: Int, z: Int): ByteArray? =
        GZIPInputStream(getGrid().openDownloadStream(toID(z, x, y).toString())).readBytes()

    override fun getTileJson(x: Int, y: Int, z: Int): String? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun serveTile(x: Int, y: Int, z: Int, properties: List<String>, filter: List<BoundingBox>): ByteArray? {
        return if (exists(x, y, z)) getTile(x, y, z) else null
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
            gzip.write(vt.mergeTilesInject(checkNotNull(getTile(x, y, z)), tile))
            gzip.close()
            GlobalScope.launch {
                val up = getGrid().openUploadStream(toID(z, x, y).toString())
                up.write(out.toByteArray())
                up.close()
            }
        } else {
            setTile(x, y, z, tile)
        }
    }

    override fun updateTile(x: Int, y: Int, z: Int, tile: GeoJSON) {
        if (exists(x, y, z)) {
            val out = ByteArrayOutputStream()
            val gzip = GZIPOutputStream(out)
            gzip.write(vt.mergeTilesInject(checkNotNull(getTile(x, y, z)), tile))
            gzip.close()
//            GlobalScope.launch {
                val up = getGrid().openUploadStream(toID(z, x, y).toString())
                up.write(out.toByteArray())
                up.close()
//            }
        } else {
            setTile(x, y, z, vt.createTileTransform(tile, z, x, y))
        }
    }


    private fun getGrid() : GridFSBucket = GridFSBuckets.create(database)

    private fun exists(x: Int, y: Int, z: Int) : Boolean =
            getGrid().find(Filters.eq("filename", toID(z, x, y))).count() > 0

    override fun clearStore() {
        database.drop()
    }
}
