package com.hangry.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*
import kotlin.collections.HashMap
import kotlin.random.Random

val sessionStorage = mutableListOf<Session>()

@Serializable
class Session(val code: String, val type: SessionType, val location: Location, val radius: Int) { // TODO: do this with inheritance
    @Transient private val tokens = mutableListOf<String>()
    @Transient private var adminToken: String? = null

    @Transient private val categoryVotes: MutableMap<Category, Int> = EnumMap(Category::class.java)
    @Transient private val dietVotes: MutableMap<Diet, Int> = EnumMap(Diet::class.java)
    @Transient private val alcoholVotes: MutableMap<Boolean, Int> = HashMap()
    @Transient private val minPriceVotes: MutableMap<Int, Int> = HashMap()
    @Transient private val maxPriceVotes: MutableMap<Int, Int> = HashMap()

    // Store if location pair has been voted on already by user
    @Transient private val pairVoted: MutableMap<Pair<String, String>, List<String>> = HashMap()
    // Store the currently issued pair
    @Transient private val givenPair: MutableMap<String, Pair<String, String>> = HashMap()

    var started = false

    companion object {
        val SESSION_CODE_LENGTH = 4
        val SESSION_CHAR_POOL: List<Char> = ('A'..'Z') + ('0'..'9')

        fun generateCode(): String {
            // TODO: check if it already exists
            return (1..SESSION_CODE_LENGTH)
                .map { Random.nextInt(0, SESSION_CHAR_POOL.size).let { SESSION_CHAR_POOL[it] } }
                .joinToString("")
        }
    }

    fun createToken(categories: List<Category>, diet: Diet, alcohol: Boolean, minPrice: Int, maxPrice: Int): TokenInfo {
        val token = UUID.randomUUID().toString()
        tokens.add(token) // add user's token to session
        // increment vote answer for each question
        categories.forEach { categoryVotes[it] = categoryVotes.getOrDefault(it, 0) + 1 }
        dietVotes[diet] = dietVotes.getOrDefault(diet, 0) + 1
        alcoholVotes[alcohol] = alcoholVotes.getOrDefault(alcohol, 0) + 1
        minPriceVotes[minPrice] = minPriceVotes.getOrDefault(minPrice, 0) + 1
        maxPriceVotes[maxPrice] = maxPriceVotes.getOrDefault(maxPrice, 0) + 1

        if (adminToken == null) {
            adminToken = token
            return TokenInfo(token, true)
        }

        return TokenInfo(token, false)
    }

    private fun restaurantIdToRestaurant(id: String): Restaurant {
        return Restaurant("test")
    }

    fun getRestaurantChoices(token: String): Choices {
        var pair = givenPair[token]
        if (pair == null) {
            // TODO: some logic to get two pairs and also handle when no pairs left
            pair = Pair("A", "B") // dummy

            givenPair[token] = pair
        }

        return Choices(listOf(
            restaurantIdToRestaurant(pair.first),
            restaurantIdToRestaurant(pair.second)
        ))
    }

    fun addRestaurantVote(token: String, location: String) {
        val pair = givenPair[token]
        if (pair != null && (pair.first == location || pair.second == location)) {
            // TODO: add vote to location

            givenPair.remove(token)
        }
    }

    fun isAdmin(token: String) = adminToken == token
    fun isValidToken(token: String) = tokens.any { it == token}
    fun start() { started = true }
}
