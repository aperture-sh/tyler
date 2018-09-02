package io.marauder.tyler.parser

import io.marauder.tyler.models.Feature

fun merge(f: List<Feature>) =
        """{
  "type": "FeatureCollection",
  "features": [
        ${f.fold("") {l,r -> "$l,${toJson(r)}"}.substring(1)}
        ]
    }
        """.trimIndent()

fun toJson(f: Feature) =
        """
            {
                "type": "Feature",
                "geometry": {
                    "type": "${f.geometry.type}",
                    //TODO: coords export
                    "coordinates": []
                },
                "properties": {${f.properties.map { "\"${it.key}\": \"${it.value}\"" }.fold("") {l,r -> "$l,$r"}.substring(1)}}
            }
        """.trimIndent()