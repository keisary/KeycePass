package com.ak.keycepass.android.ui.screens

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ak.keycepass.android.ui.theme.StateSuccess
import com.ak.keycepass.android.ui.viewmodel.ScanViewModel
import com.ak.keycepass.shared.domain.model.Seance
import com.ak.keycepass.shared.domain.model.StatutSeance
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherScreen(
    viewModel: ScanViewModel,
    onBackToLogin: () -> Unit
) {
    val seances by viewModel.seances.collectAsState()
    val activeSeance by viewModel.activeSeance.collectAsState()
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(activeSeance) {
        activeSeance?.let { seance ->
            val qrText = "teacher-close-${seance.idSeance}"
            qrBitmap = generateQrCode(qrText, 512)
        } ?: run {
            qrBitmap = null
        }
    }

    val gradientBackground = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Espace Enseignant", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackToLogin) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color.Transparent,
        modifier = Modifier.background(gradientBackground)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Sélectionnez votre cours :",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF94A3B8)
            )

            if (seances.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.4f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucune séance disponible.\nSynchronisation en cours...",
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.4f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(seances) { seance ->
                        val isSelected = activeSeance?.idSeance == seance.idSeance
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF312E81) else Color(0xFF1E293B)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectSeance(seance) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = seance.nomMatiere,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${seance.dateJour} | ${seance.heureDebut}-${seance.heureFin}",
                                        fontSize = 13.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Text(
                                        text = "Classe : ${seance.classeId}",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                if (seance.statutSeance == StatutSeance.CLOTURE_ENSEIGNANT) {
                                    Box(
                                        modifier = Modifier
                                            .background(StateSuccess.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Clôturé", fontSize = 11.sp, color = StateSuccess, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Divider(color = Color(0xFF334155))

            if (activeSeance == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.6f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Veuillez sélectionner un cours pour générer le code de clôture.",
                        color = Color(0xFF94A3B8),
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.6f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Code de validation de fin de cours",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Appuyez d'abord sur \"Clôturer\" pour activer le scan de fin, puis montrez le QR.",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    val estCloture = activeSeance?.statutSeance == StatutSeance.CLOTURE_ENSEIGNANT

                    if (!estCloture) {
                        Button(
                            onClick = { viewModel.cloturerSeanceActive() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.QrCode, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clôturer la séance", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        qrBitmap?.let { bitmap ->
                            Box(
                                modifier = Modifier
                                    .size(200.dp)
                                    .background(Color.White, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "QR Code Clôture Enseignant",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Text(
                            text = "Séance clôturée ✓ | ID : ${activeSeance?.idSeance}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StateSuccess
                        )

                        Text(
                            text = "Montrez ce QR Code aux étudiants pour qu'ils scannent la fin du cours.",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

private fun generateQrCode(text: String, size: Int): Bitmap? {
    return try {
        val bitMatrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(
                    x,
                    y,
                    if (bitMatrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE
                )
            }
        }
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
