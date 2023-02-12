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
    TAPAS,
    HALAL
}

@Serializable
enum class UserStatus {
    JOINED,
    READY
}

@Serializable
data class Location(val lat: Float, val lng: Float)

@Serializable
data class CreateSessionBody(val type: SessionType, val location: Location, val radius: Int)

@Serializable
data class JoinSessionBody(
    val photo: String
)

@Serializable
data class PreferencesBody(
    val categories: List<Category>,
    val vegetarian: Boolean,
    val alcohol: Boolean,
    val wheelchair: Boolean,
    val minPrice: Int,
    val maxPrice: Int
)

@Serializable
data class TokenInfo(val token: String, val admin: Boolean)

@Serializable
data class Restaurant(
    val id: String,
    val name: String,
    val photos: List<String>,
    val types: List<String>,
    val rating: Float?,
    val priceLevel: Int?,
    val delivery: Boolean?,
    val dineIn: Boolean?,
    val beer: Boolean?,
    val vegetarianFood: Boolean?,
    val wine: Boolean?,
    val takeout: Boolean?,
    val website: String?,
    val description: String?,
    val wheelchair: Boolean?,
    val location: Location?,
    val match: Float? // this should be somewhere else as it's not populated until the end
)

@Serializable
data class Choices(val choices: List<Restaurant>)

@Serializable
data class ChoiceBody(val choice: String)

@Serializable
data class ResultsResponse(val results: List<Restaurant>)

@Serializable
data class UserInfo(val photo: String, val status: UserStatus)

@Serializable
data class UsersResponse(val users: List<UserInfo>)
