package ru.eleventh.svmd

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.sanctionco.jmail.JMail
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.util.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import ru.eleventh.svmd.exceptions.SvmdException
import ru.eleventh.svmd.model.*
import ru.eleventh.svmd.model.db.MapMeta
import ru.eleventh.svmd.model.db.NewMap
import ru.eleventh.svmd.model.db.NewUser
import ru.eleventh.svmd.services.InviteService
import ru.eleventh.svmd.services.MapService
import ru.eleventh.svmd.services.UserService

data class UserSession(val userId: Long) : Principal

const val AUTH_NAME = "auth-session"

fun Application.configureRouting() {
    install(Sessions) {
        cookie<UserSession>("session", storage = SessionStorageMemory()) {
            cookie.path = "/"
            cookie.httpOnly = false
            cookie.maxAgeInSeconds = 43200 // 1 month
            transform(
                SessionTransportTransformerEncrypt(
                    hex(Config.encryptionKey),
                    hex(Config.signKey)
                )
            )
        }
    }
    install(Authentication) {
        session<UserSession>(AUTH_NAME) {
            validate { session -> session }
            challenge {
                call.respond(HttpStatusCode.Unauthorized, FailResponse(ApiErrors.UNAUTHORIZED))
            }
        }
    }
    install(ContentNegotiation) {
        jackson {
            disable(SerializationFeature.INDENT_OUTPUT)
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            registerModule(JavaTimeModule())
        }
    }
    install(createApplicationPlugin("exception handler") {
        on(CallFailed) { call, cause ->
            System.err.println("Error: $cause")
            cause.printStackTrace()
            if (cause is SvmdException)
                call.respond(cause.toResponse())
            else call.respond(FailResponse(cause.message.orEmpty()))
        }
    })
    routing {
        route("api") {
            post("login") {
                val email = call.parameters["email"]
                val password = call.parameters["password"]
                val user = email?.let { UserService.getUserByEmail(it) }

                if (email == null || password == null)
                    call.respond(HttpStatusCode.BadRequest, FailResponse(ApiErrors.BAD_REQUEST))
                else if (user == null)
                    call.respond(HttpStatusCode.BadRequest, FailResponse(ApiErrors.NOT_FOUND))
                else if (user.password != password) {
                    call.respond(HttpStatusCode.Unauthorized, FailResponse(ApiErrors.BAD_CREDS))
                } else {
                    call.sessions.set(UserSession(user.id))
                    call.respond(SuccessResponse("Logged successfully"))
                }
            }
            post("/logout") {
                call.sessions.clear<UserSession>()
                call.respond(SuccessResponse("Logged out"))
            }
            route("meta") {
                get("{mapId}") { call.respond(SuccessResponse(MapService.getMap(call.parameters["mapId"]!!.uppercase()))) }
                authenticate(AUTH_NAME) {
                    post {
                        val userId = call.sessions.get<UserSession>()!!.userId
                        call.respond(SuccessResponse(MapService.createMap(userId, call.receive<NewMap>())))
                    }
                    get {
                        val userId = call.sessions.get<UserSession>()!!.userId
                        call.respond(SuccessResponse(MapService.getMapsByUser(userId)))
                    }
                    put("{mapId}") {
                        val meta = call.receive<MapMeta>()
                        val mapId = call.parameters["mapId"]
                        val userId = call.sessions.get<UserSession>()?.userId
                        if (userId == null)
                            call.respond(HttpStatusCode.Unauthorized, FailResponse(ApiErrors.UNAUTHORIZED))
                        else call.respond(SuccessResponse(MapService.updateMap(mapId!!.uppercase(), meta, userId)))
                    }
                }
            }
            route("map/{mapId}") {
                get {
                    val mapId = call.parameters["mapId"]!!
                    val transformedMap = MapService.convertMap(mapId)
                    if (transformedMap is MapResponse) {
                        call.response.header(
                            HttpHeaders.CacheControl,
                            Config.cacheLifetime
                        )
                    }
                    call.respond(transformedMap)
                }
                get("geojson") { TODO() }
            }
            route("user") {
                post {
                    val (email, password, invite) = call.receive<RegistrationRequest>()

                    if (!JMail.isValid(email))
                        call.respond(FailResponse(ApiErrors.BAD_EMAIL))
                    if (!UserService.validatePassword(password))
                        call.respond(FailResponse(ApiErrors.BAD_PASSWORD))

                    newSuspendedTransaction {
                        if (!InviteService.validateInvite(invite))
                            call.respond(FailResponse(ApiErrors.BAD_INVITE))

                        val newUserId = UserService.createUser(NewUser(email, password))!!
                        call.sessions.set(UserSession(newUserId))
                        InviteService.useInvite(invite, newUserId)
                    }
                    call.respond(ApiResponse(true))
                }
                authenticate(AUTH_NAME) {
                    get("me") {
                        val userId = call.sessions.get<UserSession>()!!.userId
                        val email = UserService.getUserById(userId)?.email
                        if (email != null) {
                            call.respond(HttpStatusCode.OK, SuccessResponse(email))
                        } else {
                            call.respond(HttpStatusCode.Unauthorized, FailResponse(ApiErrors.UNAUTHORIZED))
                        }
                    }
                    put("{me}") {
                        val userId = call.sessions.get<UserSession>()?.userId
                        val password = call.receive<ChangeProfileRequest>().password.trim()
                        if (userId == null)
                            call.respond(HttpStatusCode.Unauthorized, FailResponse(ApiErrors.UNAUTHORIZED))
                        else if (!UserService.validatePassword(password))
                            call.respond(HttpStatusCode.BadRequest, FailResponse(ApiErrors.BAD_PASSWORD))
                        else {
                            call.respond(ApiResponse(UserService.updateUser(userId, password)))
                        }
                    }
                }
            }
        }
    }
}
