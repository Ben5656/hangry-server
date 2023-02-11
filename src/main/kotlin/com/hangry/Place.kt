package com.hangry

import kotlinx.serialization.Serializable

@Serializable
data class Place(
    val html_attributions: List<String>,
    val results: List<Result>,
    val status: String
)

@Serializable
data class Result(
    val business_status: String?,
    val geometry: Geometry?,
    val icon: String?,
    val icon_background_color: String?,
    val icon_mask_base_uri: String?,
    val name: String?,
    val opening_hours: OpeningHours?,
    val photos: List<Photo>?,
    val place_id: String?,
    val plus_code: PlusCode?,
    val price_level: Int? = null,
    val rating: Double?,
    val reference: String?,
    val scope: String?,
    val types: List<String>?,
    val user_ratings_total: Int?,
    val vicinity: String?
)

@Serializable
data class Geometry(
    val location: Location,
    val viewport: Viewport
)

@Serializable
data class OpeningHours(
    val open_now: Boolean
)

@Serializable
data class Photo(
    val height: Int,
    val html_attributions: List<String>,
    val photo_reference: String,
    val width: Int
)

@Serializable
data class PlusCode(
    val compound_code: String,
    val global_code: String
)

@Serializable
data class Location(
    val lat: Double,
    val lng: Double
)

@Serializable
data class Viewport(
    val northeast: Northeast,
    val southwest: Southwest
)

@Serializable
data class Northeast(
    val lat: Double,
    val lng: Double
)

@Serializable
data class Southwest(
    val lat: Double,
    val lng: Double
)