package io.marauder.tyler.tiling

import io.marauder.supercharged.Clipper
import io.marauder.supercharged.Intersector
import io.marauder.supercharged.Projector
import io.marauder.supercharged.models.GeoJSON
import io.marauder.tyler.store.StoreClient
import kotlinx.coroutines.*
import kotlin.math.pow

class Tiler(
        private val client: StoreClient,
        private val minZoom: Int = 2,
        private val maxZoom: Int = 15,
        private val maxInsert: Int = 500000,
        private val chunkInsert: Int = 250000,
        private val threads: Int = 2,
        private val extend: Int = 4096,
        private val buffer: Int = 64) {

    private val projector = Projector()
    private val intersector = Intersector()
    private val clipper = Clipper()

    suspend fun tiler(input: GeoJSON) {

        input.features.take(maxInsert).chunked(chunkInsert).forEach {
            val bulk = GeoJSON(features = it)

            println("calculating bounding box: ${bulk.features.size} features")
            projector.calcBbox(bulk)
            println("start split")

            //TODO: wrap geometries at 180 degree
            //wrap -> left + 1 (offset), right - 1 (offset)
            /*val buffer: Double = BUFFER.toDouble() / EXTENT
        val left = clip(input, 1.0, -1 -buffer, buffer, 0)
        val right = clip(input, 1.0, 1 -buffer, 2+buffer, 0)
        val center = clip(input, 1.0, -buffer, 1+buffer, 0)*/

//        val merged = FeatureCollection(features = left.features + right.features + center.features)

            (minZoom..maxZoom).chunked(threads).forEach { zoomLvL ->
                println("\rzoom level in parallel: $zoomLvL")
                val jobs = mutableListOf<Job>()
                zoomLvL.forEach { z ->
                    jobs.add(traverseZoom(bulk, z))
                }
                jobs.forEach { job -> job.join() }
            }
            println("\rfinished split: ${bulk.features.size} features")
        }
    }

    private fun traverseZoom(f: GeoJSON, z: Int) = GlobalScope.launch {
        (0..(2.0.pow(z.toDouble()).toInt())).forEach { x ->
            val boundCheck = intersector.fcOutOfBounds(f, (1 shl z).toDouble(), (x).toDouble(), (1 + x).toDouble(), 0)

            if (boundCheck == 1) return@forEach
            if (boundCheck == 0)
                (0..(2.0.pow(z.toDouble()).toInt())).forEach { y ->
                    split(f, z, x, y)
                }
        }
    }

    private fun split(f: GeoJSON, z: Int, x: Int, y: Int) {
        val z2 = 1 shl (if (z == 0) 0 else z)

        val k1 = 0.5 * buffer / extend
        val k3 = 1 + k1

        val clipped = clipper.clip(f, z2.toDouble(), x - k1, x + k3, y - k1, y + k3)

        if (clipped.features.isNotEmpty()) {
            projector.calcBbox(clipped)
            print("\rencode: $z/$x/$y")

            runBlocking {
                client.updateTile(x, y, z, clipped)
            }
        }
    }


}
