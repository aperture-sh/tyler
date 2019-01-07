package io.marauder.tyler.tiling

import io.marauder.Engine
import io.marauder.models.Feature
import io.marauder.models.GeoJSON
import io.marauder.tyler.models.BoundingBox
import io.marauder.tyler.models.Tile
import kotlinx.serialization.ImplicitReflectionSerializer


val encoder = Engine()

const val LAYER_NAME = "io.marauder.main"

@ImplicitReflectionSerializer
fun createTile(geoJSON: GeoJSON, z: Int, x: Int, y: Int): ByteArray {
    return encoder.encode(transformTile(Tile(geoJSON, 1 shl z, x, y, 4096)).geojson.features, LAYER_NAME).toByteArray()
}

@ImplicitReflectionSerializer
fun createTileTransform(geoJSON: GeoJSON, z: Int, x: Int, y: Int): ByteArray {
    return encoder.encode(transformTile(Tile(geoJSON, 1 shl z, x, y, 4096)).geojson.features, LAYER_NAME).toByteArray()
}

fun mergeTiles(t1: ByteArray, t2: GeoJSON, z: Int, x: Int, y: Int) : ByteArray {
    val tile1 = encoder.deserialize(t1)
    val tile2 = encoder.encode(t2.features, LAYER_NAME)
    return encoder.merge(tile1, tile2).toByteArray()
}

fun mergeTiles(t1: ByteArray, t2: ByteArray, z: Int, x: Int, y: Int) : ByteArray {
    return encoder.merge(t1, t2).toByteArray()
}

fun filterTileBoxes(features: List<Feature>, boxes: List<BoundingBox>, z: Int, x: Int, y: Int) = features.filter { f ->
        val coords = foldCoordinates(f)
        boxes
                .map { box -> includesPoints(transformBBox(z, x, y, 4096, box), coords.map { listOf(it[0], it[1]) })  }
                .fold(false) { a, b -> a || b}

    }
