package com.ak.keycepass.shared.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Suite de tests unitaires pour valider la sérialisation et la désérialisation
 * JSON des modèles réseau (ScanPayload, ScanResponse, SessionStatusDto, SeanceCouranteDto).
 */
class NetworkModelsTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun testScanPayloadSerializationFull() {
        val payload = ScanPayload(
            matricule = "ETU2026001",
            deviceUuid = "123e4567-e89b-12d3-a456-426614174000",
            seanceId = 42,
            timestamp = "2026-06-11T08:15:30",
            scanType = ScanType.DEBUT,
            lat = 43.6107,
            lon = 3.8767
        )

        val serialized = json.encodeToString(payload)
        
        // Vérification de la structure JSON
        assert(serialized.contains("\"matricule\": \"ETU2026001\""))
        assert(serialized.contains("\"scanType\": \"DEBUT\""))
        assert(serialized.contains("\"lat\": 43.6107"))
        assert(serialized.contains("\"lon\": 3.8767"))

        val deserialized = json.decodeFromString<ScanPayload>(serialized)
        assertEquals(payload, deserialized)
        assertEquals(43.6107, deserialized.lat)
        assertEquals(3.8767, deserialized.lon)
    }

    @Test
    fun testScanPayloadSerializationNullGps() {
        val payload = ScanPayload(
            matricule = "ETU2026002",
            deviceUuid = "987f6543-e21b-32d1-b654-246614174999",
            seanceId = 43,
            timestamp = "2026-06-11T12:00:00",
            scanType = ScanType.FIN
            // lat et lon doivent être nuls par défaut
        )

        val serialized = json.encodeToString(payload)
        val deserialized = json.decodeFromString<ScanPayload>(serialized)

        assertNull(deserialized.lat)
        assertNull(deserialized.lon)
        assertEquals(ScanType.FIN, deserialized.scanType)
        assertEquals(payload, deserialized)
    }

    @Test
    fun testScanResponseSerialization() {
        val response = ScanResponse(
            success = true,
            statutCalcule = "PRESENT",
            message = "Émargement enregistré avec succès",
            localisationRefusee = false
        )

        val serialized = json.encodeToString(response)
        assert(serialized.contains("\"success\": true"))
        assert(serialized.contains("\"statutCalcule\": \"PRESENT\""))
        assert(serialized.contains("\"localisationRefusee\": false"))

        val deserialized = json.decodeFromString<ScanResponse>(serialized)
        assertEquals(response, deserialized)
    }

    @Test
    fun testScanResponseSerializationDefaultValues() {
        val response = ScanResponse(
            success = false,
            statutCalcule = "ABSENT"
            // message doit être nul, localisationRefusee doit être false par défaut
        )

        val serialized = json.encodeToString(response)
        val deserialized = json.decodeFromString<ScanResponse>(serialized)

        assertNull(deserialized.message)
        assertFalse(deserialized.localisationRefusee)
        assertEquals(response, deserialized)
    }

    @Test
    fun testScanResponseSerializationLocalisationRefused() {
        val response = ScanResponse(
            success = false,
            statutCalcule = "ABSENT",
            message = "Scan refusé : Hors du périmètre de la classe (distance > 200m)",
            localisationRefusee = true
        )

        val serialized = json.encodeToString(response)
        val deserialized = json.decodeFromString<ScanResponse>(serialized)

        assertTrue(deserialized.localisationRefusee)
        assertEquals("ABSENT", deserialized.statutCalcule)
        assertEquals(response, deserialized)
    }

    @Test
    fun testSessionStatusDtoSerialization() {
        val dto = SessionStatusDto(
            seanceId = 12,
            totalInscrits = 30,
            totalPresents = 25,
            totalRetards = 2,
            totalAbsents = 3,
            cloture = true
        )

        val serialized = json.encodeToString(dto)
        assert(serialized.contains("\"totalPresents\": 25"))
        assert(serialized.contains("\"cloture\": true"))

        val deserialized = json.decodeFromString<SessionStatusDto>(serialized)
        assertEquals(dto, deserialized)
    }

    @Test
    fun testSeanceCouranteDtoSerialization() {
        val dto = SeanceCouranteDto(
            seanceId = 15,
            nomMatiere = "Gestion de Projet",
            heureDebut = "09:00:00",
            heureFin = "12:00:00"
        )

        val serialized = json.encodeToString(dto)
        val deserialized = json.decodeFromString<SeanceCouranteDto>(serialized)

        assertEquals(dto, deserialized)
        assertEquals("Gestion de Projet", deserialized.nomMatiere)
    }

    @Test
    fun testScanTypeEnumValues() {
        assertEquals("DEBUT", ScanType.DEBUT.name)
        assertEquals("FIN", ScanType.FIN.name)
    }
}
