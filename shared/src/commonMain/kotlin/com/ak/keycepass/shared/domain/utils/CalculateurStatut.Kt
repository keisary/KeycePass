package com.ak.keycepass.shared.domain.utils

import com.ak.keycepass.shared.domain.model.StatutFinal

/**
 * Gère les algorithmes de calcul du temps et d'attribution des statuts de présence.
 * Implémente la règle stricte des 15 minutes (EF 04, US_02, US_03).
 */
object CalculateurStatut {

    /**
     * Convertit une chaîne de caractères au format "HH:MM:SS" en un nombre total de minutes.
     * Exemple : "08:15:00" -> (8 * 60) + 15 = 495 minutes.
     */
    private fun convertirEnMinutes(heureStr: String): Int {
        val parties = heureStr.split(":")
        val heures = parties[0].toInt()
        val minutes = parties[1].toInt()
        return (heures * 60) + minutes
    }

    /**
     * Calcule le statut final de l'étudiant en croisant les données des deux scans.
     * * @param heureDebutCours L'heure officielle du début du cours (ex: "08:00:00")
     * @param heureScanDebut L'heure du premier scan de l'étudiant (null s'il ne l'a pas fait)
     * @param heureScanFin L'heure du second scan de l'étudiant (null s'il ne l'a pas fait)
     * @return [StatutFinal] PRESENT, RETARD ou ABSENT
     */
    fun determinerStatutFinal(
        heureDebutCours: String,
        heureScanDebut: String?,
        heureScanFin: String?
    ): StatutFinal {
        
        // RÈGLE : Si le premier scan OU le second scan est manquant -> ABSENT (EF 04 / US_03)
        if (heureScanDebut == null || heureScanFin == null) {
            return StatutFinal.ABSENT
        }

        // Conversion des heures en minutes pour pouvoir faire des soustractions simples
        val minutesDebutCours = convertirEnMinutes(heureDebutCours)
        val minutesScanDebut = convertirEnMinutes(heureScanDebut)

        // Calcul du retard en minutes au début du cours
        val differenceMinutes = minutesScanDebut - minutesDebutCours

        // RÈGLE : Premier scan entre 0 et 15 min après le début -> PRESENT (EF 04 / US_03)
        return if (differenceMinutes <= 15) {
            StatutFinal.PRESENT
        } else {
            // RÈGLE : Premier scan après 15 min -> RETARD (EF 04 / US_03)
            StatutFinal.RETARD
        }
    }
}