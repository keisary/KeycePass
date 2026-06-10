package com.ak.keycepass.android.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ak.keycepass.android.ui.theme.StateAbsent
import com.ak.keycepass.android.ui.theme.StateLate
import com.ak.keycepass.android.ui.theme.StateSuccess
import com.ak.keycepass.android.ui.viewmodel.DelegueViewModel
import com.ak.keycepass.android.ui.viewmodel.StatsUiState
import com.ak.keycepass.shared.domain.model.StatutFinal
import com.ak.keycepass.shared.network.SessionStatusDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DelegateScreen(
    viewModel: DelegueViewModel,
    onBackToLogin: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.statsState.collectAsStateWithLifecycle()

    var selectedSeanceId by remember { mutableIntStateOf(-1) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedSeanceId, refreshTrigger) {
        if (selectedSeanceId > 0) {
            viewModel.chargerStatistiques(selectedSeanceId)
        }
    }

    val gradientBackground = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
    )

    val actionGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF10B981), Color(0xFF059669))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Espace Délégué", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackToLogin) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { refreshTrigger++ }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Rafraîchir", tint = Color.White)
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
            // Saisie de la séance (simulation)
            OutlinedTextField(
                value = if (selectedSeanceId > 0) selectedSeanceId.toString() else "",
                onValueChange = { selectedSeanceId = it.toIntOrNull() ?: -1 },
                label = { Text("ID Séance") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1E293B),
                    unfocusedContainerColor = Color(0xFF1E293B),
                    focusedBorderColor = Color(0xFF818CF8),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color(0xFF818CF8),
                    unfocusedLabelColor = Color(0xFF94A3B8)
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Synthèse et Exportation
            when (uiState) {
                is StatsUiState.Idle -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.6f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sélectionnez une séance pour afficher la synthèse de présence.",
                            color = Color(0xFF94A3B8),
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                is StatsUiState.Chargement -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.6f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF818CF8))
                    }
                }
                is StatsUiState.Erreur -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.6f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (uiState as StatsUiState.Erreur).message,
                            color = Color(0xFFEF4444),
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                is StatsUiState.Succes -> {
                    val report = (uiState as StatsUiState.Succes).stats
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.6f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Synthèse globale : séance ${report.seanceId}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatWidget(
                                label = "Présents",
                                value = report.totalPresents.toString(),
                                color = StateSuccess,
                                modifier = Modifier.weight(1f)
                            )
                            StatWidget(
                                label = "Retards",
                                value = report.totalRetards.toString(),
                                color = StateLate,
                                modifier = Modifier.weight(1f)
                            )
                            StatWidget(
                                label = "Absents",
                                value = report.totalAbsents.toString(),
                                color = StateAbsent,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (false) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        } else {
                            Button(
                                onClick = {
                                    Toast.makeText(context, "Export réseau non connecté pour l'instant", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .background(actionGradient, RoundedCornerShape(12.dp))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, tint = Color.White)
                                    Text(
                                        text = "ENVOYER LE RAPPORT À LA SCOLARITÉ",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatWidget(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8)
            )
        }
    }
}
