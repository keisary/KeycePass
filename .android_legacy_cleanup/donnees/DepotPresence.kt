package com.ak.keycepass.android.donnees

import android.content.Context
import com.ak.keycepass.android.donnees.locales.GestionnaireSession
import com.ak.keycepass.android.donnees.locales.BaseDonneesLocale
import com.ak.keycepass.android.reseau.ClientReseau
import com.ak.keycepass.android.metier.modele.Etudiant
import com.ak.keycepass.android.reseau.ReponseScan
import kotlinx.coroutines.flow.Flow

class DepotPresence(
    private val contexte: Context,
    private val gestionnaireSession: GestionnaireSession,
    private val baseDonnees: BaseDonneesLocale,
    private val clientReseau: ClientReseau = ClientReseau
) {

    suspend fun enroler(
        matricule: String,
        identifiantClasse: String,
        identifiantAppareil: String,
        role: String,
        urlServeur: String
    ): Resultat<Unite> {
        return try {
            val etudiant = Etudiant(matricule = matricule, nom = "", prenom = "", identifiantClasse = identifiantClasse, identifiantAppareil = identifiantAppareil)
            baseDonnees.accesDonnees().insererEtudiant(
                com.ak.keycepass.android.donnees.locales.entites.EtudiantLocal(matricule = matricule, identifiantClasse = identifiantClasse, identifiantAppareil = identifiantAppareil, role = role)
            )
            gestionnaireSession.sauvegarderSession(contexte, matricule, identifiantAppareil, role, identifiantClasse, urlServeur.dropWhile { it == '/' }.removeSuffix("/api"))
            Resultat.succes(Unite)
        } catch (e: Exception) {
            Resultat.echec(e)
        }
    }

    fun observerSeancesParClasse(identifiantClasse: String): Flow<List<com.ak.keycepass.android.donnees.locales.entites.SeanceLocal>> =
        baseDonnees.accesDonnees().observerSeancesParClasse(identifiantClasse)

    fun observerEmargements(idSeance: Int): Flow<List<com.ak.keycepass.android.donnees.locales.entites.EmargementLocal>> =
        baseDonnees.accesDonnees().observerEmargementsParSeance(idSeance)

    suspend fun recupererSeanceCourante(idSemaine: Int, urlBase: String): Resultat<com.ak.keycepass.android.reseau.SeanceCouranteDto> {
        return try {
            val dto = clientReseau.obtenirJson(urlBase, "/api/semaine/$idSemaine/seance-courante", emptyMap())
            Resultat.succes(dto)
        } catch (e: Exception) {
            Resultat.echec(e)
        }
    }

    suspend fun enregistrerScan(
        matricule: String,
        identifiantAppareil: String,
        idSeance: Int,
        typeScan: com.ak.keycepass.android.reseau.TypeScan,
        urlBase: String,
        latitude: Double? = null,
        longitude: Double? = null
    ): Resultat<ReponseScan> {
        val charge = com.ak.keycepass.android.reseau.DonneesScan(
            matricule = matricule,
            identifiantAppareil = identifiantAppareil,
            idSeance = idSeance,
            horodatage = java.time.Instant.now().toString(),
            typeScan = typeScan,
            latitude = latitude,
            longitude = longitude
        )
        return try {
            val reponse = clientReseau.envoyerJson(urlBase, "/api/scan", charge, emptyMap())
            Resultat.succes(reponse)
        } catch (e: Exception) {
            Resultat.echec(e)
        }
    }

    fun utilisateurActuel(): Resultat<Triple<String, String, String>> = runCatching {
        val matricule = gestionnaireSession.matricule(contexte) ?: return Resultat.echec(IllegalStateException("Session introuvable"))
        val identifiantAppareil = gestionnaireSession.identifiantAppareil(contexte) ?: return Resultat.echec(IllegalStateException("Appareil introuvable"))
        val role = gestionnaireSession.role(contexte) ?: return Resultat.echec(IllegalStateException("Role introuvable"))
        Resultat.succes(Triple(matricule, identifiantAppareil, role))
    }.getOrElse { Resultat.echec(it) }
}
