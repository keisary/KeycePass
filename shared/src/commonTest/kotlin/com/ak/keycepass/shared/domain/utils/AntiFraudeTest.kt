package com.ak.keycepass.shared.domain.utils

import com.ak.keycepass.shared.domain.model.Etudiant
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests unitaires pour AntiFraude.validerAppareil().
 *
 * Couvre les 3 cas du device binding (ENF_01) :
 * - Premier enrôlement (deviceUuid == null)
 * - Même appareil que l'enrôlement
 * - Appareil différent (fraude par complaisance)
 */
class AntiFraudeTest {

    @Test
    fun `premier enrolement autorise le scan`() {
        val etudiant = Etudiant(
            idEtudiant = 1,
            matricule = "MAT-001",
            nom = "Dupont",
            prenom = "Jean",
            classeId = "B2_IT",
            deviceUuid = null // Pas encore enrôlé
        )
        assertTrue(AntiFraude.validerAppareil(etudiant, "abc-123"))
    }

    @Test
    fun `meme appareil autorise le scan`() {
        val etudiant = Etudiant(
            idEtudiant = 2,
            matricule = "MAT-002",
            nom = "Martin",
            prenom = "Sophie",
            classeId = "B2_IT",
            deviceUuid = "xyz-789"
        )
        assertTrue(AntiFraude.validerAppareil(etudiant, "xyz-789"))
    }

    @Test
    fun `appareil different refuse le scan`() {
        val etudiant = Etudiant(
            idEtudiant = 3,
            matricule = "MAT-003",
            nom = "Bernard",
            prenom = "Paul",
            classeId = "B2_IT",
            deviceUuid = "original-device"
        )
        assertFalse(AntiFraude.validerAppareil(etudiant, "fraudeur-device"))
    }
}
