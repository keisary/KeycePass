package com.ak.keycepass.desktop.data.database

import com.ak.keycepass.shared.domain.model.Etudiant
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.io.FileOutputStream

/**
 * Service d'importation des étudiants depuis un fichier Excel (.xlsx) ou CSV.
 *
 * Format attendu du fichier Excel :
 * | matricule | nom | prenom | classe_id |
 * |-----------|-----|--------|-----------|
 * | 2024001   | Dupont | Jean | B2_IT  |
 *
 * La première ligne doit être l'en-tête (elle est ignorée).
 */
object ImportService {

    /**
     * Résultat d'un import.
     */
    data class ImportResult(
        val totalLignes: Int,
        val lignesImportees: Int,
        val lignesIgnorees: Int,      // Lignes dupliquées (matricule déjà présent)
        val erreurs: List<String>
    )

    /**
     * Importe les étudiants depuis un fichier Excel (.xlsx ou .xls).
     *
     * @param fichier Le fichier Excel à importer.
     * @return Le résultat de l'import avec les statistiques.
     */
    fun importerDepuisExcel(fichier: File): ImportResult {
        val erreurs = mutableListOf<String>()
        var lignesImportees = 0
        var lignesIgnorees = 0
        var totalLignes = 0

        if (!fichier.exists() || !fichier.canRead()) {
            return ImportResult(0, 0, 0, listOf("Fichier introuvable ou illisible : ${fichier.path}"))
        }

        try {
            WorkbookFactory.create(fichier).use { workbook ->
                val feuille = workbook.getSheetAt(0)
                    ?: return ImportResult(0, 0, 0, listOf("Le fichier Excel ne contient aucune feuille."))

                // Ignorer la première ligne (en-tête)
                val lignes = feuille.rowIterator().asSequence().drop(1).toList()
                totalLignes = lignes.size

                transaction {
                    for ((index, ligne) in lignes.withIndex()) {
                        val numLigne = index + 2 // +2 car on a sauté l'en-tête
                        try {
                            val matricule = ligne.getCell(0)?.toString()?.trim() ?: ""
                            val nom = ligne.getCell(1)?.toString()?.trim() ?: ""
                            val prenom = ligne.getCell(2)?.toString()?.trim() ?: ""
                            val classeId = ligne.getCell(3)?.toString()?.trim() ?: ""

                            if (matricule.isEmpty() || nom.isEmpty() || prenom.isEmpty() || classeId.isEmpty()) {
                                erreurs.add("Ligne $numLigne : données manquantes, ligne ignorée.")
                                lignesIgnorees++
                                continue
                            }

                            // Vérifier si le matricule existe déjà
                            val dejaPresent = EtudiantTable
                                .selectAll()
                                .where { EtudiantTable.matricule eq matricule }
                                .count() > 0

                            if (dejaPresent) {
                                lignesIgnorees++
                            } else {
                                EtudiantTable.insert {
                                    it[EtudiantTable.matricule] = matricule
                                    it[EtudiantTable.nom] = nom
                                    it[EtudiantTable.prenom] = prenom
                                    it[EtudiantTable.classeId] = classeId
                                }
                                lignesImportees++
                            }
                        } catch (e: Exception) {
                            erreurs.add("Ligne $numLigne : erreur inattendue — ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            return ImportResult(totalLignes, lignesImportees, lignesIgnorees,
                erreurs + "Erreur lors de la lecture du fichier : ${e.message}")
        }

        return ImportResult(totalLignes, lignesImportees, lignesIgnorees, erreurs)
    }

    /**
     * Récupère tous les étudiants d'une classe donnée.
     */
    fun getEtudiantsParClasse(classeId: String): List<Etudiant> {
        return transaction {
            EtudiantTable
                .selectAll()
                .where { EtudiantTable.classeId eq classeId }
                .map {
                    Etudiant(
                        idEtudiant = it[EtudiantTable.idEtudiant],
                        matricule = it[EtudiantTable.matricule],
                        nom = it[EtudiantTable.nom],
                        prenom = it[EtudiantTable.prenom],
                        classeId = it[EtudiantTable.classeId],
                        deviceUuid = it[EtudiantTable.deviceUuid]
                    )
                }
        }
    }

    /**
     * Récupère toutes les classes disponibles dans la base.
     */
    fun getAllClasses(): List<String> {
        return transaction {
            EtudiantTable
                .selectAll()
                .map { it[EtudiantTable.classeId] }
                .distinct()
                .sorted()
        }
    }

    /**
     * Génère un fichier Excel type conforme à l'importation.
     */
    fun genererFichierTestExcel(destination: File) {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Etudiants")

        // En-tête
        val header = sheet.createRow(0)
        header.createCell(0).setCellValue("matricule")
        header.createCell(1).setCellValue("nom")
        header.createCell(2).setCellValue("prenom")
        header.createCell(3).setCellValue("classe_id")

        // Données
        val data = listOf(
            listOf("2026_B2IT_001", "Koffi", "Jean-Emmanuel", "B2_IT"),
            listOf("2026_B2IT_002", "Traore", "Mariam", "B2_IT"),
            listOf("2026_B2IT_003", "Kouadio", "Adjoua", "B2_IT"),
            listOf("2026_B1IT_001", "N'Guessan", "Serge", "B1_IT"),
            listOf("2026_B1IT_002", "Bamba", "Ali", "B1_IT"),
            listOf("2026_B3M_001", "Ouattara", "Awa", "B3_MANAGEMENT")
        )

        data.forEachIndexed { index, rowData ->
            val row = sheet.createRow(index + 1)
            rowData.forEachIndexed { cellIndex, cellValue ->
                row.createCell(cellIndex).setCellValue(cellValue)
            }
        }

        FileOutputStream(destination).use { out ->
            workbook.write(out)
        }
        workbook.close()
    }
}

