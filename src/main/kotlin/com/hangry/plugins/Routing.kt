package com.hangry.plugins

import com.hangry.models.CreateSessionBody
import com.hangry.models.Session
import com.hangry.models.sessionStorage
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
    }
}
