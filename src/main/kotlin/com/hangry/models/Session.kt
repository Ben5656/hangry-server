package com.hangry.models

import com.fasterxml.jackson.annotation.JsonIgnore
import com.hangry.nearbySearch.getNearby
import com.hangry.nearbySearchRestaurantToSessionRestaurant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*
import kotlin.random.Random

val sessionStorage = mutableListOf<Session>()

@Serializable
class Session(val code: String, val type: SessionType, val location: Location, val radius: Int) { // TODO: do this with inheritance
    @Transient private val tokens = mutableSetOf<String>() // joined, but not necessarily given votes
    @Transient private var adminToken: String? = null

    @Transient private val givenPreferences = mutableSetOf<String>() // track if token has given votes/preferences

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

    fun createToken(): TokenInfo {
        val token = UUID.randomUUID().toString()
        tokens.add(token) // add user's token to session

        if (adminToken == null) {
            adminToken = token
            return TokenInfo(token, true)
        }

        return TokenInfo(token, false)
    }

    fun addPreferences(token: String, categories: List<Category>, diet: Diet, alcohol: Boolean, minPrice: Int, maxPrice: Int) {
        givenPreferences.add(token)
        // increment vote answer for each question
        categories.forEach { addCategory(it) }
        addDiet(diet)
        addAlcohol(alcohol)
        addPrice(minPrice, maxPrice)
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

        // Remove all users who joined but have not given their preferences
        val notGivenPreferences = tokens.minus(givenPreferences)
        tokens.removeAll(notGivenPreferences)

        // Create initial order based on category popularity
        val orderedCategories = categoryVotes.toList().sortedBy { (_, value) -> value }
        orderedCategories.forEach { category ->
            val restaurantsForCategory = getNearby(
                location.lat.toDouble(),
                location.lng.toDouble(),
                radius,
                category.first.toString(),
                NUMBER_OF_IMAGES
            ).map { nearbySearchRestaurantToSessionRestaurant(it) }

            val isVegetarian = dietVotes.maxBy { it.value }.key == Diet.VEGETARIAN
            val isAlcohol = alcoholVotes.maxBy { it.value }.key
            val averageMinPrice = minPriceVotes.maxBy { it.value }.key
            val averageMaxPrice = maxPriceVotes.maxBy { it.value }.key
            val sortedRestaurantsForCategory = restaurantsForCategory.sortedWith(
                compareBy(
                    // true will be sorted before false
                    { it.vegetarianFood == isVegetarian },
                    { it.wine == isAlcohol || it.beer == isAlcohol },
                    { it.priceLevel !=null && averageMinPrice <= it.priceLevel },
                    { it.priceLevel != null && averageMaxPrice >= it.priceLevel }
                )
            )
            restaurants.addAll(sortedRestaurantsForCategory)
        }
    }
    fun end() {
        ended = true
    }

    @JsonIgnore
    fun getOrderedRestaurants(): List<Restaurant> {
        return restaurants
    }
}
