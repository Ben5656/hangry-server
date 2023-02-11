package com.hangry.models

import kotlinx.serialization.Serializable
import java.util.*
import kotlin.random.Random

val sessionStorage = mutableListOf<Session>()

@Serializable
class Session(val code: String, val type: SessionType, val location: Location, val radius: Int) { // TODO: do this with inheritance
    private val tokens = mutableListOf<String>()
    private var adminToken: String? = null

    private val categoryVotes: MutableMap<Category, Int> = EnumMap(Category::class.java)
    private val dietVotes: MutableMap<Diet, Int> = EnumMap(Diet::class.java)
    private val alcoholVotes: MutableMap<Boolean, Int> = HashMap()
    private val minPriceVotes: MutableMap<Int, Int> = HashMap()
    private val maxPriceVotes: MutableMap<Int, Int> = HashMap()

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

    fun isAdmin(token: String) = adminToken == token
}