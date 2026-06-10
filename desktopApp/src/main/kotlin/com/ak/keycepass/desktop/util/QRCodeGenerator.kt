package com.ak.keycepass.desktop.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.jetbrains.skia.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

/**
 * Génère un BufferedImage (AWT) à partir d'un contenu texte.
 * Utilisé pour l'affichage et l'export PNG.
 */
fun generateQRBufferedImage(content: String, size: Int = 400): BufferedImage {
    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)

    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_RGB)
    for (x in 0 until size) {
        for (y in 0 until size) {
            image.setRGB(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
        }
    }
    return image
}

/**
 * Génère un ImageBitmap Compose à partir d'un contenu texte.
 * Utilisé pour l'affichage dans l'interface.
 */
fun generateQRBitmap(content: String, size: Int = 400): ImageBitmap {
    val image = generateQRBufferedImage(content, size)
    val baos = ByteArrayOutputStream()
    ImageIO.write(image, "PNG", baos)
    return Image.makeFromEncoded(baos.toByteArray()).toComposeImageBitmap()
}

/**
 * Sauvegarde un QR code en PNG dans un fichier.
 */
fun saveQRToFile(content: String, file: File, size: Int = 400): Boolean {
    return try {
        val image = generateQRBufferedImage(content, size)
        ImageIO.write(image, "PNG", file)
        true
    } catch (e: Exception) {
        false
    }
}
