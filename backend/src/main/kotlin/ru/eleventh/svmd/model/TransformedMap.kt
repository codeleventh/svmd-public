package ru.eleventh.svmd.model

import mil.nga.sf.geojson.FeatureCollection

data class TransformedMap(
    val warns: List<String>,
    val directivesMap: Map<String, Set<String>>,
    val geojson: FeatureCollection
)