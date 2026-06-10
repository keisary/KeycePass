package com.ak.keycepass.shared.domain.utils

import com.ak.keycepass.shared.domain.model.Etudiant

/**
 * Gère la logique de sécurité et de vérification du terminal mobile (Device Binding).
 * Répond aux exigences de lutte contre la fraude par complaisance (ENF_01, US_01).
 */
object AntiFraude {

    /**
     * Vérifie si le téléphone utilisé pour scanner est autorisé.
     * * @param etudiant L'étudiant récupéré dans le système.
     * @param deviceUuidActuel L'UUID du smartphone qui est en train de scanner le QR code.
     * @return [Boolean] true si le scan est valide (même appareil ou premier enrôlement), false si c'est une fraude.
     */
    fun validerAppareil(etudiant: Etudiant, deviceUuidActuel: String): Boolean {
        // Cas 1 : Premier scan du semestre (Enrôlement initial)
        // L'étudiant n'a pas encore d'UUID enregistré (égal à null)
        if (etudiant.deviceUuid == null) {
            return true 
            // On autorise le scan pour permettre l'enrôlement initial de l'appareil.
        }

        // Cas 2 : Vérification anti-fraude standard
        // On compare l'UUID enregistré avec l'UUID du téléphone qui scanne actuellement
        return etudiant.deviceUuid == deviceUuidActuel
    }
}