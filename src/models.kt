package io.marauder.tyler.models

data class FeatureCollection(val type: String = "FeatureCollection", val features: List<Feature>)
data class Feature(val type: String = "Feature",
                   val geometry: Geometry,
                   val properties: Map<String, Any> = emptyMap()
)
data class Geometry(val type: String, var coordinates: MutableList<List<List<Double>>>)

typealias BoundingBox = Pair<Pair<Double, Double>, Pair<Double, Double>>
