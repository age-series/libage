package org.ageseries.libage.api

import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*

val gson = Gson()

data class LibageApiResponse(val crashed: Boolean, val why: String?, val json: String?, val code: Int) {
    suspend fun respond(call: ApplicationCall) {
        call.respondText(ContentType.Application.Json, HttpStatusCode.fromValue(code)) {
            gson.toJson(
                mapOf(
                    Pair("crashed", crashed),
                    Pair("why", why),
                    Pair("result", json)
                )
            )
        }
    }
}

suspend fun ApplicationCall.libageApiResponse(provider: suspend () -> String?) {
    try {
        val output = provider.invoke()
        LibageApiResponse(false, null, output, 200).respond(this)
    } catch (e: Exception) {
        LibageApiResponse(true, e.stackTraceToString(), null, 500).respond(this)
    }
}
