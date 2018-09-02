package io.marauder.tyler.parser

import io.marauder.tyler.models.Feature
import io.marauder.tyler.models.FeatureCollection
import io.marauder.tyler.models.Geometry

fun clip(f: FeatureCollection, scale: Double, k1: Double, k2: Double, axis: Int) : FeatureCollection =
    FeatureCollection(features =
        f.features.filter { f ->
        val scaleK2 = k2 / scale
        val scaleK1 = k1 / scale
//        val min = f.min[axis]
//        val max = f.max[axis]
//        condition for trivia reject
//        !(min > scaleK2 || max < scaleK1)
        //TODO: reject when complete collection or complete features bboxes are out of bounds
        true
    }.flatMap { f ->
        if (f.geometry.coordinates.isEmpty()) {
            listOf()
            //TODO: accept everyhing when bbox matches all
            //condition for trivia accept
//        } else if (min >= scaleK1 && max <= scaleK2) {
//            listOf(f)
        } else {
            //TODO: adapt min/max during clipping
            val geom = clipGeometry(f.geometry, k1 / scale, k2 / scale, axis)
            if (geom.coordinates[0][0].isNotEmpty()) {
                listOf(Feature(
                        f.type,
                        geom,
                        f.properties
                )
                )
            } else {
                listOf()
            }

        }
    }
    )

fun clipGeometry(g: Geometry, k1: Double, k2: Double, axis: Int): Geometry {
    val slice = mutableListOf<List<Double>>()
    end@for(i in g.coordinates[0].indices) {
        if (i >= g.coordinates[0].size - 1) {
            break@end
        }
        if (g.coordinates[0][i][axis] < k1) {
            if (g.coordinates[0][i+1][axis] > k2) {
                slice.addAll(listOf(intersect(g.coordinates[0][i], g.coordinates[0][i + 1], k1, axis), intersect(g.coordinates[0][i], g.coordinates[0][i + 1], k2, axis)))
                // ---|-----|-->
            } else if (g.coordinates[0][i+1][axis] >= k1) {
                slice.add(intersect(g.coordinates[0][i], g.coordinates[0][i + 1], k1, axis))
                // ---|-->  |
            }

        } else if (g.coordinates[0][i][axis] > k2) {
            if (g.coordinates[0][i+1][axis] < k1) {
                slice.addAll(listOf(intersect(g.coordinates[0][i], g.coordinates[0][i + 1], k2, axis), intersect(g.coordinates[0][i], g.coordinates[0][i + 1], k1, axis)))
                // <--|-----|---
            } else if (g.coordinates[0][i+1][axis] <= k2) {
                slice.add(intersect(g.coordinates[0][i], g.coordinates[0][i + 1], k2, axis))
                // |  <--|---
            }
        } else {
            slice.add(g.coordinates[0][i])
            if (g.coordinates[0][i+1][axis] < k1) {
                slice.add(intersect(g.coordinates[0][i], g.coordinates[0][i + 1], k1, axis))
                // <--|---  |
            } else if (g.coordinates[0][i+1][axis] > k2) {
                slice.add(intersect(g.coordinates[0][i], g.coordinates[0][i + 1], k2, axis))
                // |  ---|-->
            }
            // | --> |
        }

    }


    val a = g.coordinates[0].last()
    if (a[axis] in k1..k2) slice.add(a)
    if (slice.isNotEmpty() && (slice[0][0] != slice.last()[0] || slice[0][1] != slice.last()[1]) && (g.type == "Polygon" || g.type == "MultiPolygon")) {
        slice.add(slice[0])
    }
    //TODO: somehow linear rings with < 4 points are created
    if (slice.size < 4) {
        return Geometry(g.type, mutableListOf(listOf(emptyList())))
    }

    return Geometry(g.type, mutableListOf(slice))
}

fun intersect(a: List<Double>, b: List<Double>, clip: Double, axis: Int): List<Double> =
    when (axis) {
        0 -> listOf(clip, (clip - a[0]) * (b[1] - a[1]) / (b[0] - a[0]) + a[1])
        else -> listOf((clip - a[1]) * (b[0] - a[0]) / (b[1] - a[1]) + a[0], clip)
    }
