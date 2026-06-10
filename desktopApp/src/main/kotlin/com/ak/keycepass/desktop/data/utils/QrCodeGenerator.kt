package com.ak.keycepass.desktop.data.utils

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Utilitaire de génération de QR Codes pour KeycePass.
 *
 * Deux types de QR Codes sont générés :
 * 1. QR Code d'enrôlement (liaison UUID de l'appareil à un étudiant)
 * 2. QR Code de séance (présence journalière, dynamique et sécurisé)
 */
object QrCodeGenerator {

    private const val QR_SIZE = 400

    /**
     * Génère un QR Code d'enrôlement pour une classe.
     * Ce QR code est scanné une seule fois par les étudiants pour lier leur appareil.
     *
     * Contenu : keycepass://enrolement?classeId={classeId}&token={token}
     *
     * @param classeId Identifiant de la classe (ex. "B2_IT")
     * @param token Jeton de validation généré par le serveur
     * @return BufferedImage du QR code
     */
    fun genererQrEnrolement(classeId: String, token: String): BufferedImage {
        val contenu = "keycepass://enrolement?classeId=$classeId&token=$token"
        return genererQrCode(contenu)
    }

    /**
     * Génère un QR Code de présence pour une séance journalière.
     * Ce QR code change à chaque séance pour empêcher la fraude.
     *
     * Contenu : keycepass://presence?seanceId={seanceId}&dateJeton={dateJeton}
     *
     * @param seanceId L'identifiant unique de la séance
     * @param dateJeton Jeton temporel basé sur la date et l'heure (anti-fraude)
     * @return BufferedImage du QR code
     */
    fun genererQrPresence(seanceId: Int, dateJeton: String): BufferedImage {
        val contenu = "keycepass://presence?seanceId=$seanceId&jeton=$dateJeton"
        return genererQrCode(contenu)
    }

    /**
     * Sauvegarde une image de QR Code dans un fichier PNG.
     *
     * @param image L'image du QR code
     * @param fichier Le fichier de destination
     */
    fun sauvegarderQrCode(image: BufferedImage, fichier: File) {
        fichier.parentFile?.mkdirs()
        ImageIO.write(image, "PNG", fichier)
    }

    /**
     * Génère une image de QR Code à partir d'une chaîne de caractères.
     */
    private fun genererQrCode(contenu: String): BufferedImage {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 2
        )
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(contenu, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints)
        return MatrixToImageWriter.toBufferedImage(bitMatrix)
    }
}
