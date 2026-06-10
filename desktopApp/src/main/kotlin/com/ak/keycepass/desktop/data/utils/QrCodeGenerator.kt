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
    fun genererQrEnrolement(classeId: String, token: String, serverUrl: String): BufferedImage {
        val encodedUrl = java.net.URLEncoder.encode(serverUrl, "UTF-8")
        val contenu = "keycepass://enrolement?classeId=$classeId&token=$token&serverUrl=$encodedUrl"
        return genererQrCode(contenu)
    }

    /**
     * Génère un QR Code de présence **hebdomadaire** pour une classe.
     *
     * Ce QR code est affiché par l'administration chaque semaine.
     * Il est unique par classe par semaine et ne change pas au cours de la semaine.
     *
     * Contenu : keycepass://presence?semaineId={id}&classeId={cId}&token={tok}&serverUrl={url}
     *
     * Le [tokenSemaine] est un HMAC-SHA256 signé côté serveur (anti-falsification).
     * Le [serverUrl] permet à l'app Android de connaître l'adresse du serveur Desktop.
     *
     * @param semaineId L'identifiant unique de la semaine dans la base
     * @param classeId L'identifiant de la classe
     * @param tokenSemaine Le token HMAC-SHA256 généré par SeanceSemaineService
     * @param serverUrl L'URL du serveur Desktop (ex. "http://192.168.1.10:8080")
     * @return BufferedImage du QR code
     */
    fun genererQrPresenceSemaine(
        semaineId: Int,
        classeId: String,
        tokenSemaine: String,
        serverUrl: String
    ): BufferedImage {
        val encodedUrl = java.net.URLEncoder.encode(serverUrl, "UTF-8")
        val contenu = "keycepass://presence?semaineId=$semaineId&classeId=$classeId&token=$tokenSemaine&serverUrl=$encodedUrl"
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
