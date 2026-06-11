package com.ak.keycepass.desktop.data.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests unitaires pour SeanceSemaineService.genererTokenSemaine().
 *
 * Le token HMAC-SHA256 est une fonction pure (pas de DB ni de transaction)
 * et doit être :
 * - Déterministe (mêmes entrées → même sortie)
 * - Longueur fixe (64 caractères hex)
 * - Hexadécimal uniquement
 * - Différent pour des entrées différentes
 */
class SeanceSemaineServiceTest {

    @Test
    fun `token est deterministe`() {
        val t1 = SeanceSemaineService.genererTokenSemaine("B2_IT", "2026-W25")
        val t2 = SeanceSemaineService.genererTokenSemaine("B2_IT", "2026-W25")
        assertEquals(t1, t2, "Mêmes entrées doivent produire le même token")
    }

    @Test
    fun `token fait 64 caracteres hex`() {
        val token = SeanceSemaineService.genererTokenSemaine("B1_MANAGEMENT", "2026-W26")
        assertEquals(64, token.length, "HMAC-SHA256 → 32 bytes → 64 hex chars")
    }

    @Test
    fun `token ne contient que des caracteres hex`() {
        val token = SeanceSemaineService.genererTokenSemaine("B3_IT", "2026-W27")
        assertTrue(token.matches(Regex("^[0-9a-f]+$")), "Token doit être hexadécimal uniquement")
    }

    @Test
    fun `classe differente produit token different`() {
        val t1 = SeanceSemaineService.genererTokenSemaine("B2_IT", "2026-W25")
        val t2 = SeanceSemaineService.genererTokenSemaine("B3_IT", "2026-W25")
        assertTrue(t1 != t2, "Classes différentes doivent donner des tokens différents")
    }

    @Test
    fun `semaine differente produit token different`() {
        val t1 = SeanceSemaineService.genererTokenSemaine("B2_IT", "2026-W25")
        val t2 = SeanceSemaineService.genererTokenSemaine("B2_IT", "2026-W26")
        assertTrue(t1 != t2, "Semaines différentes doivent donner des tokens différents")
    }

    @Test
    fun `token commence par un non-zero`() {
        // Probabilité quasi-nulle que le premier hex digit soit 0 pour tous les cas
        val token = SeanceSemaineService.genererTokenSemaine("B2_IT", "2026-W25")
        assertTrue(token[0] != '0', "Premier caractère du token non nul (très probable)")
    }
}
