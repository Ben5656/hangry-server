package com.hangry.plugins

import com.hangry.models.CreateSessionBody
import com.hangry.models.JoinSessionBody
import com.hangry.models.Session
import com.hangry.models.sessionStorage
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

            val (categories, diet, alcohol, minPrice, maxPrice) = call.receive<JoinSessionBody>()
            val tokenInfo = session.createToken(categories, diet, alcohol, minPrice, maxPrice)
            call.respond(tokenInfo)
        }
    }
}
