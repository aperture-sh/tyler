package io.marauder.tyler.models

data class Tile(val featureCollection: FeatureCollection, val extend: Int, val z: Int, val x: Int, val y: Int)

data class FeatureCollection(val type: String = "FeatureCollection", val features: List<Feature>)
data class Feature(val type: String = "Feature",
                   val geometry: Geometry,
                   val properties: Map<String, Any> = emptyMap()
)
data class Geometry(val type: String, var coordinates: List<List<List<Double>>>)
