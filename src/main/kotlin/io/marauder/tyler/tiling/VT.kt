package io.marauder.tyler.tiling

import io.marauder.supercharged.Encoder
import io.marauder.supercharged.Intersector
import io.marauder.supercharged.Projector
import kotlinx.serialization.ImplicitReflectionSerializer
import io.marauder.supercharged.models.*
/**
 * Wrapper class to encode and merge tiles using the marauder engine
 * @param extend Tile resolution
 * @param buffer Tile buffer
 * @param baseLayer the name of the tile base layer to be created
 */
class VT(extend: Int = 4096,
         private val buffer: Int = 64,
         val baseLayer: String = "io.marauder.tyler") {

    val engine = Encoder(extend)
    private val projector = Projector()
    private val intersector = Intersector()

    @ImplicitReflectionSerializer
    fun createTile(geoJSON: GeoJSON, z: Int, x: Int, y: Int, subLayer: String = ""): ByteArray {
        val layer = if (subLayer != "") "$baseLayer.$subLayer" else baseLayer
        return engine.encode(projector.transformTile(Tile(geoJSON, 1 shl z, x, y)).geojson.features, layer).toByteArray()
    }

    @ImplicitReflectionSerializer
    fun createTileTransform(geoJSON: GeoJSON, z: Int, x: Int, y: Int, subLayer: String = ""): ByteArray {
        val layer = if (subLayer != "") "$baseLayer.$subLayer" else baseLayer
        return engine.encode(projector.transformTile(Tile(geoJSON, 1 shl z, x, y)).geojson.features, layer).toByteArray()
    }

    fun mergeTiles(t1: ByteArray, t2: GeoJSON, subLayer: String = ""): ByteArray {
        val layer = if (subLayer != "") "$baseLayer.$subLayer" else baseLayer
        val tile1 = engine.deserialize(t1)
        val tile2 = engine.encode(t2.features, layer)
        return engine.merge(tile1, tile2).toByteArray()
    }

    fun mergeTilesInject(t1: ByteArray, t2: GeoJSON, subLayer: String = ""): ByteArray {
        val layer = if (subLayer != "") "$baseLayer.$subLayer" else baseLayer
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
        return engine.encode(t2.features, layer, keyList, valueList, layer1.featuresList).toByteArray()
    }

    fun mergeTilesInject(t1: ByteArray, t2: ByteArray): ByteArray {
        return engine.merge(t1, t2).toByteArray()
    }

    fun filterTileBoxes(features: List<Feature>, boxes: List<List<Double>>, z: Int, x: Int, y: Int) = features.filter { f ->
        val coords = projector.foldCoordinates(f)
        boxes
                .map { box -> intersector.includesPoints(projector.transformBBox(z, x, y, box), coords.map { listOf(it[0], it[1]) }) }
                .fold(false) { a, b -> a || b }

    }

}


