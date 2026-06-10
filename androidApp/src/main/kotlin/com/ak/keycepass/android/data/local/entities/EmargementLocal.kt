package com.ak.keycepass.android.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entité Room stockant un scan de début de cours en attente de validation.
 *
 * Cycle de vie :
 * - Créée lors du premier scan de l'étudiant (Phase 3).
 * - Lue lors du second scan pour calculer le statut final (Phase 4).
 * - Supprimée une fois le statut final envoyé au serveur Desktop.
 */
@Entity(tableName = "emargement_local")
data class EmargementLocal(
    @PrimaryKey val seanceId: Int,
    val heureScanDebut: String,        // Horodatage ISO-8601 du scan de début
    val statutProvisoire: String,      // "A_L_HEURE" ou "EN_RETARD"
    val envoiConfirme: Boolean = false // true une fois le rapport envoyé au serveur
)
