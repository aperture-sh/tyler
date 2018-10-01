package io.marauder.tyler.parser

import io.marauder.tyler.models.FeatureCollection
import io.marauder.tyler.store.StoreClient
import kotlinx.coroutines.*

class Tiler (val client: StoreClient, val minZoom: Int = 0, val maxZoom: Int = 5) {

    val BUFFER = 64
    val EXTENT = 4096

    suspend fun tiler(input: FeatureCollection) {
        println("start split: ${input.features.size} features")

        //wrap -> left + 1 (offset), right - 1 (offset)
        val buffer: Double = BUFFER.toDouble() / EXTENT
        val left = clip(input, 1.0, -1 -buffer, buffer, 0)
        val right = clip(input, 1.0, 1 -buffer, 2+buffer, 0)
        val center = clip(input, 1.0, -buffer, 1+buffer, 0)

        val merged = FeatureCollection(features = left.features + right.features + center.features)

        split(merged, 0, 0, 0).join()

        println("finished split: ${input.features.size} features")
    }

    fun split(f: FeatureCollection, z: Int, x: Int, y: Int): Job = GlobalScope.launch {
        val z2 = 1 shl z
        if (z >= minZoom) {
            runBlocking {
                client.updateTile(x, y, z, f)
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

            val jobs = mutableListOf<Job>()
            if (tl.features.isNotEmpty()) jobs.add(split(tl, z + 1, x * 2, y * 2))
            if (bl.features.isNotEmpty()) jobs.add(split(bl, z + 1, x * 2, y * 2 + 1))
            if (tr.features.isNotEmpty()) jobs.add(split(tr, z + 1, x * 2 + 1, y * 2))
            if (br.features.isNotEmpty()) jobs.add(split(br, z + 1, x * 2 + 1, y * 2 + 1))

            jobs.forEach { it.join() }

        }
    }
}