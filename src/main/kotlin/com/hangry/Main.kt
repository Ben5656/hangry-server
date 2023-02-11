package com.hangry
import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.serialization.jackson.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import java.util.*

fun main() {
    val API_KEY: String = dotenv()["API_KEY"]

    val lat = 52.948698
    val long = -1.180276
    val radius = 1500
    val foodType = "halal"

    getNearby(API_KEY, lat, long, radius, foodType)
}

fun getNearby(API_KEY: String, lat: Double, long: Double, radius: Int, foodType: String) {
    val restaurantArray = mutableListOf<Restaurant>()

    val url = "https://maps.googleapis.com/maps/api/place/"

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
                            "&fields=name%2Cdine_in%2Cdelivery%2Ctakeout%2Cserves_beer%2Cprice_level%2Cserves_vegetarian_food%2Cserves_wine%2Crating%2Cwebsite%2Cplace_id%2Cphoto" +
                            "&key=$API_KEY"
                )

                if (specificSearchRequest.status.value in 200..299) {
                    val restaurant: Restaurant = specificSearchRequest.body()
                    val encodedArray = mutableListOf<String>()

                    if(restaurant.result.photos == null) {
                        restaurant.result.photos_encoded = mutableListOf()
                    } else {
                        for (photo in restaurant.result.photos) {
                            val photoRequest: HttpResponse = client.get("${url}photo?maxwidth=400&photo_reference=${photo.photo_reference}&key=$API_KEY")
                            encodedArray += Base64.getEncoder().encodeToString(photoRequest.body())
                        }
                        restaurant.result.photos_encoded = encodedArray
                        restaurantArray += restaurant
                    }
                } else {
                    println("Unsuccessful connection, response code: ${specificSearchRequest.status}")
                }
            }
        } else {
            println("Unsuccessful connection, response code: ${generalSearchRequest.status}")
        }
    }
    client.close()
}

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        jackson {
            registerKotlinModule()
        }
    }
}