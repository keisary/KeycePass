package com.ak.keycepass.shared.domain.utils

import com.ak.keycepass.shared.domain.model.StatutFinal

/**
 * Gère les algorithmes de calcul du statut de présence (EF_04, US_02, US_03).
 */
object StatutUtils {

    /**
     * Calcule le statut final en croisant l'horodatage du premier scan et la validation du second scan.
     * 
     * @param heureDebutCoursStr L'heure officielle de début du cours (HH:MM:SS ou ISO-8601).
     * @param heurePremierScanStr L'heure du premier scan de l'étudiant (HH:MM:SS, ISO-8601, ou null).
     * @param secondScanValide Indique si le second scan a bien été effectué et validé.
     * @return [StatutFinal] PRESENT, RETARD, ou ABSENT.
     */
    fun determinerStatutFinal(
        heureDebutCoursStr: String,
        heurePremierScanStr: String?,
        secondScanValide: Boolean
    ): StatutFinal {
        // RÈGLE : Si le premier scan OU le second scan est manquant / invalide -> ABSENT
        if (heurePremierScanStr == null || !secondScanValide) {
            return StatutFinal.ABSENT
        }

        try {
            val minDebut = extractMinutes(heureDebutCoursStr)
            val minScan = extractMinutes(heurePremierScanStr)

            val diff = minScan - minDebut

            // RÈGLE : Premier scan entre -infini et +15 min -> PRESENT. Après 15 min -> RETARD
            return if (diff <= 15) {
                StatutFinal.PRESENT
            } else {
                StatutFinal.RETARD
            }
        } catch (e: Exception) {
            // En cas d'erreur de parsing, par défaut ABSENT
            return StatutFinal.ABSENT
        }
    }

    /**
     * Extrait les minutes absolues depuis une chaîne au format "HH:MM:SS", "AAAA-MM-JJ HH:MM:SS" ou "2026-06-09T08:12:34.567".
     */
    private fun extractMinutes(str: String): Int {
        val timePart = when {
            str.contains("T") -> {
                val time = str.split("T")[1]
                time.substring(0, minOf(8, time.length))
            }
            str.contains(" ") -> {
                val time = str.split(" ")[1]
                time.substring(0, minOf(8, time.length))
            }
            else -> str
        }
        val parts = timePart.split(":")
        val hours = parts[0].toIntOrNull() ?: 0
        val minutes = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return hours * 60 + minutes
    }

    fun parseTimeToMinutes(str: String): Int = extractMinutes(str)
}
