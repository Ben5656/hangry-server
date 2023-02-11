package com.hangry.nearbySearch

import kotlinx.serialization.Serializable

@Serializable
data class Restaurant(
    val html_attributions: List<String>,
    val result: Result,
    val status: String
)