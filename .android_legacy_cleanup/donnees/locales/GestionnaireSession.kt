package com.ak.keycepass.android.donnees.locales

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

object GestionnaireSession {
    private const val PREF = "keycepass_prefs"
    private const val CLE_MATRICULE = "matricule"
    private const val CLE_IDENTIFIANT_APPAREIL = "identifiant_appareil"
    private const val CLE_ROLE = "role"
    private const val CLE_IDENTIFIANT_CLASSE = "identifiant_classe"
    private const val CLE_URL_SERVEUR = "url_serveur"

    private fun preferences(context: Context) = EncryptedSharedPreferences.create(
        PREF,
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun sauvegarderSession(
        context: Context,
        matricule: String,
        identifiantAppareil: String,
        role: String,
        identifiantClasse: String,
        urlServeur: String
    ) {
        preferences(context).edit().putString(CLE_MATRICULE, matricule)
            .putString(CLE_IDENTIFIANT_APPAREIL, identifiantAppareil)
            .putString(CLE_ROLE, role)
            .putString(CLE_IDENTIFIANT_CLASSE, identifiantClasse)
            .putString(CLE_URL_SERVEUR, urlServeur)
            .apply()
    }

    fun matricule(context: Context): String? = preferences(context).getString(CLE_MATRICULE, null)
    fun identifiantAppareil(context: Context): String? = preferences(context).getString(CLE_IDENTIFIANT_APPAREIL, null)
    fun role(context: Context): String? = preferences(context).getString(CLE_ROLE, null)
    fun identifiantClasse(context: Context): String? = preferences(context).getString(CLE_IDENTIFIANT_CLASSE, null)
    fun urlServeur(context: Context): String? = preferences(context).getString(CLE_URL_SERVEUR, null)
    fun effacer(context: Context) { preferences(context).edit().clear().apply() }
}
