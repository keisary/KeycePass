package com.ak.keycepass.android.reseau

import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object ClientReseau {
    private const val BALISE = "ClientReseau"
    private const val DELAI_MS = 15_000

    private val http = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend inline fun <reified T> obtenirJson(
        urlBase: String,
        chemin: String,
        entetes: Map<String, String> = emptyMap()
    ): T {
        val cheminSur = if (chemin.startsWith("/")) chemin else "/$chemin"
        return http.get("$urlBase$cheminSur") {
            contentType(ContentType.Application.Json)
            entetes.forEach { (cle, valeur) -> header(cle, valeur) }
            timeout { requestTimeoutMillis = DELAI_MS }
        }.body()
    }

    suspend inline fun <reified T, reified B> envoyerJson(
        urlBase: String,
        chemin: String,
        corps: B,
        entetes: Map<String, String> = emptyMap()
    ): T {
        val cheminSur = if (chemin.startsWith("/")) chemin else "/$chemin"
        return http.post("$urlBase$cheminSur") {
            contentType(ContentType.Application.Json)
            setBody(corps)
            entetes.forEach { (cle, valeur) -> header(cle, valeur) }
            timeout { requestTimeoutMillis = DELAI_MS }
        }.body()
    }
}
