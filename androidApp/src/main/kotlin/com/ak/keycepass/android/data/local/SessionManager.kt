package com.ak.keycepass.android.data.local

import android.content.Context
import android.provider.Settings
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Gestionnaire de session locale sécurisée.
 *
 * Stocke dans un fichier de préférences chiffré (EncryptedSharedPreferences) :
 * - Le matricule de l'utilisateur
 * - L'UUID matériel du téléphone (Device Binding — ENF_01)
 * - Le rôle attribué (ETUDIANT, DELEGUE, ENSEIGNANT)
 * - L'URL du serveur Ktor du poste Desktop (extraite du QR Code d'enrôlement)
 */
class SessionManager(context: Context) {

    companion object {
        private const val PREFS_FILE = "keycepass_session"
        private const val KEY_MATRICULE = "matricule"
        private const val KEY_DEVICE_UUID = "device_uuid"
        private const val KEY_ROLE = "role"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_EST_ENROLE = "est_enrole"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * Récupère l'UUID matériel unique du téléphone (ANDROID_ID).
     * Stable pour la durée de vie de l'installation.
     */
    fun getDeviceUuid(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    // ─── Lecture ──────────────────────────────────────────────────────────────

    val estEnrole: Boolean
        get() = prefs.getBoolean(KEY_EST_ENROLE, false)

    val matricule: String?
        get() = prefs.getString(KEY_MATRICULE, null)

    val deviceUuid: String?
        get() = prefs.getString(KEY_DEVICE_UUID, null)

    val role: UserRole
        get() = UserRole.valueOf(prefs.getString(KEY_ROLE, UserRole.ETUDIANT.name) ?: UserRole.ETUDIANT.name)

    val serverUrl: String?
        get() = prefs.getString(KEY_SERVER_URL, null)

    // ─── Écriture (appelé une seule fois lors de l'enrôlement) ────────────────

    fun sauvegarderSession(
        matricule: String,
        deviceUuid: String,
        role: UserRole,
        serverUrl: String
    ) {
        prefs.edit()
            .putString(KEY_MATRICULE, matricule)
            .putString(KEY_DEVICE_UUID, deviceUuid)
            .putString(KEY_ROLE, role.name)
            .putString(KEY_SERVER_URL, serverUrl)
            .putBoolean(KEY_EST_ENROLE, true)
            .apply()
    }

    fun effacerSession() {
        prefs.edit().clear().apply()
    }
}

/**
 * Rôles disponibles dans l'application mobile.
 */
enum class UserRole {
    ETUDIANT,
    DELEGUE,
    ENSEIGNANT
}
