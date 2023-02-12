package com.hangry.models

import com.fasterxml.jackson.annotation.JsonIgnore
import com.hangry.nearbySearch.getNearby
import com.hangry.nearbySearchRestaurantToSessionRestaurant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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
    @Transient private var tokenPhotos: MutableMap<String, String> = mutableMapOf()

    @Transient private val givenPreferences = mutableSetOf<String>() // track if token has given votes/preferences

    @Transient private val categoryVotes: MutableMap<Category, Int> = EnumMap(Category::class.java)
    @Transient private val vegetarianVotes: MutableMap<Boolean, Int> = HashMap()
    @Transient private val alcoholVotes: MutableMap<Boolean, Int> = HashMap()
    @Transient private val wheelchairVotes: MutableMap<Boolean, Int> = HashMap()
    @Transient private val minPriceVotes: MutableMap<Int, Int> = HashMap()
    @Transient private val maxPriceVotes: MutableMap<Int, Int> = HashMap()

    @Transient private val restaurantsPerCategory: MutableMap<Category, List<Restaurant>> = EnumMap(Category::class.java)

    // Store number of choices token has made
    @Transient private val tokenChoices: MutableMap<String, Int> = HashMap()
    // Store location pairs given to user
    @Transient private val pairVoted: MutableList<Pair<String, String>> = mutableListOf()
    // Store the currently issued pair
    @Transient private val givenPair: MutableMap<String, Pair<String, String>> = HashMap()

    // Lower points, the better
    @Transient private var restaurantsPoints: MutableMap<Restaurant, Int> = HashMap()

    @Transient private var categoryJobs: MutableMap<Category, Job> = EnumMap(Category::class.java)
    @Transient private var jobCoroutineScope = CoroutineScope(Dispatchers.IO)

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

    init {
        Category.values().forEach { category ->
            val categoryJob = jobCoroutineScope.launch {
                restaurantsPerCategory[category] = getNearby(
                    location.lat.toDouble(),
                    location.lng.toDouble(),
                    radius,
                    category.name,
                    NUMBER_OF_IMAGES
                ).map { nearbySearchRestaurantToSessionRestaurant(it) }
            }
            categoryJobs[category] = categoryJob
        }
    }

    fun createToken(photo: String): TokenInfo {
        val token = UUID.randomUUID().toString()
        tokens.add(token) // add user's token to session
        tokenPhotos[token] = photo

        if (adminToken == null) {
            adminToken = token
            return TokenInfo(token, true)
        }

        return TokenInfo(token, false)
    }

    fun getUsersInfo(): List<UserInfo> {
        return tokenPhotos.map {(token, photo) ->
            UserInfo(photo, if (givenPreferences.contains(token)) UserStatus.READY else UserStatus.JOINED )
        }
    }

    fun addPreferences(
        token: String,
        categories: List<Category>,
        vegetarian: Boolean,
        alcohol: Boolean,
        wheelchair: Boolean,
        minPrice: Int,
        maxPrice: Int
    ) {
        givenPreferences.add(token)
        // increment vote answer for each question
        categories.forEach { addCategory(it) }
        addVegetarian(vegetarian)
        addAlcohol(alcohol)
        addWheelchair(wheelchair)
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

    private fun addWheelchair(wheelchair: Boolean) {
        wheelchairVotes[wheelchair] = wheelchairVotes.getOrDefault(wheelchair, 0) + 1
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

                    givenPair[token] = Pair(firstChoice.id, secondChoice.id)

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
            // Decrement voted for restaurant (lower is better)
            val chosenRestaurant = restaurantIdToRestaurant(location)
            val chosenRestaurantPoints = restaurantsPoints[chosenRestaurant]
            if (chosenRestaurantPoints != null) {
                restaurantsPoints[chosenRestaurant] = chosenRestaurantPoints.dec()
            }
            // Increment failing restaurant (lower is better)
            val otherRestaurant = restaurantIdToRestaurant(
                if (pair.first == token) pair.second else pair.first
            )
            val otherRestaurantPoints = restaurantsPoints[otherRestaurant]
            if (otherRestaurantPoints != null) {
                restaurantsPoints[otherRestaurant] = otherRestaurantPoints.inc()
            }

            givenPair.remove(token)
            pairVoted.add(pair)
            val tokenChoicesCount = tokenChoices[token]
            if (tokenChoicesCount == null) {
                tokenChoices[token] = 1
            } else {
                tokenChoices[token] = tokenChoicesCount.inc()
            }
        }
    }

    fun isAdmin(token: String) = adminToken == token
    fun isValidToken(token: String) = tokens.any { it == token}
    fun start() {
        stopCategoryJobs()

        started = true

        // Remove all users who joined but have not given their preferences
        val notGivenPreferences = tokens.minus(givenPreferences)
        notGivenPreferences.forEach {
            tokens.remove(it)
            tokenPhotos.remove(it)
        }

        // Create initial order based on category popularity
        val orderedCategories = categoryVotes.toList().sortedBy { (_, value) -> value }
        orderedCategories.forEach { category ->
            val restaurantsForCategory = restaurantsPerCategory[category.first]

            val isVegetarian = vegetarianVotes.maxBy { it.value }.key
            val isAlcohol = alcoholVotes.maxBy { it.value }.key
            val isWheelchair = wheelchairVotes.getOrDefault(true, false) // if true, then prioritise, else ignore
            val averageMinPrice = minPriceVotes.maxBy { it.value }.key
            val averageMaxPrice = maxPriceVotes.maxBy { it.value }.key
            val sortedRestaurantsForCategory = restaurantsForCategory?.sortedWith(
                compareBy(
                    // true will be sorted before false
                    { it.vegetarianFood == isVegetarian },
                    { it.wine == isAlcohol || it.beer == isAlcohol },
                    { it.wheelchair == isWheelchair },
                    { it.priceLevel !=null && averageMinPrice <= it.priceLevel },
                    { it.priceLevel != null && averageMaxPrice >= it.priceLevel }
                )
            )

            sortedRestaurantsForCategory?.forEach {
                restaurantsPoints[it] = restaurantsPoints.keys.size
            }
        }
    }

    private fun stopCategoryJobs() {
        Category.values().forEach {category ->
            if (!categoryVotes.keys.contains(category)) {
                categoryJobs[category]?.cancel()
            } else {
                jobCoroutineScope.launch { categoryJobs[category]?.join() }
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

    @JsonIgnore
    fun getMatches(): MutableMap<String, Float> {
        val totalVoters = givenPreferences.size

        val scoreZeroOffset = restaurantsPoints.values.min()
        restaurantsPoints.keys.forEach {
            // normalise the minimum value to be zero
            val restaurantPointsValue = restaurantsPoints[it]
            if (restaurantPointsValue != null) {
                restaurantsPoints[it] = restaurantPointsValue.minus(scoreZeroOffset)
            }
        }

        // calculate score based on voting (0-1)
        val maxScore = restaurantsPoints.values.max()
        val votingScores: MutableMap<String, Float> = mutableMapOf()
        restaurantsPoints.forEach { (restaurant, points) ->
            votingScores[restaurant.id] = maxScore.minus(points.toFloat()).div(maxScore)
        }

        // calculate score based on category (0-1)
        val categoryScores: MutableMap<String, Float> = mutableMapOf()
        restaurantsPoints.keys.forEach {
            var numInCategory = 0 // number of categories this restaurant is in
            restaurantsPerCategory.entries.forEach { (_, categoryRestaurants) ->
                if (categoryRestaurants.contains(it)) { numInCategory++ }
            }
            categoryScores[it.id] = numInCategory.div(totalVoters.toFloat())
        }

        // calculate score based on vegetarian
        val trueVegetarianScore = vegetarianVotes.getOrDefault(true, 0).div(totalVoters.toFloat())
        val falseVegetarianScore = vegetarianVotes.getOrDefault(false, 0).div(totalVoters.toFloat())
        val vegetarianScores: MutableMap<String, Float> = mutableMapOf()
        restaurantsPoints.keys.forEach {
            if (it.vegetarianFood != null) {
                vegetarianScores[it.id] = if (it.vegetarianFood) trueVegetarianScore else falseVegetarianScore
            }
        }

        // calculate score based on alcohol
        val trueAlcoholScore = alcoholVotes.getOrDefault(true, 0).div(totalVoters.toFloat())
        val falseAlcoholScore = alcoholVotes.getOrDefault(false, 0).div(totalVoters.toFloat())
        val alcoholScores: MutableMap<String, Float> = mutableMapOf()
        restaurantsPoints.keys.forEach {
            val hasAlcohol = it.beer ?: false || it.wine ?: false
            if (hasAlcohol) {
                alcoholScores[it.id] = if (hasAlcohol) trueAlcoholScore else falseAlcoholScore
            }
        }

        // calculate score based on wheelchair
        val wheelchairRequired = wheelchairVotes.containsKey(true) // if one person needs wheelchair, then it is a big requirement
        val wheelchairScores: MutableMap<String, Float> = mutableMapOf()
        if (wheelchairRequired) {
            restaurantsPoints.keys.forEach { wheelchairScores[it.id] = if (it.wheelchair == true) 1f else 0f }
        }

        // TODO: add pricing to scoring

        val restaurantScoreMapping: MutableMap<String, Float> = mutableMapOf()
        restaurantsPoints.keys.forEach {
            var scoresAvailable = 0
            var match = 0f

            if (votingScores[it.id] != null) {
                scoresAvailable++
                match += votingScores[it.id] ?: 0f
            }
            if (categoryScores[it.id] != null) {
                scoresAvailable++
                match += categoryScores[it.id] ?: 0f
            }
            if (vegetarianScores[it.id] != null) {
                scoresAvailable++
                match += vegetarianScores[it.id] ?: 0f
            }
            if (alcoholScores[it.id] != null) {
                scoresAvailable++
                match += alcoholScores[it.id] ?: 0f
            }
            if (wheelchairScores[it.id] != null) {
                scoresAvailable++
                match += wheelchairScores[it.id] ?: 0f
            }

            restaurantScoreMapping[it.id] = match.div(scoresAvailable)
        }
        return restaurantScoreMapping
    }
}
