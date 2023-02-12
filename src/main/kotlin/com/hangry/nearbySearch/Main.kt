package com.hangry.nearbySearch
import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.hangry.foodStandards.foodStandardsLookup
import io.ktor.serialization.jackson.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import java.util.*

fun main() {
    val lat = 39.916668
    val long = 116.383331
    val radius = 1000
    val foodType = "chinese"
    val numberOfImages = 2

    getNearby(lat, long, radius, foodType, numberOfImages)
}

fun getNearby(lat: Double, long: Double, radius: Int, foodType: String, numberOfImages: Int) : List<Restaurant> {
    val API_KEY: String = dotenv()["API_KEY"]
    val url = "https://maps.googleapis.com/maps/api/place/"
    val restaurantArray = mutableListOf<Restaurant>()

    val client = HttpClient(CIO) {
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            json()
        }
    }

    runBlocking {
        val generalSearchRequest: HttpResponse = client.get(
            "${url}nearbysearch/json?" +
                    "location=$lat%2C$long" +
                    "&radius=$radius" +
                    "&type=restaurant" +
                    "&keyword=$foodType" +
                    "&opennow" +
                    "&key=$API_KEY"
        )

        if (generalSearchRequest.status.value in 200..299) {
            val jsonObject: Place = generalSearchRequest.body()

            for (result in jsonObject.results) {
                val specificSearchRequest: HttpResponse = client.get(
                    "${url}details/json?" +
                            "place_id=${result.place_id}" +
                            "&fields=name%2Cdine_in%2Cdelivery%2Ctakeout%2Cserves_beer%2Cprice_level%2Cserves_vegetarian_food%2Cserves_wine%2Crating%2Cwebsite%2Cplace_id%2Cphoto%2Ctypes%2Ceditorial_summary%2Cwheelchair_accessible_entrance" +
                            "&key=$API_KEY"
                )

                if (specificSearchRequest.status.value == 200) {
                    val restaurant: Restaurant = specificSearchRequest.body()
                    val encodedArray = mutableListOf<String>()

                    if (restaurant.result.photos == null) {
                        restaurant.result.photos_encoded = mutableListOf()
                    } else {
                        for (photo in restaurant.result.photos.take(numberOfImages)) {
                            val photoRequest: HttpResponse = client.get("${url}photo?maxwidth=400&photo_reference=${photo.photo_reference}&key=$API_KEY")
                            encodedArray += Base64.getEncoder().encodeToString(photoRequest.body())
                        }
                        restaurant.result.photos_encoded = encodedArray

                        if(restaurant.result.geometry != null) {
                            val rating = foodStandardsLookup(restaurant.result.geometry.location.lat, restaurant.result.geometry.location.lng, restaurant.result.name)
                            restaurant.result.hygeineRating = rating
                        }
                        restaurantArray += restaurant
                    }
                } else {
                    println("Unsuccessful connection, response code: ${specificSearchRequest.status}")
                }
            }
            for(restaurant in restaurantArray){
                println("Restaurant: ${restaurant.result.name}")
            }
        } else {
            println("Unsuccessful connection, response code: ${generalSearchRequest.status}")
        }
    }
    client.close()
    return restaurantArray
}

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        jackson {
            registerKotlinModule()
        }
    }
}