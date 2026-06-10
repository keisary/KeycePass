package com.ak.keycepass.desktop.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
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
import com.ak.keycepass.desktop.data.service.SeanceSemaineRow
import com.ak.keycepass.desktop.data.service.SeanceRow
import com.ak.keycepass.desktop.data.service.EnseignantRow
import com.ak.keycepass.desktop.ui.theme.*
import com.ak.keycepass.desktop.ui.viewmodel.AdminViewModel
import com.ak.keycepass.desktop.ui.viewmodel.CreationSemaineState
import com.ak.keycepass.desktop.ui.viewmodel.ImportState
import org.jetbrains.skia.Image
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRManagementScreen() {
    val vm = remember { AdminViewModel() }
    val importState by vm.importState.collectAsState()
    val classes by vm.classes.collectAsState()
    val qrImage by vm.qrCodeImage.collectAsState()
    val semaines by vm.semaines.collectAsState()
    val creationState by vm.creationSemaineState.collectAsState()
    val enseignants by vm.enseignants.collectAsState()
    val seances by vm.seancesSemaine.collectAsState()

    var selectedClasse by remember { mutableStateOf("") }
    var semaineIso by remember { mutableStateOf("") }
    var latValue by remember { mutableStateOf("5.3229") }
    var lonValue by remember { mutableStateOf("-4.0189") }
    var showCreationForm by remember { mutableStateOf(false) }

    // Dialogues additionnels
    var showSeancesDialog by remember { mutableStateOf(false) }
    var selectedSemainePourSeances by remember { mutableStateOf<SeanceSemaineRow?>(null) }

    // Charger les classes et enseignants au demarrage
    LaunchedEffect(Unit) {
        vm.chargerToutesLesClasses()
        vm.chargerEnseignants()
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Gestion des QR Codes & Emargement",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {

            // ── Panneau gauche : import + creation + enseignants (déroulable) ──
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // 1. Importer Excel
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Import des etudiants", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val chooser = JFileChooser().apply {
                                    dialogTitle = "Selectionner le fichier Excel"
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

                        when (val s = importState) {
                            is ImportState.Chargement -> {
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(Modifier.fillMaxWidth())
                                Text("Import en cours...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            is ImportState.Succes -> {
                                Spacer(Modifier.height(8.dp))
                                Text("${s.importees}/${s.total} etudiants importes",
                                    fontSize = 12.sp, color = StatusPresent, fontWeight = FontWeight.Medium)
                            }
                            is ImportState.SuccesAvecAvertissements -> {
                                Spacer(Modifier.height(8.dp))
                                Text("${s.importees}/${s.total} importes (${s.avertissements.size} avertissements)",
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

                // 2. Gestion des enseignants
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Gestion des enseignants", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))

                        var profMatricule by remember { mutableStateOf("") }
                        var profNom by remember { mutableStateOf("") }
                        var profPrenom by remember { mutableStateOf("") }

                        OutlinedTextField(
                            value = profMatricule,
                            onValueChange = { profMatricule = it },
                            label = { Text("Matricule Enseignant", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                        )
                        Spacer(Modifier.height(6.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = profNom,
                                onValueChange = { profNom = it },
                                label = { Text("Nom", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                            )
                            OutlinedTextField(
                                value = profPrenom,
                                onValueChange = { profPrenom = it },
                                label = { Text("Prénom", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                            )
                        }
                        Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (profMatricule.isNotEmpty() && profNom.isNotEmpty() && profPrenom.isNotEmpty()) {
                                    vm.creerEnseignant(profMatricule, profNom, profPrenom)
                                    profMatricule = ""
                                    profNom = ""
                                    profPrenom = ""
                                }
                            },
                            enabled = profMatricule.isNotEmpty() && profNom.isNotEmpty() && profPrenom.isNotEmpty(),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Ajouter l'enseignant", fontSize = 12.sp)
                        }

                        if (enseignants.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Text("Enseignants enregistrés (${enseignants.size}) :", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                             enseignants.take(5).forEach { p ->
                                Text("• ${p.prenom} ${p.nom} (Matricule: ${p.matriculeEnseignant})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // 3. Creer une semaine
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Semaines d'enseignement", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            FilledTonalButton(
                                onClick = { showCreationForm = !showCreationForm },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Nouvelle", fontSize = 11.sp)
                            }
                        }

                        if (showCreationForm) {
                            Spacer(Modifier.height(10.dp))

                            if (classes.isEmpty() || (classes.size == 1 && classes[0] == "Toutes")) {
                                Text("Importer d'abord un fichier Excel pour voir les classes disponibles.",
                                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                @Suppress("UNCHECKED_CAST")

                                // Selection classe
                                Text("Classe", fontSize = 11.sp, fontWeight = FontWeight.Medium)
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
                                Spacer(Modifier.height(8.dp))

                                // Semaine ISO
                                OutlinedTextField(
                                    value = semaineIso,
                                    onValueChange = { semaineIso = it },
                                    label = { Text("Semaine (ex: 2026-W25)", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                                )
                                Spacer(Modifier.height(8.dp))

                                // GPS
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = latValue,
                                        onValueChange = { latValue = it },
                                        label = { Text("Latitude", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                                    )
                                    OutlinedTextField(
                                        value = lonValue,
                                        onValueChange = { lonValue = it },
                                        label = { Text("Longitude", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                                    )
                                }
                                Spacer(Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        vm.creerSemaine(selectedClasse, semaineIso, latValue.toDoubleOrNull() ?: 0.0, lonValue.toDoubleOrNull() ?: 0.0)
                                    },
                                    enabled = selectedClasse.isNotEmpty() && semaineIso.isNotEmpty(),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Creer la semaine", fontSize = 12.sp)
                                }

                                when (val cs = creationState) {
                                    is CreationSemaineState.Succes -> {
                                        Spacer(Modifier.height(6.dp))
                                        Text("Semaine creee ! ID: ${cs.semaineId}",
                                            fontSize = 12.sp, color = StatusPresent, fontWeight = FontWeight.Medium)
                                    }
                                    is CreationSemaineState.Erreur -> {
                                        Spacer(Modifier.height(6.dp))
                                        Text(cs.message, fontSize = 12.sp, color = StatusAbsent)
                                    }
                                    else -> {}
                                }
                            }
                        }

                        // Liste des semaines existantes (avec sélecteur de classe si classe vide)
                        LaunchedEffect(selectedClasse) {
                            if (selectedClasse.isNotEmpty()) {
                                vm.chargerSemaines(selectedClasse)
                            }
                        }

                        if (semaines.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Text("Semaines existantes (${selectedClasse}) :", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            semaines.forEach { s ->
                                Spacer(Modifier.height(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ) {
                                    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text("${s.classeId} - ${s.semaineIso}", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                            Text("${s.latReference}, ${s.lonReference}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(
                                                onClick = {
                                                    selectedSemainePourSeances = s
                                                    vm.chargerSeancesDeLaSemaine(s.idSemaine)
                                                    showSeancesDialog = true
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.List, contentDescription = "Gérer les séances", modifier = Modifier.size(14.dp))
                                            }
                                            IconButton(
                                                onClick = { vm.genererQrPresenceSemaine(s.idSemaine) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.QrCode, contentDescription = "QR Code", modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Panneau droit : QR code affiche ──
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Code QR", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

                    Spacer(Modifier.height(16.dp))

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
                                    contentDescription = "QR Code",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("Scannez ce QR avec l'app mobile",
                                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            FilledTonalButton(
                                onClick = {
                                    val chooser = JFileChooser().apply {
                                        dialogTitle = "Enregistrer le QR code"
                                        selectedFile = File("qr_code.png")
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
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.QrCode, contentDescription = null,
                                    modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                                Spacer(Modifier.height(8.dp))
                                Text("Sélectionnez une semaine pour générer le QR",
                                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
        }
    }

    // ===== DIALOGUE SÉANCES DE LA SEMAINE =====
    if (showSeancesDialog && selectedSemainePourSeances != null) {
        val sem = selectedSemainePourSeances!!

        var matNom by remember { mutableStateOf("") }
        var dateJ by remember { mutableStateOf(java.time.LocalDate.now().toString()) }
        var heureD by remember { mutableStateOf("08:00:00") }
        var heureF by remember { mutableStateOf("10:00:00") }
        var selectedProfId by remember { mutableStateOf<Int?>(null) }
        var profExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showSeancesDialog = false },
            title = {
                Text("Séances : ${sem.classeId} (${sem.semaineIso})", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    
                    // Liste
                    Text("Séances planifiées :", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    if (seances.isEmpty()) {
                        Text("Aucune séance planifiée.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        seances.forEach { s ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            ) {
                                Column(Modifier.padding(8.dp)) {
                                    Text(s.nomMatiere, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Le ${s.dateJour} de ${s.heureDebut} à ${s.heureFin}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    s.enseignantNomComplet?.let {
                                        Text("Enseignant: $it", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(6.dp))

                    Text("Planifier une nouvelle séance :", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                    OutlinedTextField(
                        value = matNom,
                        onValueChange = { matNom = it },
                        label = { Text("Nom du cours / Matière", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                    )

                    OutlinedTextField(
                        value = dateJ,
                        onValueChange = { dateJ = it },
                        label = { Text("Date (YYYY-MM-DD)", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = heureD,
                            onValueChange = { heureD = it },
                            label = { Text("Début (HH:MM:SS)", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                        )
                        OutlinedTextField(
                            value = heureF,
                            onValueChange = { heureF = it },
                            label = { Text("Fin (HH:MM:SS)", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                        )
                    }

                    // Prof
                    Text("Enseignant", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Box {
                        val selectedProfName = enseignants.find { it.idEnseignant == selectedProfId }?.let { "${it.prenom} ${it.nom}" } ?: "Choisir un enseignant..."
                        OutlinedButton(
                            onClick = { profExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(selectedProfName, fontSize = 12.sp)
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = profExpanded, onDismissRequest = { profExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Aucun", fontSize = 12.sp) },
                                onClick = { selectedProfId = null; profExpanded = false }
                            )
                            enseignants.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text("${p.prenom} ${p.nom}", fontSize = 12.sp) },
                                    onClick = { selectedProfId = p.idEnseignant; profExpanded = false }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (matNom.isNotEmpty() && dateJ.isNotEmpty() && heureD.isNotEmpty() && heureF.isNotEmpty()) {
                            vm.ajouterSeanceALaSemaine(
                                semaineId = sem.idSemaine,
                                nomMatiere = matNom,
                                classeId = sem.classeId,
                                dateJour = dateJ,
                                heureDebut = heureD,
                                heureFin = heureF,
                                enseignantId = selectedProfId
                            )
                            matNom = ""
                        }
                    },
                    enabled = matNom.isNotEmpty() && dateJ.isNotEmpty() && heureD.isNotEmpty() && heureF.isNotEmpty()
                ) {
                    Text("Créer la séance", fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSeancesDialog = false }) {
                    Text("Fermer", fontSize = 12.sp)
                }
            }
        )
    }
}
