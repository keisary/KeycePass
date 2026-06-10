package com.ak.keycepass.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.navigation.NavController
import com.ak.keycepass.android.ui.viewmodel.EnrolementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: EnrolementViewModel,
    navController: NavController,
    onBackToLogin: () -> Unit = {}
) {
    val context = LocalContext.current
    var matricule by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf<String?>(null) }

    // Listen to enrollment state
    val state by viewModel.enrolementState.collectAsStateWithLifecycle()
    LaunchedEffect(state) {
        val current = state
        if (current is com.ak.keycepass.android.ui.viewmodel.EnrolementUiState.Succes) {
            navController.navigate("scan")
            viewModel.reinitialiser()
        }
    }

    val gradientBackground = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
    )

    LaunchedEffect(state) {
        val current = state
        if (current is com.ak.keycepass.android.ui.viewmodel.EnrolementUiState.Erreur) {
            android.widget.Toast.makeText(context, current.message, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("KeycePass", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackToLogin) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Bienvenue sur KeycePass",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Inscrivez-vous avec le QR Code fourni par votre administrateur.",
                fontSize = 14.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )

            OutlinedTextField(
                value = matricule,
                onValueChange = { matricule = it },
                label = { Text("Votre matricule") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    containerColor = Color(0xFF1E293B),
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

            Text(text = "Choisissez votre rôle :", color = Color(0xFF94A3B8), fontSize = 14.sp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf("ETUDIANT", "DELEGUE", "ENSEIGNANT").forEach { role ->
                    val selected = selectedRole == role
                    androidx.compose.foundation.clickable(
                        onClick = { selectedRole = role }
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) Color(0xFF312E81) else Color(0xFF1E293B)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = role,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else Color(0xFF94A3B8),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Text(
                text = "(Simulation: sélectionnez un rôle puis cliquez S'inscrire pour continuer.)",
                fontSize = 12.sp,
                color = Color(0xFF475569),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (state is com.ak.keycepass.android.ui.viewmodel.EnrolementUiState.Chargement) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = Color(0xFF818CF8))
            } else {
                // Primary action
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF312E81)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                val role = selectedRole ?: "ETUDIANT"
                                val qrContent = "keycepass://enrolement?classeId=B2_IT&token=DEMO123&serverUrl=http://192.168.1.10:8080&role=$role"
                                viewModel.enroler(context, matricule.trim(), qrContent)
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = "S'INSCRIRE",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
