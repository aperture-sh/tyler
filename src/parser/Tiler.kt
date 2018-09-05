package io.marauder.tyler.parser

import io.marauder.tyler.models.FeatureCollection
import io.marauder.tyler.models.Tile
import io.marauder.tyler.store.StoreClient
import kotlinx.coroutines.experimental.runBlocking
import java.util.*

class Tiler (val client: StoreClient, val minZoom: Int = 0, val maxZoom: Int = 5) {

    val BUFFER = 64
    val EXTENT = 4096

    val q: Queue<Tile> = LinkedList()
    var tileCount = 0

    /**
     * json file: ~500MB, 433280 Features (1 Geometry/Feature)
     * read, project, bbox: ~1min
     * clip, save to sqlite: ~3min
     */
    fun tiler(input: FeatureCollection) {
        println("start split: ${input.features.size} features")
        //TODO: wrap -> left + 1 (offset), right - 1 (offset)

        /*val buffer: Double = BUFFER.toDouble() / EXTENT
        val left = clip(input, 1.0, -1 -buffer, buffer, 0)
        val right = clip(input, 1.0, 1 -buffer, 2+buffer, 0)
        val merged = clip(input, 1.0, -buffer, 1+buffer, 0)*/

        q.offer(Tile(input, 0, 0, 0, 4096))
        ++tileCount


        while (q.isNotEmpty()) {
            ++tileCount
            if (tileCount % 250 == 0) {
                println("$tileCount tiles queued")
            }
            val t = q.poll()
            //TODO: to every split in a new coroutine and join them
            split(t.featureCollection, t.x, t.y, t.z)
        }
        println("finished split: ${input.features.size} features")
    }

    fun split(f: FeatureCollection, x: Int, y: Int, z: Int) {
        val z2 = 1 shl z
        if (z >= minZoom) {
            runBlocking {
                client.updateTile(x, y, z, createTileTransform(f, z, x, y))
            }
        }

        val k1 = 0.5 * BUFFER / EXTENT
        val k2 = 0.5 - k1
        val k3 = 0.5 + k1
        val k4 = 1 + k1
        if (z < maxZoom ) {

            val left = clip(f, z2.toDouble(), x - k1, x + k3, 0)
            val right = clip(f, z2.toDouble(), x + k2, x + k4, 0)

            var tl = FeatureCollection()
            var bl = FeatureCollection()
            var tr = FeatureCollection()
            var br = FeatureCollection()

            if (left.features.isNotEmpty()) {
                tl = clip(left, z2.toDouble(), y - k1, y + k3, 1)
                bl = clip(left, z2.toDouble(), y + k2, y + k4, 1)
            }

            if (right.features.isNotEmpty()) {
                tr = clip(right, z2.toDouble(), y - k1, y + k3, 1)
                br = clip(right, z2.toDouble(), y + k2, y + k4, 1)
            }


            if (tl.features.isNotEmpty()) q.offer(Tile(tl, z + 1, x * 2, y * 2, EXTENT))
            if (bl.features.isNotEmpty()) q.offer(Tile(bl, z + 1, x * 2, y * 2 + 1, EXTENT))
            if (tr.features.isNotEmpty()) q.offer(Tile(tr, z + 1, x * 2 + 1, y * 2, EXTENT))
            if (br.features.isNotEmpty()) q.offer(Tile(br, z + 1, x * 2 + 1, y * 2 + 1, EXTENT))
        }
    }
}