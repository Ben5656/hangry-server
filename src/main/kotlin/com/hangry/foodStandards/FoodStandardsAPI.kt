package com.hangry.foodStandards

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking

fun main() {
    val lat = 52.948698
    val long = -1.180276
    val name = "rose and crown"

    println(foodStandardsLookup(lat, long, name))
}

fun foodStandardsLookup(lat: Double, long: Double, name: String): Int {
    val output: Any
    val client = HttpClient(CIO) {
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            json()
        }
    }

    runBlocking {
        val fsaLookup: HttpResponse = client.get(
            "https://ratings.food.gov.uk/enhanced-search/en-GB/$name/^/DISTANCE/0/^/$long/$lat/1/5/json"
        )
        val establishments = fsaLookup.body() as String
        output = establishments.substring(establishments.indexOf("\"RatingValue\":\"")+15, establishments.indexOf("\",\"RatingKey\""))
    }
    return if(output == "AwaitingInspection") -1
    else return Integer.parseInt(output as String?)
}