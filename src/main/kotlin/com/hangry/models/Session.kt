package com.hangry.models

import com.fasterxml.jackson.annotation.JsonIgnore
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
    @Transient private val tokens = mutableSetOf<String>() // joined, but not necessarily given votes
    @Transient private var adminToken: String? = null

    @Transient private val givenPreferences = mutableSetOf<String>() // track if token has given votes/preferences

    @Transient private val categoryVotes: MutableMap<Category, Int> = EnumMap(Category::class.java)
    @Transient private val vegetarianVotes: MutableMap<Boolean, Int> = HashMap()
    @Transient private val alcoholVotes: MutableMap<Boolean, Int> = HashMap()
    @Transient private val minPriceVotes: MutableMap<Int, Int> = HashMap()
    @Transient private val maxPriceVotes: MutableMap<Int, Int> = HashMap()

    // Store number of choices token has made
    @Transient private val tokenChoices: MutableMap<String, Int> = HashMap()
    // Store location pairs given to user
    @Transient private val pairVoted: MutableList<Pair<String, String>> = mutableListOf()
    // Store the currently issued pair
    @Transient private val givenPair: MutableMap<String, Pair<String, String>> = HashMap()

    // Lower points, the better
    @Transient private var restaurantsPoints: MutableMap<Restaurant, Int> = HashMap()

    var started = false
    var ended = false

    companion object {
        val SESSION_CODE_LENGTH = 4
        val SESSION_CHAR_POOL: List<Char> = ('A'..'Z') + ('0'..'9')
        val NUMBER_OF_IMAGES = 2
        val NUMBER_OF_CHOICES = 5

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

    fun addPreferences(token: String, categories: List<Category>, vegetarian: Boolean, alcohol: Boolean, minPrice: Int, maxPrice: Int) {
        givenPreferences.add(token)
        // increment vote answer for each question
        categories.forEach { addCategory(it) }
        addVegetarian(vegetarian)
        addAlcohol(alcohol)
        addPrice(minPrice, maxPrice)
    }

    private fun addCategory(category: Category) {
        categoryVotes[category] = categoryVotes.getOrDefault(category, 0) + 1
    }

    private fun addVegetarian(vegetarian: Boolean) {
        vegetarianVotes[vegetarian] = vegetarianVotes.getOrDefault(vegetarian, 0) + 1
    }

    private fun addAlcohol(alcohol: Boolean) {
        alcoholVotes[alcohol] = alcoholVotes.getOrDefault(alcohol, 0) + 1
    }

    private fun addPrice(minPrice: Int, maxPrice: Int) {
        minPriceVotes[minPrice] = minPriceVotes.getOrDefault(minPrice, 0) + 1
        maxPriceVotes[maxPrice] = maxPriceVotes.getOrDefault(maxPrice, 0) + 1
    }


    private fun restaurantIdToRestaurant(id: String): Restaurant {
        return restaurantsPoints.keys.find { it.id == id }!! // should do this a better way
    }

    fun getRestaurantChoices(token: String): Choices {
        val pair = givenPair[token]
        if (pair == null) {
            if ((tokenChoices[token] ?: 0) > NUMBER_OF_CHOICES) {
                // If we have reached number of choices, don't return more
                return Choices(listOf())
            }

            restaurantsPoints.keys.forEach firstChoice@ { firstChoice ->
                restaurantsPoints.keys.forEach secondChoice@ { secondChoice ->
                    if (firstChoice == secondChoice) return@firstChoice
                    if (pairVoted.contains(Pair(firstChoice.id, secondChoice.id)) ||
                        pairVoted.contains(Pair(secondChoice.id, firstChoice.id))) return@secondChoice

                    return Choices(listOf(firstChoice, secondChoice))
                }
            }

            // No more valid choices available
            return Choices(listOf()) // TODO: when this occurs should we end the session?
        }

        return Choices(listOf(
            restaurantIdToRestaurant(pair.first),
            restaurantIdToRestaurant(pair.second)
        ))
    }

    fun addRestaurantVote(token: String, location: String) {
        if (ended) { return }

        val pair = givenPair[token]
        // Check vote is valid for current stored pair for token
        if (pair != null && (pair.first == location || pair.second == location)) {
            // Locations should always exist
            restaurantsPoints[restaurantIdToRestaurant(location)]?.minus(1) // minus improves score (lower is better)
            restaurantsPoints[restaurantIdToRestaurant( // plus is worse
                if (pair.first == token) pair.second else pair.first
            )]?.plus(1)

            givenPair.remove(token)
            pairVoted.add(pair)
            if (tokenChoices[token] == null) {
                tokenChoices[token] = 1
            } else {
                tokenChoices[token]?.plus(1)
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

            val isVegetarian = vegetarianVotes.maxBy { it.value }.key
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

            sortedRestaurantsForCategory.forEach {
                restaurantsPoints[it] = restaurantsPoints.keys.size
            }
        }
    }
    fun end() {
        ended = true
    }

    @JsonIgnore
    fun getOrderedRestaurants(): List<Restaurant> {
        return restaurantsPoints.toList().sortedBy { (_, value) -> value }.toMap().keys.toList()
    }
}
