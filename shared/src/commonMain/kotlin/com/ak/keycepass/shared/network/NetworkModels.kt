package com.ak.keycepass.shared.network

import kotlinx.serialization.Serializable

/**
 * Charge utile envoyée lors de l'enregistrement d'un scan.
 * Utilisable aussi bien sur un réseau local qu'à travers l'internet global (API REST/Cloud).
 */
@Serializable
data class ScanPayload(
    val matricule: String,
    val deviceUuid: String,
    val seanceId: Int,
    val timestamp: String, // Format ISO-8601 (ex. 2026-06-09T08:10:00)
    val scanType: ScanType
)

@Serializable
enum class ScanType {
    DEBUT, // Premier scan à l'arrivée
    FIN    // Second scan de validation en fin de cours
}

/**
 * Réponse renvoyée par le serveur après la soumission d'un scan.
 */
@Serializable
data class ScanResponse(
    val success: Boolean,
    val statutCalcule: String, // PRESENT, RETARD, ABSENT, EN_ATTENTE
    val message: String? = null
)

/**
 * Synthèse d'une séance d'émargement (utilisée notamment par le délégué et l'administration).
 */
@Serializable
data class SessionStatusDto(
    val seanceId: Int,
    val totalInscrits: Int,
    val totalPresents: Int,
    val totalRetards: Int,
    val totalAbsents: Int,
    val cloture: Boolean
)
