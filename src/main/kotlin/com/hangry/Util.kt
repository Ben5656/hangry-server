package com.hangry

import com.hangry.nearbySearch.Restaurant

fun nearbySearchRestaurantToSessionRestaurant(restaurant: Restaurant): com.hangry.models.Restaurant {
    return com.hangry.models.Restaurant(
        restaurant.result.place_id,
        restaurant.result.name,
        restaurant.result.photos_encoded ?: arrayListOf()
    )
}