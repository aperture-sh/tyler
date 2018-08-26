package io.marauder.tyler.parser

import io.marauder.tyler.models.FeatureCollection
import no.ecc.vectortile.VectorTileDecoder
import no.ecc.vectortile.VectorTileEncoder


val gf = org.locationtech.jts.geom.GeometryFactory()

fun createTile(featureCollection: FeatureCollection, z: Int, x: Int, y: Int): ByteArray {
    val encoder = VectorTileEncoder(4096, 8, false)

    transformTile(
            Tile(features, 4096, x, y, 1 shl z)
    ).features.map {
        val geom = when (it.geom.type) {
            "Polygon" ->  gf.createPolygon(it.geom.coords.map { org.locationtech.jts.geom.Coordinate(it[0], it[1]) }.toTypedArray())
            "MultiPolygon" -> gf.createPolygon(it.geom.coords.map { org.locationtech.jts.geom.Coordinate(it[0], it[1]) }.toTypedArray())
            "Point" -> gf.createPoint(it.geom.coords.map { org.locationtech.jts.geom.Coordinate(it[0], it[1]) }[0])
            "LineString" -> gf.createLineString(it.geom.coords.map { org.locationtech.jts.geom.Coordinate(it[0], it[1]) }.toTypedArray())
            else ->  gf.createPolygon(it.geom.coords.map { org.locationtech.jts.geom.Coordinate(it[0], it[1]) }.toTypedArray())
        }
        encoder.addFeature("de.fraunhofer.igd.main", it.properties, geom)
    }

    return encoder.encode()

}

fun createTileTransform(featureCollection: FeatureCollection, z: Int, x: Int, y: Int): ByteArray {
    val encoder = VectorTileEncoder(4096, 8, false)

    transformTile(
            Tile(features, 4096, x, y, 1 shl z)
    ).features.map {
        val geom = when (it.geom.type) {
            "Polygon" ->  gf.createPolygon(it.geom.coords.map { org.locationtech.jts.geom.Coordinate(it[0], it[1]) }.toTypedArray())
            "MultiPolygon" -> gf.createPolygon(it.geom.coords.map { org.locationtech.jts.geom.Coordinate(it[0], it[1]) }.toTypedArray())
            "Point" -> gf.createPoint(it.geom.coords.map { org.locationtech.jts.geom.Coordinate(it[0], it[1]) }[0])
            "LineString" -> gf.createLineString(it.geom.coords.map { org.locationtech.jts.geom.Coordinate(it[0], it[1]) }.toTypedArray())
            else ->  gf.createPolygon(it.geom.coords.map { org.locationtech.jts.geom.Coordinate(it[0], it[1]) }.toTypedArray())
        }
        encoder.addFeature("de.fraunhofer.igd.main", it.properties, geom)
    }

    return encoder.encode()

}

fun mergeTiles(t1: ByteArray, t2: FeatureCollection, z: Int, x: Int, y: Int) : ByteArray {
    val decoder = VectorTileDecoder()
    val encoder = VectorTileEncoder(4096, 8, false)

    decoder.isAutoScale = false

    val oldTile = decoder.decode(t1)
    oldTile.forEach {
        encoder.addFeature("de.fraunhofer.igd.main", it.attributes, it.geometry)
    }

    transformTile(
            Tile(t2, 4096, x, y, 1 shl z)
    ).features.forEach {
        val geom = when (it.geom.type) {
            "Polygon" ->  gf.createPolygon(it.geom.coords.map { org.locationtech.jts.geom.Coordinate(it[0], it[1]) }.toTypedArray())
            "MultiPolygon" -> gf.createPolygon(it.geom.coords.map { org.locationtech.jts.geom.Coordinate(it[0], it[1]) }.toTypedArray())
            "Point" -> gf.createPoint(it.geom.coords.map { org.locationtech.jts.geom.Coordinate(it[0], it[1]) }[0])
            "LineString" -> gf.createLineString(it.geom.coords.map { org.locationtech.jts.geom.Coordinate(it[0], it[1]) }.toTypedArray())
            else ->  gf.createPolygon(it.geom.coords.map { org.locationtech.jts.geom.Coordinate(it[0], it[1]) }.toTypedArray())
        }
        encoder.addFeature("de.fraunhofer.igd.main", it.properties, geom)
    }


    return encoder.encode()
}

fun filterTileBoxes(features: List<VectorTileDecoder.Feature>, boxes: List<BBox>, z: Int, x: Int, y: Int): List<VectorTileDecoder.Feature> {
    val f = features.filter {
        boxes
                .map { box -> includesPoints(transformBBox(z, x, y, features[0].extent, box), it.geometry.coordinates.map { listOf(it.x, it.y) })  }
                .fold(false, { a,b -> a || b})

    }
    return f
}
