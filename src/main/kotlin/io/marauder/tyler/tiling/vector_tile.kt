package io.marauder.tyler.tiling

import io.marauder.Engine
import io.marauder.models.Feature
import io.marauder.models.GeoJSON
import io.marauder.models.Value
import io.marauder.tyler.BoundingBox
import io.marauder.tyler.Tile
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

fun mergeTiles(t1: ByteArray, t2: GeoJSON) : ByteArray {
    val tile1 = encoder.deserialize(t1)
    val tile2 = encoder.encode(t2.features, LAYER_NAME)
    return encoder.merge(tile1, tile2).toByteArray()
}

fun mergeTilesInject(t1: ByteArray, t2: GeoJSON) : ByteArray{
    val t1 = encoder.deserialize(t1)
    val layer1 = t1.getLayers(0)

    val keyList = layer1.keysList.mapIndexed { i, s -> s to i }.toMap().toMutableMap()
    val l = layer1.valuesList.mapIndexed { i, v ->
        when {
            v.hasDoubleValue() -> Value.DoubleValue(v.doubleValue) to i
            v.hasIntValue() ->  Value.IntValue(v.intValue) to i
            else -> Value.StringValue(v.stringValue) to i
        }
    }.toMap().toMutableMap()
    return encoder.encode(t2.features, LAYER_NAME, keyList, l, layer1.featuresList).toByteArray()
}

fun mergeTilesInject(t1: ByteArray, t2: ByteArray) : ByteArray {
    return encoder.merge(t1, t2).toByteArray()
}

fun filterTileBoxes(features: List<Feature>, boxes: List<BoundingBox>, z: Int, x: Int, y: Int) = features.filter { f ->
        val coords = foldCoordinates(f)
        boxes
                .map { box -> includesPoints(transformBBox(z, x, y, 4096, box), coords.map { listOf(it[0], it[1]) })  }
                .fold(false) { a, b -> a || b}

    }
