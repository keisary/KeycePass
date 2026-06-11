package com.ak.keycepass.shared.domain.utils

import com.ak.keycepass.shared.domain.model.StatutFinal

/**
 * Utilitaires pour le calcul des statuts de présence des étudiants.
 */
object StatutUtils {

    /**
     * Calcule le statut final de présence d'un étudiant.
     *
     * Règles fonctionnelles (EF_04) :
     * - Présent : Premier scan entre [0 - 15 min] (ou en avance) ET second scan validé.
     * - En retard : Premier scan après [> 15 min] ET second scan validé.
     * - Absent : Premier scan OU second scan manquant.
     *
     * @param heureDebutCoursStr Heure de début officielle du cours (format "HH:MM", "HH:MM:SS" ou ISO-8601)
     * @param heurePremierScanStr Heure ou horodatage complet du premier scan d'entrée, ou null si absent
     * @param secondScanValide Indique si le second scan de fin a été effectué et validé
     * @return Le [StatutFinal] calculé
     */
     
    fun determinerStatutFinal(
        heureDebutCoursStr: String,
        heurePremierScanStr: String?,
        secondScanValide: Boolean
    ): StatutFinal {
        if (heurePremierScanStr.isNullOrEmpty() || !secondScanValide) {
            return StatutFinal.ABSENT
        }

        val minutesDebut = parseTimeToMinutes(heureDebutCoursStr)
        val minutesScan = parseTimeToMinutes(heurePremierScanStr)

        // Différence en minutes (peut être négative si scan en avance)
        val difference = minutesScan - minutesDebut

        return if (difference <= 15) {
            StatutFinal.PRESENT
        } else {
            StatutFinal.RETARD
        }
    }

    /**
     * Extrait l'heure d'une chaîne de caractères (au format HH:MM, HH:MM:SS ou ISO-8601)
     * et la convertit en minutes écoulées depuis minuit.
     */
    fun parseTimeToMinutes(timeOrDateTimeStr: String): Int {
        val trimmed = timeOrDateTimeStr.trim()
        if (trimmed.isEmpty()) return 0

        // Gestion du format ISO-8601 (ex. 2026-06-09T08:15:00 ou 2026-06-09 08:15:00)
        val timePart = when {
            trimmed.contains("T") -> trimmed.substringAfter("T")
            trimmed.contains(" ") -> trimmed.substringAfter(" ")
            else -> trimmed
        }

        // Suppression des éventuelles millisecondes (ex. 08:15:00.321)
        val cleanTime = timePart.substringBefore(".")

        return try {
            val parts = cleanTime.split(":")
            if (parts.isNotEmpty()) {
                val hours = parts[0].toIntOrNull() ?: 0
                val minutes = if (parts.size > 1) parts[1].toIntOrNull() ?: 0 else 0
                hours * 60 + minutes
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }
}
