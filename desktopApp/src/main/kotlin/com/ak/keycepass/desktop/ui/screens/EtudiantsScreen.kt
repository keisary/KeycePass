package com.ak.keycepass.desktop.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.ak.keycepass.desktop.ui.theme.*
import com.ak.keycepass.desktop.ui.viewmodel.AdminViewModel
import com.ak.keycepass.desktop.ui.viewmodel.ImportState
import org.jetbrains.skia.Image
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EtudiantsScreen() {
    val vm = remember { AdminViewModel() }
    val importState by vm.importState.collectAsState()
    val classes by vm.classes.collectAsState()
    val etudiants by vm.etudiants.collectAsState()
    val qrImage by vm.qrCodeImage.collectAsState()

    var selectedClasse by remember { mutableStateOf("") }

    // Charger les classes au démarrage
    LaunchedEffect(Unit) {
        vm.chargerToutesLesClasses()
    }

    // Charger la liste des étudiants quand la classe change
    LaunchedEffect(selectedClasse) {
        if (selectedClasse.isNotEmpty()) {
            vm.selectionnerClasse(selectedClasse)
        }
    }

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        // ── Panneau gauche : Import + Liste des étudiants ──
        Column(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Import Excel Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Import des étudiants", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val chooser = JFileChooser().apply {
                                    dialogTitle = "Sélectionner le fichier Excel"
                                    fileFilter = FileNameExtensionFilter("Fichiers Excel (.xlsx)", "xlsx")
                                }
                                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                    vm.importerExcel(chooser.selectedFile)
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            enabled = importState !is ImportState.Chargement
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Importer .xlsx", fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val chooser = JFileChooser().apply {
                                    dialogTitle = "Enregistrer le fichier modèle"
                                    selectedFile = File("etudiants_test.xlsx")
                                    fileFilter = FileNameExtensionFilter("Fichiers Excel (.xlsx)", "xlsx")
                                }
                                if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                                    var file = chooser.selectedFile
                                    if (!file.name.endsWith(".xlsx")) {
                                        file = File(file.parentFile, file.name + ".xlsx")
                                    }
                                    vm.genererFichierTestExcel(file)
                                }
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Générer modèle", fontSize = 13.sp)
                        }
                    }

                    when (val s = importState) {
                        is ImportState.Chargement -> {
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                            Text("Import en cours...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        is ImportState.Succes -> {
                            Spacer(Modifier.height(8.dp))
                            Text("${s.importees}/${s.total} étudiants importés",
                                fontSize = 12.sp, color = StatusPresent, fontWeight = FontWeight.Medium)
                        }
                        is ImportState.SuccesAvecAvertissements -> {
                            Spacer(Modifier.height(8.dp))
                            Text("${s.importees}/${s.total} importés (${s.avertissements.size} avertissements)",
                                fontSize = 12.sp, color = StatusLate, fontWeight = FontWeight.Medium)
                        }
                        is ImportState.Erreur -> {
                            Spacer(Modifier.height(8.dp))
                            Text(s.message, fontSize = 12.sp, color = StatusAbsent, fontWeight = FontWeight.Medium)
                        }
                        else -> {}
                    }
                }
            }

            // 2. Student List Card
            Card(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(16.dp).fillMaxHeight()) {
                    Text("Liste des étudiants", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))

                    // Sélection classe
                    var classExp by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = classExp, onExpandedChange = { classExp = it }) {
                        OutlinedTextField(
                            value = selectedClasse.ifEmpty { "Choisir une classe..." },
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classExp) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                        )
                        ExposedDropdownMenu(expanded = classExp, onDismissRequest = { classExp = false }) {
                            classes.filter { it != "Toutes" }.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c, fontSize = 12.sp) },
                                    onClick = { selectedClasse = c; classExp = false }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    if (selectedClasse.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Sélectionnez une classe pour voir les étudiants", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else if (etudiants.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Aucun étudiant trouvé dans cette classe", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(etudiants, key = { it.idEtudiant ?: 0 }) { etudiant ->
                                val isEnrolled = !etudiant.deviceUuid.isNullOrEmpty()
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text("${etudiant.prenom} ${etudiant.nom}", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                            Text("Matricule: ${etudiant.matricule}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isEnrolled) StatusPresent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                                        ) {
                                            Text(
                                                if (isEnrolled) "Enrôlé" else "Non enrôlé",
                                                color = if (isEnrolled) StatusPresent else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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

        // ── Panneau droit : QR d'enrôlement ──
        Card(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("QR Code d'enrôlement", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))

                if (selectedClasse.isNotEmpty()) {
                    LaunchedEffect(selectedClasse) {
                        vm.genererQrEnrolement(selectedClasse)
                    }

                    if (qrImage != null) {
                        val bitmap = remember(qrImage) {
                            val baos = ByteArrayOutputStream()
                            ImageIO.write(qrImage, "PNG", baos)
                            Image.makeFromEncoded(baos.toByteArray()).toComposeImageBitmap()
                        }
                        if (bitmap != null) {
                            Box(Modifier.size(240.dp)) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = "QR Code Enrolement",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Scannez avec le téléphone pour lier un étudiant",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            FilledTonalButton(
                                onClick = {
                                    val chooser = JFileChooser().apply {
                                        dialogTitle = "Enregistrer le QR code"
                                        selectedFile = File("qr_enrolement_${selectedClasse}.png")
                                        fileFilter = FileNameExtensionFilter("PNG image", "png")
                                    }
                                    if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                                        com.ak.keycepass.desktop.data.utils.QrCodeGenerator.sauvegarderQrCode(qrImage!!, chooser.selectedFile)
                                    }
                                },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Exporter PNG", fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.QrCode, contentDescription = null,
                                modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                            Spacer(Modifier.height(8.dp))
                            Text("Sélectionnez une classe pour générer le QR d'enrôlement",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}
