package io.marauder.tyler.tiling

import io.marauder.Engine
import io.marauder.models.GeoJSON
import io.marauder.models.Geometry
import io.marauder.tyler.models.BoundingBox
import io.marauder.tyler.models.Tile
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.dump
import kotlinx.serialization.protobuf.ProtoBuf
import no.ecc.vectortile.VectorTileDecoder
import no.ecc.vectortile.VectorTileEncoder


val gf = org.locationtech.jts.geom.GeometryFactory()
val encoder = Engine()

const val LAYER_NAME = "io.marauder.main"

@ImplicitReflectionSerializer
fun createTile(geoJSON: GeoJSON, z: Int, x: Int, y: Int): ByteArray {
    return ProtoBuf.dump(encoder.encode(transformTile(Tile(geoJSON, 1 shl z, x, y, 4096)).geojson.features, LAYER_NAME))
}

@ImplicitReflectionSerializer
fun createTileTransform(geoJSON: GeoJSON, z: Int, x: Int, y: Int): ByteArray {
    return ProtoBuf.dump(encoder.encode(transformTile(Tile(geoJSON, 1 shl z, x, y, 4096)).geojson.features, LAYER_NAME))
}

fun mergeTiles(t1: ByteArray, t2: GeoJSON, z: Int, x: Int, y: Int) : ByteArray {
    val decoder = VectorTileDecoder()
    val encoder = VectorTileEncoder(4096, 8, false)

    decoder.isAutoScale = false

    val oldTile = decoder.decode(t1)
    oldTile.forEach {
        encoder.addFeature(LAYER_NAME, it.attributes, it.geometry)
    }

    transformTile(
            Tile(t2, 1 shl z, x, y, 4096)
    ).geojson.features.forEach {
        val geom = when (it.geometry) {
            is Geometry.Polygon ->  gf.createPolygon((it.geometry as Geometry.Polygon).coordinates[0].map { org.locationtech.jts.geom.Coordinate(it[0], it[1]) }.toTypedArray())
//            "MultiPolygon" -> gf.createPolygon(it.geometry.coordinates[0].map { org.locationtech.jts.geom.Coordinate(it[0], it[1]) }.toTypedArray())
            is Geometry.Point -> gf.createPoint((it.geometry as Geometry.Point).coordinates.let { org.locationtech.jts.geom.Coordinate(it[0], it[1]) })
//            "LineString" -> gf.createLineString(it.geometry.coordinates[0].map { org.locationtech.jts.geom.Coordinate(it[0], it[1]) }.toTypedArray())
            else ->  gf.createPoint((it.geometry as Geometry.Point).coordinates.let { org.locationtech.jts.geom.Coordinate(it[0], it[1]) })
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

    val newTile = decoder.decode(t2)
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
