package io.marauder.tyler.tiling

import io.marauder.Engine
import io.marauder.models.Feature
import io.marauder.models.GeoJSON
import io.marauder.models.Value
import io.marauder.tyler.BoundingBox
import io.marauder.tyler.Tile
import kotlinx.serialization.ImplicitReflectionSerializer

/**
 * Actual wrapper class to encoder and merge tiles using the marauder engine
 * @param extend Tile resolution
 * @param buffer Tile buffer
 * @param layerName the name of the tile layer to be created
 */
class VT(extend: Int = 4096,
         private val buffer: Int = 64,
         val layerName: String = "io.marauder.tyler") {

    val engine = Engine(extend)

    @ImplicitReflectionSerializer
    fun createTile(geoJSON: GeoJSON, z: Int, x: Int, y: Int): ByteArray {
        return engine.encode(transformTile(Tile(geoJSON, 1 shl z, x, y, 4096)).geojson.features, layerName).toByteArray()
    }

    @ImplicitReflectionSerializer
    fun createTileTransform(geoJSON: GeoJSON, z: Int, x: Int, y: Int): ByteArray {
        return engine.encode(transformTile(Tile(geoJSON, 1 shl z, x, y, 4096)).geojson.features, layerName).toByteArray()
    }

    fun mergeTiles(t1: ByteArray, t2: GeoJSON): ByteArray {
        val tile1 = engine.deserialize(t1)
        val tile2 = engine.encode(t2.features, layerName)
        return engine.merge(tile1, tile2).toByteArray()
    }

    fun mergeTilesInject(t1: ByteArray, t2: GeoJSON): ByteArray {
        val tile1 = engine.deserialize(t1)
        val layer1 = tile1.getLayers(0)

        val keyList = layer1.keysList.mapIndexed { i, s -> s to i }.toMap().toMutableMap()
        val valueList = layer1.valuesList.mapIndexed { i, v ->
            when {
                v.hasDoubleValue() -> Value.DoubleValue(v.doubleValue) to i
                v.hasIntValue() -> Value.IntValue(v.intValue) to i
                else -> Value.StringValue(v.stringValue) to i
            }
        }.toMap().toMutableMap()
        return engine.encode(t2.features, layerName, keyList, valueList, layer1.featuresList).toByteArray()
    }

    fun mergeTilesInject(t1: ByteArray, t2: ByteArray): ByteArray {
        return engine.merge(t1, t2).toByteArray()
    }

    fun filterTileBoxes(features: List<Feature>, boxes: List<BoundingBox>, z: Int, x: Int, y: Int) = features.filter { f ->
        val coords = foldCoordinates(f)
        boxes
                .map { box -> includesPoints(transformBBox(z, x, y, 4096, box), coords.map { listOf(it[0], it[1]) }) }
                .fold(false) { a, b -> a || b }

    }

}


