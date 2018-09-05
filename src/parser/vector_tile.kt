package io.marauder.tyler.parser

import io.marauder.tyler.models.BoundingBox
import io.marauder.tyler.models.FeatureCollection
import io.marauder.tyler.models.Tile
import no.ecc.vectortile.VectorTileDecoder
import no.ecc.vectortile.VectorTileEncoder


val gf = org.locationtech.jts.geom.GeometryFactory()
const val LAYER_NAME = "io.marauder.main"

fun createTile(featureCollection: FeatureCollection, z: Int, x: Int, y: Int): ByteArray {
    val encoder = VectorTileEncoder(4096, 8, false)

    featureCollection.features.map {
        val geom = when (it.geometry.type) {
            "Polygon" ->  gf.createPolygon(it.geometry.coordinates[0].map { org.locationtech.jts.geom.Coordinate(it[0], it[1]) }.toTypedArray())
            "MultiPolygon" -> gf.createPolygon(it.geometry.coordinates[0].map { org.locationtech.jts.geom.Coordinate(it[0], it[1]) }.toTypedArray())
            "Point" -> gf.createPoint(it.geometry.coordinates[0].map { org.locationtech.jts.geom.Coordinate(it[0], it[1]) }[0])
            "LineString" -> gf.createLineString(it.geometry.coordinates[0].map { org.locationtech.jts.geom.Coordinate(it[0], it[1]) }.toTypedArray())
            else ->  gf.createPolygon(it.geometry.coordinates[0].map { org.locationtech.jts.geom.Coordinate(it[0], it[1]) }.toTypedArray())
        }
        encoder.addFeature(LAYER_NAME, it.properties, geom)
    }

    return encoder.encode()

}

fun createTileTransform(featureCollection: FeatureCollection, z: Int, x: Int, y: Int): ByteArray {
    val encoder = VectorTileEncoder(4096, 8, false)

    transformTile(
            Tile(featureCollection, 1 shl z, x, y, 4096)
    ).featureCollection.features.map {
        val geom = when (it.geometry.type) {
            "Polygon" ->  gf.createPolygon(it.geometry.coordinates[0].map { org.locationtech.jts.geom.Coordinate(it[0], it[1]) }.toTypedArray())
            "MultiPolygon" -> gf.createPolygon(it.geometry.coordinates[0].map { org.locationtech.jts.geom.Coordinate(it[0], it[1]) }.toTypedArray())
            "Point" -> gf.createPoint(it.geometry.coordinates[0].map { org.locationtech.jts.geom.Coordinate(it[0], it[1]) }[0])
            "LineString" -> gf.createLineString(it.geometry.coordinates[0].map { org.locationtech.jts.geom.Coordinate(it[0], it[1]) }.toTypedArray())
            else ->  gf.createPolygon(it.geometry.coordinates[0].map { org.locationtech.jts.geom.Coordinate(it[0], it[1]) }.toTypedArray())
        }
        encoder.addFeature(LAYER_NAME, it.properties, geom)
    }

    return encoder.encode()

}

fun mergeTiles(t1: ByteArray, t2: FeatureCollection, z: Int, x: Int, y: Int) : ByteArray {
    val decoder = VectorTileDecoder()
    val encoder = VectorTileEncoder(4096, 8, false)

    decoder.isAutoScale = false

    val oldTile = decoder.decode(t1)
    oldTile.forEach {
        encoder.addFeature(LAYER_NAME, it.attributes, it.geometry)
    }

    transformTile(
            Tile(t2, 4096, x, y, 1 shl z)
    ).featureCollection.features.forEach {
        val geom = when (it.geometry.type) {
            "Polygon" ->  gf.createPolygon(it.geometry.coordinates[0].map { org.locationtech.jts.geom.Coordinate(it[0], it[1]) }.toTypedArray())
            "MultiPolygon" -> gf.createPolygon(it.geometry.coordinates[0].map { org.locationtech.jts.geom.Coordinate(it[0], it[1]) }.toTypedArray())
            "Point" -> gf.createPoint(it.geometry.coordinates[0].map { org.locationtech.jts.geom.Coordinate(it[0], it[1]) }[0])
            "LineString" -> gf.createLineString(it.geometry.coordinates[0].map { org.locationtech.jts.geom.Coordinate(it[0], it[1]) }.toTypedArray())
            else ->  gf.createPolygon(it.geometry.coordinates[0].map { org.locationtech.jts.geom.Coordinate(it[0], it[1]) }.toTypedArray())
        }
        encoder.addFeature(LAYER_NAME, it.properties, geom)
    }


    return encoder.encode()
}

fun mergeTiles(t1: ByteArray, t2: ByteArray, z: Int, x: Int, y: Int) : ByteArray {
    val decoder = VectorTileDecoder()
    decoder.isAutoScale = false

    val encoder = VectorTileEncoder(4096, 8, false)

    val oldTile = decoder.decode(t1)
    oldTile.forEach {
        encoder.addFeature(LAYER_NAME, it.attributes, it.geometry)
    }

    val newTile = decoder.decode(t1)
    newTile.forEach {
        encoder.addFeature(LAYER_NAME, it.attributes, it.geometry)
    }

    return encoder.encode()
}

fun filterTileBoxes(features: List<VectorTileDecoder.Feature>, boxes: List<BoundingBox>, z: Int, x: Int, y: Int): List<VectorTileDecoder.Feature> {
    val f = features.filter {
        boxes
                .map { box -> includesPoints(transformBBox(z, x, y, features[0].extent, box), it.geometry.coordinates.map { listOf(it.x, it.y) })  }
                .fold(false, { a,b -> a || b})

    }
    return f
}
