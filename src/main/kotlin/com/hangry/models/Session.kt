package com.hangry.models

import com.hangry.nearbySearch.getNearby
import com.hangry.nearbySearchRestaurantToSessionRestaurant
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
    @Transient private val pairVoted: MutableMap<Pair<String, String>, MutableList<String>> = HashMap()
    // Store the currently issued pair
    @Transient private val givenPair: MutableMap<String, Pair<String, String>> = HashMap()

    @Transient private var restaurants: MutableList<Restaurant> = arrayListOf()

    var started = false
    var ended = false

    companion object {
        val SESSION_CODE_LENGTH = 4
        val SESSION_CHAR_POOL: List<Char> = ('A'..'Z') + ('0'..'9')
        val NUMBER_OF_IMAGES = 2

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
        categories.forEach { addCategory(it) }
        addDiet(diet)
        addAlcohol(alcohol)
        addPrice(minPrice, maxPrice)

        if (adminToken == null) {
            adminToken = token
            return TokenInfo(token, true)
        }

        return TokenInfo(token, false)
    }

    private fun addCategory(category: Category) {
        categoryVotes[category] = categoryVotes.getOrDefault(category, 0) + 1
    }

    private fun addDiet(diet: Diet) {
        dietVotes[diet] = dietVotes.getOrDefault(diet, 0) + 1
    }

    private fun addAlcohol(alcohol: Boolean) {
        alcoholVotes[alcohol] = alcoholVotes.getOrDefault(alcohol, 0) + 1
    }

    private fun addPrice(minPrice: Int, maxPrice: Int) {
        minPriceVotes[minPrice] = minPriceVotes.getOrDefault(minPrice, 0) + 1
        maxPriceVotes[maxPrice] = maxPriceVotes.getOrDefault(maxPrice, 0) + 1
    }

    private fun restaurantIdToRestaurant(id: String): Restaurant {
        return restaurants.find { it.id == id }!! // should do this a better way
    }

    fun getRestaurantChoices(token: String): Choices {
        var pair = givenPair[token]
        if (pair == null) {
            // TODO: some logic to get two pairs and also handle when no pairs left/ended
            pair = Pair("A", "B") // dummy

            givenPair[token] = pair
        }

        return Choices(listOf(
            restaurantIdToRestaurant(pair.first),
            restaurantIdToRestaurant(pair.second)
        ))
    }

    fun addRestaurantVote(token: String, location: String) {
        if (ended) { return }

        val pair = givenPair[token]
        if (pair != null && (pair.first == location || pair.second == location)) {
            // TODO: add vote to location

            givenPair.remove(token)

            if (pairVoted.containsKey(pair)) {
                pairVoted[pair]?.add(token)
            } else {
                pairVoted[pair] = arrayListOf(token)
            }
        }
    }

    fun isAdmin(token: String) = adminToken == token
    fun isValidToken(token: String) = tokens.any { it == token}
    fun start() {
        started = true

        val orderedCategories = categoryVotes.toList().sortedBy { (_, value) -> value }
        orderedCategories.forEach {
            val restaurantsForCategory = getNearby(
                location.lat.toDouble(),
                location.lng.toDouble(),
                radius,
                it.toString(),
                NUMBER_OF_IMAGES
            ).map { nearbySearchRestaurantToSessionRestaurant(it) }
            restaurants.addAll(restaurantsForCategory)
        }
    }
    fun end() {
        ended = true
    }

    fun getOrderedRestaurants(): List<Restaurant> {
        // TODO: make ordered based on votes
        return restaurants
    }
}
