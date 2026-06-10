package com.ak.keycepass.desktop.data.utils

import kotlin.math.*

/**
 * Utilitaires de géolocalisation pour la vérification anti-fraude.
 *
 * Rayon de tolérance global par défaut : 200 mètres.
 * Si un étudiant scanne le QR code depuis un endroit situé à plus de 200 m
 * des coordonnées de référence saisies par l'administration, le scan est refusé.
 */
object GeoUtils {

    private const val RAYON_TERRE_KM = 6371.0

    /**
     * Calcule la distance en mètres entre deux points GPS en utilisant
     * la formule de Haversine (précise pour des distances courtes).
     *
     * @param lat1 Latitude du point 1 (degrés décimaux)
     * @param lon1 Longitude du point 1 (degrés décimaux)
     * @param lat2 Latitude du point 2 (degrés décimaux)
     * @param lon2 Longitude du point 2 (degrés décimaux)
     * @return Distance en mètres entre les deux points
     */
    fun distanceMetres(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return RAYON_TERRE_KM * c * 1000.0 // Conversion km → mètres
    }

    /**
     * Vérifie si les coordonnées GPS du scan sont dans le périmètre autorisé.
     *
     * @param latScan Latitude GPS de l'étudiant au moment du scan
     * @param lonScan Longitude GPS de l'étudiant au moment du scan
     * @param latRef Latitude GPS de référence (saisie par l'admin)
     * @param lonRef Longitude GPS de référence (saisie par l'admin)
     * @param rayonM Rayon de tolérance en mètres (défaut : 200)
     * @return true si l'étudiant est dans le périmètre autorisé
     */
    fun localisationValide(
        latScan: Double,
        lonScan: Double,
        latRef: Double,
        lonRef: Double,
        rayonM: Int = 200
    ): Boolean {
        val distance = distanceMetres(latScan, lonScan, latRef, lonRef)
        return distance <= rayonM
    }
}
