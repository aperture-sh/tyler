package io.marauder.tyler.parser

import io.marauder.tyler.models.FeatureCollection
import io.marauder.tyler.store.StoreClient
import kotlinx.coroutines.*
import kotlin.math.pow

class Tiler2 (val client: StoreClient, val minZoom: Int = 0, val maxZoom: Int = 5) {

    val BUFFER = 64
    val EXTENT = 4096

    val CPUS = 4

    suspend fun tiler(input: FeatureCollection) {
        println("calculating bounding box: ${input.features.size} features")
        calcBbox(input)
        println("start split")

        //wrap -> left + 1 (offset), right - 1 (offset)
        /*val buffer: Double = BUFFER.toDouble() / EXTENT
        val left = clip(input, 1.0, -1 -buffer, buffer, 0)
        val right = clip(input, 1.0, 1 -buffer, 2+buffer, 0)
        val center = clip(input, 1.0, -buffer, 1+buffer, 0)*/

//        val merged = FeatureCollection(features = left.features + right.features + center.features)

//        split(input, 0, 0, 0).join()

        (minZoom..maxZoom).chunked(CPUS).forEach {
            println("\rzoom level in parallel: $it")
            val jobs = mutableListOf<Job>()
            it.forEach { z ->
//                traverseZoom(input, z)
                jobs.add(traverseZoom(input, z))
            }
            jobs.forEach { job -> job.join() }
        }
        println("\rfinished split: ${input.features.size} features")
    }

    fun traverseZoom(f: FeatureCollection, z: Int) = GlobalScope.launch {
        (0..(2.0.pow(z.toDouble()).toInt())).forEach { x ->
            val boundCheck = fcOutOfBounds(f, (1 shl z).toDouble(), (x).toDouble(), (1 + x).toDouble(), 0)

            if (boundCheck == 1) return@forEach
            if (boundCheck == 0)
                (0..(2.0.pow(z.toDouble()).toInt())).forEach { y ->
                    split(f, z, x, y)
                }
        }
    }

    fun split(f: FeatureCollection, z: Int, x: Int, y: Int) {
        val z2 = 1 shl (if (z == 0) 0 else z )

        val k1 = 0.5 * BUFFER / EXTENT
        val k3 = 1 + k1

        val vertical = clip(f, z2.toDouble(), y + k1, y + k3, 1)

        if (vertical.features.isNotEmpty()) {
            calcBbox(vertical)
            val horizontal = clip(f, z2.toDouble(), x - k1, x + k3, 0)

            if (horizontal.features.isNotEmpty()) {
                print("\rencode: $z/$x/$y")
//            println(vertical.features)

                runBlocking {
                    client.updateTile(x, y, z, horizontal)
                }
            }
        }

    }
}