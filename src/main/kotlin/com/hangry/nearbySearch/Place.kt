package com.hangry.nearbySearch

import kotlinx.serialization.Serializable

@Serializable
data class Place(
    val html_attributions: List<String>,
    val results: List<Result>,
    val status: String
)

@Serializable
data class Result(
    val business_status: String? = null,
    val geometry: Geometry? = null,
    val icon: String? = null,
    val icon_background_color: String? = null,
    val icon_mask_base_uri: String? = null,
    val name: String,
    val opening_hours: OpeningHours? = null,
    val photos: List<Photo>? = null,
    var photos_encoded: List<String>? = null,
    val place_id: String,
    val plus_code: PlusCode? = null,
    val price_level: Int? = null,
    val rating: Double? = null,
    val reference: String? = null,
    val scope: String? = null,
    val types: List<String>? = null,
    val user_ratings_total: Int? = null,
    val vicinity: String? = null,

    val delivery: Boolean? = null,
    val dine_in: Boolean? = null,
    val serves_beer: Boolean? = null,
    val serves_vegetarian_food: Boolean? = null,
    val serves_wine: Boolean? = null,
    val takeout: Boolean? = null,
    val website: String? = null
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
    val html_attributions: List<String>? = null,
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