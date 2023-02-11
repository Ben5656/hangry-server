package com.hangry.plugins

import com.hangry.models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        post("/create") {
            val (type, location, radius) = call.receive<CreateSessionBody>()
            val sessionCode = Session.generateCode()
            val session = Session(sessionCode, type, location, radius)
            sessionStorage.add(session)
            call.respond(session)
        }
        post("/{code}/join") {
            val code = call.parameters["code"]
            val session = sessionStorage.find { it.code == code }

            if (session == null) {
                // If session doesn't exist, throw 404
                call.respond(HttpStatusCode.NotFound)
                return@post
            }

            // TODO: stop people joining if already started

            val (categories, diet, alcohol, minPrice, maxPrice) = call.receive<JoinSessionBody>()
            val tokenInfo = session.createToken(categories, diet, alcohol, minPrice, maxPrice)
            call.respond(tokenInfo)
        }
        // TODO: switch to a proper auth library
        put("/{code}/start") {
            val code = call.parameters["code"]
            val token = call.request.headers["Authorization"]
            val session = sessionStorage.find { it.code == code }

            if (session == null) {
                // If session doesn't exist, throw 404
                call.respond(HttpStatusCode.NotFound)
                return@put
            } else if (token == null || !session.isAdmin(token)) {
                // If not authed, or token is not admin, throw 401
                call.respond(HttpStatusCode.Unauthorized)
                return@put
            }

            session.start()
            call.respond(HttpStatusCode.OK) // is this required?
        }
        get("/{code}/info") {
            val code = call.parameters["code"]
            val token = call.request.headers["Authorization"]
            val session = sessionStorage.find { it.code == code }

            if (session == null) {
                // If session doesn't exist, throw 404
                call.respond(HttpStatusCode.NotFound)
                return@get
            } else if (token == null || !session.isValidToken(token)) {
                // If not authed, or token is not valid, throw 401
                call.respond(HttpStatusCode.Unauthorized)
                return@get
            }

            call.respond(session)
        }
        get("/{code}/choices") {
            val code = call.parameters["code"]
            val token = call.request.headers["Authorization"]
            val session = sessionStorage.find { it.code == code }

            if (session == null) {
                // If session doesn't exist, throw 404
                call.respond(HttpStatusCode.NotFound)
                return@get
            } else if (token == null || !session.isValidToken(token)) {
                // If not authed, or token is not valid, throw 401
                call.respond(HttpStatusCode.Unauthorized)
                return@get
            }

            call.respond(session.getRestaurantChoices(token))
        }
        post("/{code}/choice") {
            val code = call.parameters["code"]
            val token = call.request.headers["Authorization"]
            val session = sessionStorage.find { it.code == code }
            val choice = call.receive<ChoiceBody>()

            if (session == null) {
                // If session doesn't exist, throw 404
                call.respond(HttpStatusCode.NotFound)
                return@post
            } else if (token == null || !session.isValidToken(token)) {
                // If not authed, or token is not valid, throw 401
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }

            session.addRestaurantVote(token, choice.choice)
            call.respond(HttpStatusCode.OK)
        }
        put("/{code}/end") {
            val code = call.parameters["code"]
            val token = call.request.headers["Authorization"]
            val session = sessionStorage.find { it.code == code }

            if (session == null) {
                // If session doesn't exist, throw 404
                call.respond(HttpStatusCode.NotFound)
                return@put
            } else if (token == null || !session.isAdmin(token)) {
                // If not authed, or token is not admin, throw 401
                call.respond(HttpStatusCode.Unauthorized)
                return@put
            }

            session.end()
            call.respond(HttpStatusCode.OK) // is this required?
        }
        get("/{code}/results") {
            // There's no reason to authenticate this
            val code = call.parameters["code"]
            val session = sessionStorage.find { it.code == code }

            if (session == null) {
                // If session doesn't exist, throw 404
                call.respond(HttpStatusCode.NotFound)
                return@get
            } else if (!session.ended) {
                // Session has not ended yet
                call.respond(HttpStatusCode.Locked)
                return@get
            }

            call.respond(session.getOrderedRestaurants())
        }
    }
}
