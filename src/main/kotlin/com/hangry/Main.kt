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

suspend fun main() {
    val lat = 52.948698
    val long = -1.180276
    val radius = 1500
    val API_KEY: String = dotenv()["API_KEY"]
    val foodType = "greek"

    getNearby(API_KEY, lat, long, radius, foodType)
}
suspend fun getNearby(API_KEY: String, lat: Double, long: Double, radius: Int, foodType: String) {

    val client = HttpClient(CIO) {
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            json()
        }
    }
    val request: HttpResponse = client.get("https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
            "location=$lat%2C$long" +
            "&radius=$radius" +
            "&type=restaurant" +
            "&keyword=$foodType" +
            "&opennow" +
            "&key=$API_KEY")

    if(request.status.value in 200..299) {
        val jsonObject: Place = request.body()

        for(result in jsonObject.results){
            val specificRequest: HttpResponse = client.get("https://maps.googleapis.com/maps/api/place/details/json?" +
                    "place_id=${result.place_id}" +
                    "&fields=name%2Cdine_in%2Cdelivery%2Ctakeout%2Cserves_beer%2Cprice_level%2Cserves_vegetarian_food%2Cserves_wine%2Crating%2Cwebsite" +
                    "&key=$API_KEY")
            println(specificRequest.body() as String)
        }



    } else {
        println("Unsuccessful connection, response code: ${request.status}")
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