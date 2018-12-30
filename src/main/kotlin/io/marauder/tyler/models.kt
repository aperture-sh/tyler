package io.marauder.tyler.models

import io.marauder.models.GeoJSON

data class FeatureCollection(val type: String = "FeatureCollection",
                             val features: List<Feature> = emptyList(),
                             val bbox: MutableList<Double> = mutableListOf(Double.MAX_VALUE, Double.MAX_VALUE, Double.MIN_VALUE, Double.MIN_VALUE)
)
data class Feature(val type: String = "Feature",
                   val geometry: Geometry,
                   val properties: Map<String, Any> = emptyMap(),
                   val bbox: MutableList<Double> = mutableListOf(Double.MAX_VALUE, Double.MAX_VALUE, Double.MIN_VALUE, Double.MIN_VALUE)
)
data class Geometry(val type: String, var coordinates: MutableList<List<List<Double>>>)

data class Tile(val featureCollection: FeatureCollection, val z: Int, val x: Int, val y: Int, val extend: Int)

typealias BoundingBox = Pair<Pair<Double, Double>, Pair<Double, Double>>

fun toID(z: Int, x: Int, y: Int) = (((1 shl z) * y + x) * 32) + z