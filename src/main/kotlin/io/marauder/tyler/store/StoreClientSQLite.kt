package io.marauder.tyler.store

import io.marauder.models.Feature
import io.marauder.models.GeoJSON
import io.marauder.tyler.BoundingBox
import io.marauder.tyler.tiling.VT
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.JSON
import kotlinx.serialization.parse
import java.io.ByteArrayOutputStream
import java.sql.Connection
import java.sql.DriverManager
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

@ImplicitReflectionSerializer
class StoreClientSQLite(db: String, private val vt: VT) : StoreClient {

    private var conn: Connection

    init {
        val url = "jdbc:sqlite:$db"
        conn = DriverManager.getConnection(url)
        createTable()
    }

    private fun createTable() {
        val sql = """
            CREATE TABLE IF NOT EXISTS tiles (zoom_level integer, tile_column integer, tile_row integer, tile_data blob, primary key (zoom_level, tile_column, tile_row));
            """
        val stmt = conn.createStatement()
        stmt.execute(sql)
        stmt.close()
    }

    override fun setTile(x: Int, y: Int, z: Int, tile: String) {
        val out = ByteArrayOutputStream()
        val gzip = GZIPOutputStream(out)
        gzip.write(tile.toByteArray())
        gzip.close()
        val sql = """
            INSERT INTO tiles(zoom_level, tile_column, tile_row, tile_data) VALUES (?, ?, ?, ?)
            """
        val stmt = conn.prepareStatement(sql)
        stmt.setInt(1, z)
        stmt.setInt(2, x)
        stmt.setInt(3, (1 shl z) -1 - y)
        stmt.setBytes(4, out.toByteArray())
        stmt.execute()
        stmt.close()
        out.close()
    }

    override fun setTile(x: Int, y: Int, z: Int, tile: ByteArray) {
        val out = ByteArrayOutputStream()
        val gzip = GZIPOutputStream(out)
        gzip.write(tile)
        gzip.close()
        val sql = """
            INSERT INTO tiles(zoom_level, tile_column, tile_row, tile_data) VALUES (?, ?, ?, ?)
            """
        val stmt = conn.prepareStatement(sql)
        stmt.setInt(1, z)
        stmt.setInt(2, x)
        stmt.setInt(3, (1 shl z) -1 - y)
        stmt.setBytes(4, out.toByteArray())
        stmt.execute()
        stmt.close()
        out.close()
    }

    override fun updateTile(x: Int, y: Int, z: Int, tile: String) {
        if (exists(x, y, z)) {
            val out = ByteArrayOutputStream()
            val gzip = GZIPOutputStream(out)
            gzip.write(vt.mergeTilesInject(checkNotNull(getTile(x, y, z)), JSON.plain.parse<GeoJSON>(tile)))
            gzip.close()
            val sql = """
                UPDATE tiles SET tile_data = ? WHERE zoom_level = '$z' AND tile_column = '$x' AND tile_row = '${(1 shl z) -1 - y}'
            """
            val stmt = conn.prepareStatement(sql)
            stmt.setBytes(1, out.toByteArray())
            stmt.execute()
            stmt.close()
        } else {
            setTile(x, y, z, tile)
        }
    }

    override fun updateTile(x: Int, y: Int, z: Int, tile: ByteArray) {
        if (exists(x, y, z)) {
            val out = ByteArrayOutputStream()
            val gzip = GZIPOutputStream(out)
            gzip.write(vt.mergeTilesInject(checkNotNull(getTile(x, y, z)), tile))
            gzip.close()
            val sql = """
                UPDATE tiles SET tile_data = ? WHERE zoom_level = '$z' AND tile_column = '$x' AND tile_row = '${(1 shl z) - 1 - y}'
            """
            val stmt = conn.prepareStatement(sql)
            stmt.setBytes(1, out.toByteArray())
            stmt.execute()
            stmt.close()
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
            val sql = """
                UPDATE tiles SET tile_data = ? WHERE zoom_level = '$z' AND tile_column = '$x' AND tile_row = '${(1 shl z) - 1 - y}'
            """
            val stmt = conn.prepareStatement(sql)
            stmt.setBytes(1, out.toByteArray())
            stmt.execute()
            stmt.close()
        } else {
            setTile(x, y, z, vt.createTileTransform(tile, z, x, y))
        }
    }

    override fun getTile(x: Int, y: Int, z: Int) : ByteArray? {
        val sql = """
            SELECT tile_data FROM tiles WHERE zoom_level = '$z' AND tile_column = '$x' AND tile_row = '${(1 shl z) -1 - y}'
            """
        val stmt = conn.createStatement()
        val rs = stmt.executeQuery(sql)
        if (rs != null && rs.next()) {
            val ins = rs.getBinaryStream(1)
            return GZIPInputStream(ins).readBytes()
        } else {
            return null
        }
    }

    override fun getTileJson(x: Int, y: Int, z: Int): String? {
        val sql = """
            SELECT tile_data FROM tiles WHERE zoom_level = '$z' AND tile_column = '$x' AND tile_row = '${(1 shl z) -1 - y}'
            """
        val stmt = conn.createStatement()
        val rs = stmt.executeQuery(sql)
        if (rs != null && rs.next()) {
            val ins = rs.getBinaryStream(1)
            val gzip = GZIPInputStream(ins)
            return gzip.readBytes().contentToString()
        } else {
            return null
        }
    }

    override suspend fun serveTile(x: Int, y: Int, z: Int, properties: List<String>, filter: List<BoundingBox>) : ByteArray? {
        val sql = """
            SELECT tile_data FROM tiles WHERE zoom_level = '$z' AND tile_column = '$x' AND tile_row = '${(1 shl z) -1 - y}'
            """
        val stmt = conn.createStatement()
        val rs = stmt.executeQuery(sql)
        if (rs != null && rs.next()) {
            return if (properties.isEmpty()) {
                getTile(x, y, z)
            } else {
                val features = vt.engine.decode(vt.engine.deserialize(getTile(x, y, z)!!)).map {
                    Feature(geometry = it.geometry, properties =  it.properties.filter { attr -> properties.contains(attr.key) })
                }

                val os = ByteArrayOutputStream()
                val gzip = GZIPOutputStream(os)
                gzip.write(vt.engine.encode(features, vt.layerName).toByteArray())
                gzip.close()

                os.toByteArray()
            }
        } else {
            return null
        }
    }

    override fun deleteTile(x: Int, y: Int, z: Int) {
        val sql = """
          DELETE FROM tiles WHERE zoom_level = '$z' AND tile_column = '$x' AND tile_row = '${(1 shl z) -1 - y}'
            """
        val stmt = conn.createStatement()
        stmt.execute(sql)
    }

    fun exists(x: Int, y: Int, z: Int) : Boolean {
        val sql = """
            SELECT 1 FROM tiles WHERE zoom_level = '$z' AND tile_column = '$x' AND tile_row = '${(1 shl z) -1 - y}'
            """
        val stmt = conn.createStatement()
        return checkNotNull(stmt.executeQuery(sql).next())
    }

    override fun clearStore() {
        val sql = """
            DELETE FROM tiles;
            """
        val stmt = conn.createStatement()
        stmt.execute(sql)
    }

}