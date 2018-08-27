package io.marauder.tyler.parser

import io.marauder.tyler.models.*

fun projectFeatures(featureCollection: FeatureCollection): FeatureCollection =
        FeatureCollection(featureCollection.type, featureCollection.features.map { f -> projectFeature(f) })


fun projectFeature(f: Feature): Feature {
    f.geometry.coordinates[0] = f.geometry.coordinates[0].map { p -> projectPoint(p) }
    return calcBbox(f)
}

fun projectPoint(p: List<Double>): List<Double> {
    val sin = Math.sin(p[1] * Math.PI / 180)
    val x = p[0] / 360 + 0.5
    var y = (0.5 - 0.25 * Math.log((1 + sin) / (1 - sin)) / Math.PI)

    y = when {
        y < 0 -> 0.0
        y > 1 -> 1.0
        else -> y
    }
    return listOf(x, y)
}

fun calcBbox(f: Feature): Feature {
     f.geometry.coordinates.forEach { p ->
         //TODO: find a way to store bbox
         /*f.min[0] = Math.min(p[0], f.min[0])
         f.max[0] = Math.max(p[0], f.max[0])
         f.min[1] = Math.min(p[1], f.min[1])
         f.max[1] = Math.max(p[1], f.max[1])*/
     }
    return f
}

fun calcBbox(f: List<Feature>): BoundingBox {
    val bbox = Pair(Pair(Double.MAX_VALUE, Double.MAX_VALUE), Pair(Double.MIN_VALUE, Double.MIN_VALUE))
    f.forEach {
        //TODO: needs bboxes per feature
        /*bbox.min[0] = Math.min(it.min[0], bbox.min[0])
        bbox.max[0] = Math.max(it.max[0], bbox.max[0])
        bbox.min[1] = Math.min(it.min[1], bbox.min[1])
        bbox.max[1] = Math.max(it.max[1], bbox.max[1])*/
    }
    return bbox
}

fun intersects(b1: BoundingBox, b2: BoundingBox): Boolean =
        (b1.first.first < b2.second.first && b1.second.first > b2.first.first && b1.second.second > b2.first.second && b1.first.second < b2.second.second)

fun includesPoints(b: BoundingBox, coords: List<List<Double>>): Boolean =
        coords.map { includesPoint(b, it) }.fold(true, { a,b -> a && b})

fun includesPoint(b: BoundingBox, coord: List<Double>): Boolean =
        coord[0] < b.second.first && coord[0] > b.first.first && coord[1] < b.second.second && coord[1] > b.first.second

fun transformTile(t: Tile) : Tile =
        Tile(FeatureCollection(features = t.featureCollection.features.map {
            Feature(
                    it.type,
                    Geometry(
                            it.geometry.type,
                            mutableListOf(it.geometry.coordinates[0].map { transformPoint(it, t.extend, t.z, t.x, t.y) })
                    ),
                    it.properties
            )
        }),
                t.z,
                t.x,
                t.y,
                t.extend
        )

fun transformPoint(p: List<Double>, extend: Int, z: Int, x: Int, y: Int) : List<Double> =
        listOf(Math.round(extend * (p[0] * z - x)).toDouble(), Math.round(extend * (p[1] * z - y)).toDouble())

fun transformBBox(z: Int, x: Int, y: Int, extend: Int, bbox: BoundingBox) : BoundingBox =
        BoundingBox(
                transformPoint(projectPoint(mutableListOf(bbox.first.first, bbox.second.second)), extend, 1 shl z, x, y).zipWithNext().first(),
                transformPoint(projectPoint(mutableListOf(bbox.second.first, bbox.first.second)), extend, 1 shl z, x, y).zipWithNext().first()
        )

fun tileBBox(z: Int, x: Int, y: Int) = BoundingBox(Pair(tileToLon(x, z), tileToLat( y+1, z)), Pair(tileToLon(x+1, z), tileToLat(y, z)))

fun tileToLon(x: Int, z: Int) = x.toDouble() / Math.pow(2.0, z.toDouble()) * 360.0 - 180.0

fun tileToLat(y: Int, z: Int) = Math.toDegrees(Math.atan(Math.sinh(Math.PI - (2.0 * Math.PI * y.toDouble()) / Math.pow(2.0, z.toDouble()))))