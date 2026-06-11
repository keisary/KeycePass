package com.ak.keycepass.desktop.data.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.math.abs

/**
 * Tests unitaires pour GeoUtils — formule de Haversine et vérification
 * de périmètre (anti-fraude géolocalisation).
 *
 * Cas testés :
 * - Distance zéro (même point)
 * - Distance connue entre deux villes
 * - Périmètre valide (< 200m)
 * - Périmètre refusé (> 200m)
 * - Rayon personnalisé
 */
class GeoUtilsTest {

    private val DELTA = 1.0 // Tolérance 1 mètre

    @Test
    fun `meme point donne zero metre`() {
        val d = GeoUtils.distanceMetres(48.8566, 2.3522, 48.8566, 2.3522)
        assertEquals(0.0, d, DELTA)
    }

    @Test
    fun `distance Tour Eiffel a Arc de Triomphe`() {
        // Tour Eiffel
        val lat1 = 48.8584
        val lon1 = 2.2945
        // Arc de Triomphe (~3 km)
        val lat2 = 48.8738
        val lon2 = 2.2950

        val d = GeoUtils.distanceMetres(lat1, lon1, lat2, lon2)
        // La distance réelle est ~1700m, on vérifie l'ordre de grandeur
        assertTrue(d in 1500.0..2000.0)
    }

    @Test
    fun `distance Paris Lyon`() {
        // Paris (Notre-Dame)
        val lat1 = 48.8530
        val lon1 = 2.3499
        // Lyon (Part-Dieu)
        val lat2 = 45.7600
        val lon2 = 4.8600

        val d = GeoUtils.distanceMetres(lat1, lon1, lat2, lon2)
        // ~390 km en ligne droite
        assertTrue(d in 385_000.0..400_000.0, "Distance Paris-Lyon devrait être ~390km mais était $d")
    }

    @Test
    fun `localisation dans le perimetre est valide`() {
        // Référence : Keyce Paris (48.8748, 2.3247)
        // Scan : à ~50m
        val valide = GeoUtils.localisationValide(
            latScan = 48.8748,
            lonScan = 2.3247,
            latRef = 48.8750,
            lonRef = 2.3250,
            rayonM = 200
        )
        assertTrue(valide)
    }

    @Test
    fun `localisation hors perimetre est refusee`() {
        // Référence : Keyce Paris
        // Scan : à ~500m
        val valide = GeoUtils.localisationValide(
            latScan = 48.8790,
            lonScan = 2.3300,
            latRef = 48.8748,
            lonRef = 2.3247,
            rayonM = 200
        )
        assertFalse(valide)
    }

    @Test
    fun `rayon personnalise accepte la distance`() {
        // Référence : centre ville
        // Scan à ~300m, mais rayon autorisé = 500m
        val valide = GeoUtils.localisationValide(
            latScan = 48.8566,
            lonScan = 2.3522,
            latRef = 48.8590,
            lonRef = 2.3550,
            rayonM = 500
        )
        assertTrue(valide)
    }

    @Test
    fun `distance equatoriale approximative`() {
        // 1 degré le long de l'équateur ~ 111 km
        val d = GeoUtils.distanceMetres(0.0, 0.0, 0.0, 1.0)
        assertTrue(abs(d - 111_319.0) < 500.0, "1° à l'équateur ~111km, était $d")
    }

    @Test
    fun `symetrie de la distance`() {
        val d1 = GeoUtils.distanceMetres(48.8566, 2.3522, 43.2965, 5.3698)
        val d2 = GeoUtils.distanceMetres(43.2965, 5.3698, 48.8566, 2.3522)
        assertEquals(d1, d2, DELTA, "La distance doit être symétrique")
    }
}
