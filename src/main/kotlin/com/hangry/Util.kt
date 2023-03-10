package com.hangry

import com.hangry.models.Location
import com.hangry.nearbySearch.Restaurant

fun nearbySearchRestaurantToSessionRestaurant(restaurant: Restaurant): com.hangry.models.Restaurant {
    return com.hangry.models.Restaurant(
        restaurant.result.place_id,
        restaurant.result.name,
        restaurant.result.photos_encoded ?: arrayListOf(),
        restaurant.result.types ?: arrayListOf(),
        restaurant.result.rating?.toFloat(),
        restaurant.result.price_level,
        restaurant.result.delivery,
        restaurant.result.dine_in,
        restaurant.result.serves_beer,
        restaurant.result.serves_vegetarian_food,
        restaurant.result.serves_wine,
        restaurant.result.takeout,
        restaurant.result.website,
        restaurant.result.editorial_summary?.overview,
        restaurant.result.wheelchair_accessible_entrance,
        if (restaurant.result.geometry != null)
        Location(
            restaurant.result.geometry.location.lat.toFloat(),
            restaurant.result.geometry.location.lng.toFloat()
        ) else null,
        restaurant.result.hygeineRating
    )
}