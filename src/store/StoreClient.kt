package io.marauder.tyler.store

import io.marauder.tyler.models.FeatureCollection
import java.io.Writer

interface StoreClient {
    fun setTile(x: Int, y: Int, z: Int, tile: String)
    fun setTile(x: Int, y: Int, z: Int, tile: ByteArray)
    fun getTile(x: Int, y: Int, z: Int): ByteArray?
    fun getTileJson(x: Int, y: Int, z: Int): String?
    fun serveTile(x: Int, y: Int, z: Int, writer: Writer, properties: List<String> = emptyList(), filter: List<List<Double>> = emptyList())
    fun deleteTile(x: Int, y: Int, z: Int)
    fun clearStore()
    fun updateTile(x: Int, y: Int, z: Int, tile: String)
    fun updateTile(x: Int, y: Int, z: Int, tile: ByteArray)
    fun updateTile(x: Int, y: Int, z: Int, tile: FeatureCollection)
}