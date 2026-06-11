package com.ak.keycepass.shared.domain.utils

import com.ak.keycepass.shared.domain.model.StatutFinal
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Suite de tests unitaires pour valider les règles d'attribution des statuts (EF_04).
 *
 * Règles du cahier des charges :
 * - Présent  : Premier scan entre [0 - 15 min] ET second scan validé.
 * - En retard : Premier scan après [> 15 min] ET second scan validé.
 * - Absent   : Premier scan OU second scan manquant.
 */
class StatutUtilsTest {

    // ══════════════════════════════════════════════════════════════
    //  CAS NOMINAUX — Statut PRESENT
    // ══════════════════════════════════════════════════════════════

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
        // Premier scan effectué pile à 15 minutes — c'est la frontière, doit être PRESENT
        val status = StatutUtils.determinerStatutFinal(
            heureDebutCoursStr = "08:00:00",
            heurePremierScanStr = "08:15:00",
            secondScanValide = true
        )
        assertEquals(StatutFinal.PRESENT, status)
    }

    @Test
    fun testScanInAdvanceReturnsPresent() {
        // Premier scan effectué en avance (5 minutes avant le cours)
        val status = StatutUtils.determinerStatutFinal(
            heureDebutCoursStr = "08:00:00",
            heurePremierScanStr = "07:55:00",
            secondScanValide = true
        )
        assertEquals(StatutFinal.PRESENT, status)
    }

    @Test
    fun testScanExactlyAtStartReturnsPresent() {
        // Premier scan effectué pile à l'heure du début (0 min de retard)
        val status = StatutUtils.determinerStatutFinal(
            heureDebutCoursStr = "09:30:00",
            heurePremierScanStr = "09:30:00",
            secondScanValide = true
        )
        assertEquals(StatutFinal.PRESENT, status)
    }

    @Test
    fun testScanAt1MinuteReturnsPresent() {
        // Premier scan 1 minute après le début
        val status = StatutUtils.determinerStatutFinal(
            heureDebutCoursStr = "14:00:00",
            heurePremierScanStr = "14:01:00",
            secondScanValide = true
        )
        assertEquals(StatutFinal.PRESENT, status)
    }

    // ══════════════════════════════════════════════════════════════
    //  CAS NOMINAUX — Statut RETARD
    // ══════════════════════════════════════════════════════════════

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
    fun testScanVeryLateReturnsRetard() {
        // Premier scan effectué 1 heure après le début — toujours RETARD tant que les 2 scans existent
        val status = StatutUtils.determinerStatutFinal(
            heureDebutCoursStr = "08:00:00",
            heurePremierScanStr = "09:00:00",
            secondScanValide = true
        )
        assertEquals(StatutFinal.RETARD, status)
    }

    @Test
    fun testScanAt30MinutesLateReturnsRetard() {
        // 30 minutes de retard pour un cours de 10h
        val status = StatutUtils.determinerStatutFinal(
            heureDebutCoursStr = "10:00:00",
            heurePremierScanStr = "10:30:00",
            secondScanValide = true
        )
        assertEquals(StatutFinal.RETARD, status)
    }

    // ══════════════════════════════════════════════════════════════
    //  CAS NOMINAUX — Statut ABSENT
    // ══════════════════════════════════════════════════════════════

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
    fun testBothScansMissingReturnsAbsent() {
        // Aucun scan du tout : premier scan null ET second scan non validé
        val status = StatutUtils.determinerStatutFinal(
            heureDebutCoursStr = "08:00:00",
            heurePremierScanStr = null,
            secondScanValide = false
        )
        assertEquals(StatutFinal.ABSENT, status)
    }

    @Test
    fun testLateFirstScanWithoutSecondScanReturnsAbsent() {
        // Premier scan en retard MAIS sans second scan → ABSENT (pas RETARD)
        val status = StatutUtils.determinerStatutFinal(
            heureDebutCoursStr = "08:00:00",
            heurePremierScanStr = "08:20:00",
            secondScanValide = false
        )
        assertEquals(StatutFinal.ABSENT, status)
    }

    @Test
    fun testEmptyFirstScanStringReturnsAbsent() {
        // Chaîne vide au lieu de null pour le premier scan
        val status = StatutUtils.determinerStatutFinal(
            heureDebutCoursStr = "08:00:00",
            heurePremierScanStr = "",
            secondScanValide = true
        )
        assertEquals(StatutFinal.ABSENT, status)
    }

    // ══════════════════════════════════════════════════════════════
    //  FORMATS DE TEMPS — Compatibilité ISO-8601 et variantes
    // ══════════════════════════════════════════════════════════════

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

    @Test
    fun testIso8601LateReturnsRetard() {
        // ISO-8601 complet avec retard (18 min après le début)
        val status = StatutUtils.determinerStatutFinal(
            heureDebutCoursStr = "2026-06-10T14:00:00",
            heurePremierScanStr = "2026-06-10T14:18:00",
            secondScanValide = true
        )
        assertEquals(StatutFinal.RETARD, status)
    }

    @Test
    fun testIso8601WithSpaceSeparator() {
        // Format ISO-8601 avec espace au lieu de T (ex: "2026-06-09 08:00:00")
        val status = StatutUtils.determinerStatutFinal(
            heureDebutCoursStr = "2026-06-09 08:00:00",
            heurePremierScanStr = "2026-06-09 08:10:00",
            secondScanValide = true
        )
        assertEquals(StatutFinal.PRESENT, status)
    }

    @Test
    fun testFormatHHMMWithoutSeconds() {
        // Format court HH:MM sans les secondes
        val status = StatutUtils.determinerStatutFinal(
            heureDebutCoursStr = "08:00",
            heurePremierScanStr = "08:10",
            secondScanValide = true
        )
        assertEquals(StatutFinal.PRESENT, status)
    }

    // ══════════════════════════════════════════════════════════════
    //  PARSING — Tests directs de parseTimeToMinutes()
    // ══════════════════════════════════════════════════════════════

    @Test
    fun testParseTimeSimpleHHMMSS() {
        // Format standard HH:MM:SS
        assertEquals(480, StatutUtils.parseTimeToMinutes("08:00:00"))  // 8h = 480 min
        assertEquals(495, StatutUtils.parseTimeToMinutes("08:15:00"))  // 8h15 = 495 min
        assertEquals(0, StatutUtils.parseTimeToMinutes("00:00:00"))    // Minuit = 0 min
        assertEquals(1439, StatutUtils.parseTimeToMinutes("23:59:00")) // 23h59 = 1439 min
    }

    @Test
    fun testParseTimeIso8601() {
        // Format ISO-8601 complet — seule la partie heure doit être extraite
        assertEquals(480, StatutUtils.parseTimeToMinutes("2026-06-09T08:00:00"))
        assertEquals(492, StatutUtils.parseTimeToMinutes("2026-06-09T08:12:34.567"))
    }

    @Test
    fun testParseTimeEmptyStringReturnsZero() {
        // Chaîne vide — doit retourner 0 sans exception
        assertEquals(0, StatutUtils.parseTimeToMinutes(""))
    }

    @Test
    fun testParseTimeHHMMWithoutSeconds() {
        // Format court sans secondes
        assertEquals(510, StatutUtils.parseTimeToMinutes("08:30"))  // 8h30 = 510 min
        assertEquals(840, StatutUtils.parseTimeToMinutes("14:00"))  // 14h = 840 min
    }
}
