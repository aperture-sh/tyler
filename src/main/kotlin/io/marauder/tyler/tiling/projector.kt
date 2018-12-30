package io.marauder.tyler.tiling

import io.marauder.models.Feature
import io.marauder.models.GeoJSON
import io.marauder.models.Geometry
import io.marauder.models.GeometryType
import io.marauder.tyler.models.BoundingBox
import io.marauder.tyler.models.Tile

fun projectFeatures(featureCollection: GeoJSON): GeoJSON =
        GeoJSON(featureCollection.type, featureCollection.features.map { f -> projectFeature(f) })


fun projectFeature(f: Feature): Feature {
    val geometry = when(f.geometry) {
        is Geometry.Point -> Geometry.Point(GeometryType.Point, (f.geometry as Geometry.Point).let { projectPoint(it.coordinates) })
        else -> TODO()
    }
    return Feature (
            type = f.type,
            properties = f.properties,
            geometry = geometry
    )
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

fun calcBbox(f: Feature) {
    when(f.geometry) {
        is Geometry.Point -> calcBbox(listOf((f.geometry as Geometry.Point).coordinates), f.bbox)
        else -> TODO()
    }
}

fun calcBbox(points: List<List<Double>>, bbox: MutableList<Double>) {
    points.forEach {p ->
        bbox[0] = Math.min(p[0], bbox[0])
        bbox[2] = Math.max(p[0], bbox[2])
        bbox[1] = Math.min(p[1], bbox[1])
        bbox[3] = Math.max(p[1], bbox[3])
    }
}

fun calcBbox(f: GeoJSON) {
    f.features.forEach {
        calcBbox(it)
        //TODO: needs bboxes per feature
        f.bbox[0] = Math.min(it.bbox[0], f.bbox[0])
        f.bbox[2] = Math.max(it.bbox[2], f.bbox[2])
        f.bbox[1] = Math.min(it.bbox[1], f.bbox[1])
        f.bbox[3] = Math.max(it.bbox[3], f.bbox[3])
    }
}

fun intersects(b1: BoundingBox, b2: BoundingBox): Boolean =
        (b1.first.first < b2.second.first && b1.second.first > b2.first.first && b1.second.second > b2.first.second && b1.first.second < b2.second.second)

fun includesPoints(b: BoundingBox, coords: List<List<Double>>): Boolean =
        coords.map { includesPoint(b, it) }.fold(true, { a,b -> a && b})

fun includesPoint(b: BoundingBox, coord: List<Double>): Boolean =
        coord[0] < b.second.first && coord[0] > b.first.first && coord[1] < b.second.second && coord[1] > b.first.second

fun transformTile(t: Tile) : Tile =
        Tile(GeoJSON(features = t.geojson.features.map {
            Feature(
                    it.id,
                    it.type,
                    transformGeometry(it.geometry, t.extend, t.z, t.x, t.y),
                    it.properties
            )
        }),
                t.z,
                t.x,
                t.y,
                t.extend
        )

fun transformGeometry(g: Geometry, extend: Int, z: Int, x: Int, y: Int) : Geometry = when(g) {
    is Geometry.Point -> Geometry.Point(g.type, transformPoint(g.coordinates, extend, z, x, y))
    else -> TODO()
}

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