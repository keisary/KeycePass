package com.ak.keycepass.desktop.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.jetbrains.skia.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * Génère un vrai QR Code à partir d'un contenu texte.
 * Utilise ZXing (QRCodeWriter) et le convertit en ImageBitmap Compose.
 */
fun generateQRBitmap(content: String, size: Int = 400): ImageBitmap {
    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)

    // BufferedImage monochrome (noir sur blanc)
    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_RGB)
    for (x in 0 until size) {
        for (y in 0 until size) {
            image.setRGB(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
        }
    }

    // Conversion BufferedImage → Skia Image → Compose ImageBitmap
    val baos = ByteArrayOutputStream()
    ImageIO.write(image, "PNG", baos)
    return Image.makeFromEncoded(baos.toByteArray()).toComposeImageBitmap()
}
