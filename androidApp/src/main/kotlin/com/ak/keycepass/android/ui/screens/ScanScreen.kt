package com.ak.keycepass.android.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ak.keycepass.android.ui.viewmodel.ScanUiState
import com.ak.keycepass.android.ui.viewmodel.ScanViewModel
import com.ak.keycepass.shared.domain.model.Seance
import com.ak.keycepass.shared.domain.model.StatutSeance
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    viewModel: ScanViewModel,
    onBackToLogin: () -> Unit
) {
    val context = LocalContext.current
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val seances by viewModel.seances.collectAsStateWithLifecycle()
    val etudiantClasse by viewModel.etudiantClasse.collectAsStateWithLifecycle()

    var handledError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(scanState) {
        val s = scanState
        if (s is ScanUiState.Erreur && s.message != handledError) {
            handledError = s.message
            Toast.makeText(context, s.message, Toast.LENGTH_LONG).show()
        }
        if (s !is ScanUiState.Erreur) handledError = null
    }

    val permissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    val barcodeLauncher = rememberLauncherForActivityResult(
        contract = ScanContract(),
        onResult = { result ->
            if (result.contents != null) {
                viewModel.traiterResultatScan(result)
            }
        }
    )

    val gradientBackground = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("KeycePass", fontWeight = FontWeight.Bold, color = Color.White) },
                actions = {
                    IconButton(onClick = { viewModel.chargerSeances() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualiser", tint = Color.White)
                    }
                    IconButton(onClick = onBackToLogin) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Déconnexion", tint = Color(0xFFEF4444))
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ─── Profil de l'Étudiant ───
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Avatar d'initiales
                    val initials = viewModel.etudiantNom.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase()
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(54.dp)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF4F46E5))),
                                CircleShape
                            )
                    ) {
                        Text(
                            text = initials,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    Column {
                        Text(
                            text = viewModel.etudiantNom,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF312E81), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = viewModel.etudiantMatricule,
                                    color = Color(0xFFC7D2FE),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (etudiantClasse.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF065F46), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = etudiantClasse,
                                        color = Color(0xFFA7F3D0),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ─── Zone d'Action / État de Scan ───
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.4f)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (!permissions.allPermissionsGranted) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Autorisations requises",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "L'accès à la caméra et à la géolocalisation est indispensable pour valider votre présence en classe.",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { permissions.launchMultiplePermissionRequest() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Accorder les accès", color = Color.White)
                            }
                        }
                    } else {
                        // Animation pulsée pour le scanner
                        val stateInfo = getScanStateInfo(scanState)
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 1.0f,
                            targetValue = if (stateInfo.isPulsing) 1.25f else 1.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200, easing = LinearOutSlowInEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "scale"
                        )
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = if (stateInfo.isPulsing) 0.6f else 0.0f,
                            targetValue = 0.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200, easing = LinearOutSlowInEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "alpha"
                        )

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(160.dp)
                        ) {
                            if (stateInfo.isPulsing) {
                                Box(
                                    modifier = Modifier
                                        .size(110.dp)
                                        .graphicsLayer(scaleX = scale, scaleY = scale)
                                        .background(stateInfo.color.copy(alpha = alpha), CircleShape)
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (stateInfo.canScan) {
                                        val options = ScanOptions().apply {
                                            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                            setPrompt("Placez le QR Code de présence dans le cadre")
                                            setBeepEnabled(true)
                                            setOrientationLocked(false)
                                            captureActivity = com.journeyapps.barcodescanner.CaptureActivity::class.java
                                        }
                                        barcodeLauncher.launch(options)
                                    }
                                },
                                enabled = stateInfo.canScan,
                                modifier = Modifier
                                    .size(100.dp)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(stateInfo.color, stateInfo.color.copy(alpha = 0.8f))
                                        ),
                                        CircleShape
                                    )
                            ) {
                                if (scanState is ScanUiState.Traitement) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(40.dp))
                                } else {
                                    Icon(
                                        imageVector = stateInfo.icon,
                                        contentDescription = "Scan button",
                                        tint = Color.White,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = stateInfo.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = stateInfo.description,
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )

                        if (!stateInfo.canScan && scanState !is ScanUiState.Traitement) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { viewModel.reinitialiser() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                modifier = Modifier.fillMaxWidth(0.6f)
                            ) {
                                Text("Nouveau Scan", color = Color.White)
                            }
                        }
                    }
                }
            }

            // ─── Liste des Séances (Timeline) ───
            Text(
                text = "Planning d'aujourd'hui",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            if (seances.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucun cours programmé aujourd'hui.",
                        color = Color(0xFF64748B),
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(seances) { seance ->
                        SeanceCard(seance)
                    }
                }
            }
        }
    }
}

// ─── Données d'Affichage selon l'État de Scan ───
private data class ScanStateInfo(
    val title: String,
    val description: String,
    val color: Color,
    val icon: ImageVector,
    val isPulsing: Boolean,
    val canScan: Boolean
)

@Composable
private fun getScanStateInfo(state: ScanUiState): ScanStateInfo {
    return when (state) {
        is ScanUiState.Pret -> ScanStateInfo(
            title = "Prêt à scanner",
            description = "Appuyez sur le bouton pour scanner le QR Code de présence hebdomadaire",
            color = Color(0xFF6366F1),
            icon = Icons.Default.QrCodeScanner,
            isPulsing = true,
            canScan = true
        )
        is ScanUiState.Traitement -> ScanStateInfo(
            title = "Vérification",
            description = "Envoi des données au serveur desktop...",
            color = Color(0xFF6366F1),
            icon = Icons.Default.Refresh,
            isPulsing = false,
            canScan = false
        )
        is ScanUiState.AttenteClotureEnseignant -> ScanStateInfo(
            title = "Arrivée enregistrée !",
            description = "Statut : ${state.statutProvisoire}\nEn attente de la clôture du cours par l'enseignant pour effectuer le scan de fin.",
            color = Color(0xFFF59E0B),
            icon = Icons.Default.Lock,
            isPulsing = true,
            canScan = true
        )
        is ScanUiState.StatutFinal -> ScanStateInfo(
            title = "Pointage Terminé ✓",
            description = "Votre présence a été validée définitivement.\nStatut final : ${state.statut}",
            color = Color(0xFF10B981),
            icon = Icons.Default.CheckCircle,
            isPulsing = false,
            canScan = false
        )
        is ScanUiState.SeanceCloturee -> ScanStateInfo(
            title = "Cours Clôturé",
            description = "La séance est terminée.",
            color = Color(0xFF64748B),
            icon = Icons.Default.CheckCircle,
            isPulsing = false,
            canScan = false
        )
        is ScanUiState.Erreur -> ScanStateInfo(
            title = "Erreur de Pointage",
            description = state.message,
            color = Color(0xFFEF4444),
            icon = Icons.Default.Error,
            isPulsing = false,
            canScan = false
        )
    }
}

// ─── Carte de Séance Individuelle ───
@Composable
private fun SeanceCard(seance: Seance) {
    val badgeColor = when (seance.statutSeance) {
        StatutSeance.CLOTURE_ENSEIGNANT -> Color(0xFF64748B)
        StatutSeance.EN_COURS -> Color(0xFF10B981)
        StatutSeance.PLANIFIE -> Color(0xFF6366F1)
    }
    val badgeText = when (seance.statutSeance) {
        StatutSeance.CLOTURE_ENSEIGNANT -> "Clôturé"
        StatutSeance.EN_COURS -> "En cours"
        StatutSeance.PLANIFIE -> "Planifié"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = seance.nomMatiere,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${seance.heureDebut} - ${seance.heureFin}",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = badgeText,
                    color = badgeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
