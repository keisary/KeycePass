package com.ak.keycepass.shared.domain.utils

import com.ak.keycepass.shared.domain.model.StatutFinal
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Suite de tests unitaires pour valider les règles d'attribution des statuts (EF_04).
 */
class StatutUtilsTest {

    @Test
    fun testScanInTimeReturnsPresent() {
        // Premier scan effectué 10 minutes après le début (dans la limite des 15 minutes)
        val status = StatutUtils.determinerStatutFinal(
            heureDebutCoursStr = "08:00:00",
            heurePremierScanStr = "08:10:00",
            secondScanValide = true
        )
        assertEquals(StatutFinal.PRESENT, status)
    }

    @Test
    fun testScanExactlyAt15MinReturnsPresent() {
        // Premier scan effectué pile à 15 minutes
        val status = StatutUtils.determinerStatutFinal(
            heureDebutCoursStr = "08:00:00",
            heurePremierScanStr = "08:15:00",
            secondScanValide = true
        )
        assertEquals(StatutFinal.PRESENT, status)
    }

    @Test
    fun testScanInAdvanceReturnsPresent() {
        // Premier scan effectué en avance (par exemple 5 minutes avant le cours)
        val status = StatutUtils.determinerStatutFinal(
            heureDebutCoursStr = "08:00:00",
            heurePremierScanStr = "07:55:00",
            secondScanValide = true
        )
        assertEquals(StatutFinal.PRESENT, status)
    }

    @Test
    fun testScanLateReturnsRetard() {
        // Premier scan effectué après la limite (16 minutes après le début)
        val status = StatutUtils.determinerStatutFinal(
            heureDebutCoursStr = "08:00:00",
            heurePremierScanStr = "08:16:00",
            secondScanValide = true
        )
        assertEquals(StatutFinal.RETARD, status)
    }

    @Test
    fun testMissingFirstScanReturnsAbsent() {
        // Aucun premier scan enregistré (heurePremierScanStr est null)
        val status = StatutUtils.determinerStatutFinal(
            heureDebutCoursStr = "08:00:00",
            heurePremierScanStr = null,
            secondScanValide = true
        )
        assertEquals(StatutFinal.ABSENT, status)
    }

    @Test
    fun testSecondScanNotValidReturnsAbsent() {
        // Premier scan dans les temps, mais le second scan n'est pas validé
        val status = StatutUtils.determinerStatutFinal(
            heureDebutCoursStr = "08:00:00",
            heurePremierScanStr = "08:05:00",
            secondScanValide = false
        )
        assertEquals(StatutFinal.ABSENT, status)
    }

    @Test
    fun testIso8601ParsingWorksCorrectly() {
        // Test avec des formats d'horodatage complets (ISO-8601) avec millisecondes
        val status = StatutUtils.determinerStatutFinal(
            heureDebutCoursStr = "2026-06-09T08:00:00.000",
            heurePremierScanStr = "2026-06-09T08:12:34.567",
            secondScanValide = true
        )
        assertEquals(StatutFinal.PRESENT, status)
    }
}
