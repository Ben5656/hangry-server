package com.hangry.models

import kotlinx.serialization.Serializable

@Serializable
enum class SessionType {
    DELIVERY,
    EAT_IN
}

@Serializable
enum class Category {
    CHINESE,
    ITALIAN,
    INDIAN,
    GREEK,
    JAPANESE,
    FRENCH,
    CARIBBEAN,
    THAI,
    SPANISH,
    MEXICAN,
    PUB,
    TAPAS
}

@Serializable
enum class Diet {
    NORMAL,
    VEGETARIAN,
    VEGAN
}

@Serializable
data class Location(val lat: Float, val lng: Float)

@Serializable
data class CreateSessionBody(val type: SessionType, val location: Location, val radius: Int)

@Serializable
data class JoinSessionBody(
    val categories: List<Category>,
    val diet: Diet,
    val alcohol: Boolean,
    val minPrice: Int,
    val maxPrice: Int
)

@Serializable
data class TokenInfo(val token: String, val admin: Boolean)