package com.ak.keycepass.shared.domain.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull

/**
 * Suite de tests unitaires pour valider la sérialisation et la désérialisation
 * JSON des modèles de domaine (Etudiant, Seance, Emargement).
 */
class ModelSerializationTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    @Test
    fun testEtudiantSerializationFull() {
        val etudiant = Etudiant(
            idEtudiant = 42,
            matricule = "ETU2026001",
            nom = "Dupont",
            prenom = "Jean",
            classeId = "B2_IT",
            deviceUuid = "123e4567-e89b-12d3-a456-426614174000"
        )

        val serialized = json.encodeToString(etudiant)
        
        // Vérification sommaire de la structure JSON
        assert(serialized.contains("\"matricule\": \"ETU2026001\""))
        assert(serialized.contains("\"idEtudiant\": 42"))
        assert(serialized.contains("\"deviceUuid\": \"123e4567-e89b-12d3-a456-426614174000\""))

        val deserialized = json.decodeFromString<Etudiant>(serialized)
        assertEquals(etudiant, deserialized)
    }

    @Test
    fun testEtudiantSerializationNullableFields() {
        val etudiant = Etudiant(
            matricule = "ETU2026002",
            nom = "Martin",
            prenom = "Sophie",
            classeId = "B2_IT"
            // idEtudiant et deviceUuid doivent être nuls par défaut
        )

        val serialized = json.encodeToString(etudiant)
        val deserialized = json.decodeFromString<Etudiant>(serialized)
        
        assertNull(deserialized.idEtudiant)
        assertNull(deserialized.deviceUuid)
        assertEquals("Martin", deserialized.nom)
        assertEquals(etudiant, deserialized)
    }

    @Test
    fun testSeanceSerialization() {
        val seance = Seance(
            idSeance = 101,
            nomMatiere = "Développement Multiplateforme Kotlin",
            classeId = "B2_IT",
            dateJour = "2026-06-11",
            heureDebut = "08:00:00",
            heureFin = "11:30:00",
            statutSeance = StatutSeance.EN_COURS
        )

        val serialized = json.encodeToString(seance)
        assert(serialized.contains("\"statutSeance\": \"EN_COURS\""))
        
        val deserialized = json.decodeFromString<Seance>(serialized)
        assertEquals(seance, deserialized)
        assertEquals(StatutSeance.EN_COURS, deserialized.statutSeance)
    }

    @Test
    fun testSeanceSerializationNullId() {
        val seance = Seance(
            nomMatiere = "Anglais Professionnel",
            classeId = "B2_MGT",
            dateJour = "2026-06-12",
            heureDebut = "14:00:00",
            heureFin = "17:00:00",
            statutSeance = StatutSeance.PLANIFIE
        )

        val serialized = json.encodeToString(seance)
        val deserialized = json.decodeFromString<Seance>(serialized)
        
        assertNull(deserialized.idSeance)
        assertEquals(StatutSeance.PLANIFIE, deserialized.statutSeance)
        assertEquals(seance, deserialized)
    }

    @Test
    fun testEmargementSerialization() {
        val emargement = Emargement(
            idEmargement = 1001,
            etudiantId = 42,
            seanceId = 101,
            horodatageScanDebut = "2026-06-11T08:02:15",
            horodatageScanFin = "2026-06-11T11:28:45",
            statutFinal = StatutFinal.PRESENT
        )

        val serialized = json.encodeToString(emargement)
        assert(serialized.contains("\"statutFinal\": \"PRESENT\""))

        val deserialized = json.decodeFromString<Emargement>(serialized)
        assertEquals(emargement, deserialized)
        assertEquals(StatutFinal.PRESENT, deserialized.statutFinal)
    }

    @Test
    fun testEmargementDefaultValues() {
        val emargement = Emargement(
            etudiantId = 43,
            seanceId = 101,
            horodatageScanDebut = null,
            horodatageScanFin = null
            // statutFinal doit être EN_ATTENTE par défaut
        )

        val serialized = json.encodeToString(emargement)
        val deserialized = json.decodeFromString<Emargement>(serialized)

        assertNull(deserialized.idEmargement)
        assertNull(deserialized.horodatageScanDebut)
        assertNull(deserialized.horodatageScanFin)
        assertEquals(StatutFinal.EN_ATTENTE, deserialized.statutFinal)
        assertEquals(emargement, deserialized)
    }

    @Test
    fun testStatutFinalEnumValues() {
        // Validation des valeurs d'énumération attendues pour StatutFinal
        assertEquals("EN_ATTENTE", StatutFinal.EN_ATTENTE.name)
        assertEquals("PRESENT", StatutFinal.PRESENT.name)
        assertEquals("RETARD", StatutFinal.RETARD.name)
        assertEquals("ABSENT", StatutFinal.ABSENT.name)
    }

    @Test
    fun testStatutSeanceEnumValues() {
        // Validation des valeurs d'énumération attendues pour StatutSeance
        assertEquals("PLANIFIE", StatutSeance.PLANIFIE.name)
        assertEquals("EN_COURS", StatutSeance.EN_COURS.name)
        assertEquals("CLOTURE_ENSEIGNANT", StatutSeance.CLOTURE_ENSEIGNANT.name)
    }
}
