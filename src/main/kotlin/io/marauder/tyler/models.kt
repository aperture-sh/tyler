package io.marauder.tyler

import io.marauder.models.GeoJSON

data class Tile(val geojson: GeoJSON, val z: Int, val x: Int, val y: Int, val extend: Int)

typealias BoundingBox = Pair<Pair<Double, Double>, Pair<Double, Double>>

fun toID(z: Int, x: Int, y: Int) = (((1 shl z) * y + x) * 32) + z