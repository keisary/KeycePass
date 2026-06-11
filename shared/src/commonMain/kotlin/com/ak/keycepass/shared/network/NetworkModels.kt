package com.ak.keycepass.shared.network

import kotlinx.serialization.Serializable

/**
 * Charge utile envoyée lors de l'enregistrement d'un scan.
 * Utilisable aussi bien sur un réseau local Wi-Fi que via Internet (API REST/Cloud).
 *
 * Les champs [lat] et [lon] permettent la vérification de géolocalisation côté serveur
 * pour prévenir la fraude (scan depuis l'extérieur de la salle).
 */
@Serializable
data class ScanPayload(
    val matricule: String,
    val deviceUuid: String,
    val seanceId: Int,
    val timestamp: String,  // Format ISO-8601 (ex. 2026-06-09T08:10:00)
    val scanType: ScanType,
    val lat: Double? = null, // Latitude GPS de l'étudiant au moment du scan
    val lon: Double? = null  // Longitude GPS de l'étudiant au moment du scan
)

@Serializable
enum class ScanType {
    DEBUT, // Premier scan à l'arrivée
    FIN    // Second scan de validation en fin de cours
}

/**
 * Réponse renvoyée par le serveur après la soumission d'un scan.
 *
 * Si [localisationRefusee] est true, l'étudiant est hors du périmètre autorisé (>200 m).
 */
@Serializable
data class ScanResponse(
    val success: Boolean,
    val statutCalcule: String, // PRESENT, RETARD, ABSENT, EN_ATTENTE
    val message: String? = null,
    val localisationRefusee: Boolean = false
)

/**
 * Synthèse d'une séance d'émargement (utilisée par le délégué et l'administration).
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

/**
 * Séance active retournée par GET /api/semaine/{semaineId}/seance-courante.
 * Permet à l'application mobile de connaître la séance en cours après avoir
 * scanné le QR Code hebdomadaire.
 */
@Serializable
data class SeanceCouranteDto(
    val seanceId: Int,
    val nomMatiere: String,
    val heureDebut: String,
    val heureFin: String
)

/**
 * Séance complète pour la synchronisation (enseignant/étudiant).
 */
@Serializable
data class SeanceDto(
    val idSeance: Int,
    val nomMatiere: String,
    val classeId: String,
    val dateJour: String,
    val heureDebut: String,
    val heureFin: String,
    val statutSeance: String
)

