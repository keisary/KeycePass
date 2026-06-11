package com.ak.keycepass.android.data.local

import android.content.Context
import android.provider.Settings
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionManager(val context: Context) {

    companion object {
        private const val PREFS_FILE = "keycepass_session"
        private const val KEY_MATRICULE = "matricule"
        private const val KEY_NOM = "nom"
        private const val KEY_PRENOM = "prenom"
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

    fun getDeviceUuid(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    val estEnrole: Boolean
        get() = prefs.getBoolean(KEY_EST_ENROLE, false)

    val matricule: String?
        get() = prefs.getString(KEY_MATRICULE, null)

    val nom: String?
        get() = prefs.getString(KEY_NOM, null)

    val prenom: String?
        get() = prefs.getString(KEY_PRENOM, null)

    val deviceUuid: String?
        get() = prefs.getString(KEY_DEVICE_UUID, null)

    val role: UserRole
        get() = runCatching { UserRole.valueOf(prefs.getString(KEY_ROLE, null) ?: UserRole.ETUDIANT.name) }
            .getOrDefault(UserRole.ETUDIANT)

    val serverUrl: String?
        get() = prefs.getString(KEY_SERVER_URL, null)

    fun sauvegarderSession(
        matricule: String,
        nom: String,
        prenom: String,
        deviceUuid: String,
        role: UserRole,
        serverUrl: String
    ) {
        prefs.edit()
            .putString(KEY_MATRICULE, matricule)
            .putString(KEY_NOM, nom)
            .putString(KEY_PRENOM, prenom)
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
