package io.marauder.tyler.models

data class FeatureCollection(val type: String = "FeatureCollection", val features: List<Feature> = emptyList())
data class Feature(val type: String = "Feature",
                   val geometry: Geometry,
                   val properties: Map<String, Any> = emptyMap()
)
data class Geometry(val type: String, var coordinates: MutableList<List<List<Double>>>)

data class Tile(val featureCollection: FeatureCollection, val z: Int, val x: Int, val y: Int, val extend: Int)

typealias BoundingBox = Pair<Pair<Double, Double>, Pair<Double, Double>>

fun toID(z: Int, x: Int, y: Int) = (((1 shl z) * y + x) * 32) + z;