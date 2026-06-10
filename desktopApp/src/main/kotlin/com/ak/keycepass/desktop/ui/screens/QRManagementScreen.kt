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
import org.jetbrains.skia.Image
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRManagementScreen() {
    val vm = remember { AdminViewModel() }
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

    // Dialogues
    var showSeancesDialog by remember { mutableStateOf(false) }
    var selectedSemainePourSeances by remember { mutableStateOf<SeanceSemaineRow?>(null) }
    var selectedJourPourConfig by remember { mutableStateOf<LocalDate?>(null) }
    var showCalendarDialog by remember { mutableStateOf(false) }

    // Charger les classes et enseignants au démarrage
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

            // ── Panneau gauche : création + enseignants (déroulable) ──
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // 1. Gestion des enseignants
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

                // 2. Creer une semaine
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

                        Spacer(Modifier.height(8.dp))

                        // Selection classe pour visualiser et gérer
                        Text("Classe à gérer", fontSize = 11.sp, fontWeight = FontWeight.Medium)
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

                        if (showCreationForm) {
                            Spacer(Modifier.height(10.dp))

                            if (selectedClasse.isEmpty()) {
                                Text("Sélectionnez d'abord une classe ci-dessus pour créer une semaine.",
                                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                @Suppress("UNCHECKED_CAST")

                                // Semaine ISO
                                OutlinedTextField(
                                    value = semaineIso,
                                    onValueChange = { semaineIso = it },
                                    label = { Text("Semaine (ex: 2026-W25)", fontSize = 11.sp) },
                                    singleLine = true,
                                    trailingIcon = {
                                        IconButton(onClick = { showCalendarDialog = true }) {
                                            Icon(Icons.Default.CalendarToday, contentDescription = "Choisir la date", modifier = Modifier.size(18.dp))
                                        }
                                    },
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
                                                    selectedJourPourConfig = null
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
        val monday = remember(sem.semaineIso) { getMondayOfIsoWeek(sem.semaineIso) ?: LocalDate.now() }
        val days = remember(monday) { (0..4).map { monday.plusDays(it.toLong()) } }

        // Form states (internal to the day editor)
        var s1IsTpe by remember { mutableStateOf(false) }
        var s1MatiereInput by remember { mutableStateOf("") }
        var s1SelectedProfId by remember { mutableStateOf<Int?>(null) }
        var s1HeureDebut by remember { mutableStateOf("08:00:00") }
        var s1HeureFin by remember { mutableStateOf("12:00:00") }

        var s2IsTpe by remember { mutableStateOf(false) }
        var s2MatiereInput by remember { mutableStateOf("") }
        var s2SelectedProfId by remember { mutableStateOf<Int?>(null) }
        var s2HeureDebut by remember { mutableStateOf("13:00:00") }
        var s2HeureFin by remember { mutableStateOf("17:00:00") }

        val dayToEdit = selectedJourPourConfig
        
        // Initialiser la configuration lors du choix de la journée
        LaunchedEffect(dayToEdit, seances) {
            if (dayToEdit != null) {
                val daySeances = seances.filter { it.dateJour == dayToEdit.toString() }.sortedBy { it.heureDebut }
                val s1 = daySeances.getOrNull(0)
                val s2 = daySeances.getOrNull(1)

                s1IsTpe = s1?.nomMatiere == "TPE"
                s1MatiereInput = if (s1?.nomMatiere == "TPE") "" else (s1?.nomMatiere ?: "")
                s1SelectedProfId = s1?.enseignantId
                s1HeureDebut = s1?.heureDebut ?: "08:00:00"
                s1HeureFin = s1?.heureFin ?: "12:00:00"

                s2IsTpe = s2?.nomMatiere == "TPE"
                s2MatiereInput = if (s2?.nomMatiere == "TPE") "" else (s2?.nomMatiere ?: "")
                s2SelectedProfId = s2?.enseignantId
                s2HeureDebut = s2?.heureDebut ?: "13:00:00"
                s2HeureFin = s2?.heureFin ?: "17:00:00"
            }
        }

        AlertDialog(
            onDismissRequest = { showSeancesDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (dayToEdit != null) {
                        IconButton(onClick = { selectedJourPourConfig = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                        }
                        Spacer(Modifier.width(8.dp))
                        val frenchFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)
                        val dayLabel = dayToEdit.format(frenchFormatter).replaceFirstChar { it.uppercase() }
                        Text(dayLabel, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text("Séances de la semaine : ${sem.classeId} (${sem.semaineIso})", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            text = {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (dayToEdit != null) {
                        // === MODE CONFIGURATION D'UNE JOURNÉE ===
                        val daySeances = seances.filter { it.dateJour == dayToEdit.toString() }.sortedBy { it.heureDebut }
                        val s1 = daySeances.getOrNull(0)
                        val s2 = daySeances.getOrNull(1)

                        // ── SÉANCE 1 ──
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Séance 1 (Matin)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(6.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = s1IsTpe, onCheckedChange = { s1IsTpe = it })
                                    Text("TPE (Pas de cours programmé)", fontSize = 12.sp)
                                }

                                if (!s1IsTpe) {
                                    OutlinedTextField(
                                        value = s1MatiereInput,
                                        onValueChange = { s1MatiereInput = it },
                                        label = { Text("Nom de la matière", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                                    )
                                    Spacer(Modifier.height(6.dp))

                                    // Enseignant Dropdown
                                    Text("Enseignant", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    var s1ProfExpanded by remember { mutableStateOf(false) }
                                    Box {
                                        val selectedProfName = enseignants.find { it.idEnseignant == s1SelectedProfId }?.let { "${it.prenom} ${it.nom}" } ?: "Choisir un enseignant..."
                                        OutlinedButton(
                                            onClick = { s1ProfExpanded = true },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(selectedProfName, fontSize = 12.sp)
                                            Spacer(Modifier.weight(1f))
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                        DropdownMenu(expanded = s1ProfExpanded, onDismissRequest = { s1ProfExpanded = false }) {
                                            DropdownMenuItem(
                                                text = { Text("Aucun", fontSize = 12.sp) },
                                                onClick = { s1SelectedProfId = null; s1ProfExpanded = false }
                                            )
                                            enseignants.forEach { p ->
                                                DropdownMenuItem(
                                                    text = { Text("${p.prenom} ${p.nom}", fontSize = 12.sp) },
                                                    onClick = { s1SelectedProfId = p.idEnseignant; s1ProfExpanded = false }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = s1HeureDebut,
                                        onValueChange = { s1HeureDebut = it },
                                        label = { Text("Début (HH:MM:SS)", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                                    )
                                    OutlinedTextField(
                                        value = s1HeureFin,
                                        onValueChange = { s1HeureFin = it },
                                        label = { Text("Fin (HH:MM:SS)", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                                    )
                                }
                            }
                        }

                        // ── SÉANCE 2 ──
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Séance 2 (Après-midi)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(6.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = s2IsTpe, onCheckedChange = { s2IsTpe = it })
                                    Text("TPE (Pas de cours programmé)", fontSize = 12.sp)
                                }

                                if (!s2IsTpe) {
                                    OutlinedTextField(
                                        value = s2MatiereInput,
                                        onValueChange = { s2MatiereInput = it },
                                        label = { Text("Nom de la matière", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                                    )
                                    Spacer(Modifier.height(6.dp))

                                    // Enseignant Dropdown
                                    Text("Enseignant", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    var s2ProfExpanded by remember { mutableStateOf(false) }
                                    Box {
                                        val selectedProfName = enseignants.find { it.idEnseignant == s2SelectedProfId }?.let { "${it.prenom} ${it.nom}" } ?: "Choisir un enseignant..."
                                        OutlinedButton(
                                            onClick = { s2ProfExpanded = true },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(selectedProfName, fontSize = 12.sp)
                                            Spacer(Modifier.weight(1f))
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                        DropdownMenu(expanded = s2ProfExpanded, onDismissRequest = { s2ProfExpanded = false }) {
                                            DropdownMenuItem(
                                                text = { Text("Aucun", fontSize = 12.sp) },
                                                onClick = { s2SelectedProfId = null; s2ProfExpanded = false }
                                            )
                                            enseignants.forEach { p ->
                                                DropdownMenuItem(
                                                    text = { Text("${p.prenom} ${p.nom}", fontSize = 12.sp) },
                                                    onClick = { s2SelectedProfId = p.idEnseignant; s2ProfExpanded = false }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = s2HeureDebut,
                                        onValueChange = { s2HeureDebut = it },
                                        label = { Text("Début (HH:MM:SS)", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                                    )
                                    OutlinedTextField(
                                        value = s2HeureFin,
                                        onValueChange = { s2HeureFin = it },
                                        label = { Text("Fin (HH:MM:SS)", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                                    )
                                }
                            }
                        }

                    } else {
                        // === MODE LISTE DES JOURS DE LA SEMAINE ===
                        days.forEach { day ->
                            val daySeances = seances.filter { it.dateJour == day.toString() }.sortedBy { it.heureDebut }
                            val s1 = daySeances.getOrNull(0)
                            val s2 = daySeances.getOrNull(1)

                            val frenchFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)
                            val dayLabel = day.format(frenchFormatter).replaceFirstChar { it.uppercase() }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(dayLabel, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(Modifier.height(4.dp))
                                        
                                        // Séance 1 summary
                                        Text(
                                            text = if (s1 != null) {
                                                if (s1.nomMatiere == "TPE") "• Matin: TPE (Pas de cours)"
                                                else "• Matin: ${s1.nomMatiere} (${s1.enseignantNomComplet ?: "Pas de prof"})"
                                            } else "• Matin: Non configuré",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        // Séance 2 summary
                                        Text(
                                            text = if (s2 != null) {
                                                if (s2.nomMatiere == "TPE") "• Après-midi: TPE (Pas de cours)"
                                                else "• Après-midi: ${s2.nomMatiere} (${s2.enseignantNomComplet ?: "Pas de prof"})"
                                            } else "• Après-midi: Non configuré",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(onClick = { selectedJourPourConfig = day }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Configurer le jour")
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (dayToEdit != null) {
                    val daySeances = seances.filter { it.dateJour == dayToEdit.toString() }.sortedBy { it.heureDebut }
                    val s1 = daySeances.getOrNull(0)
                    val s2 = daySeances.getOrNull(1)

                    Button(
                        onClick = {
                            val s1Matiere = if (s1IsTpe) "TPE" else s1MatiereInput
                            val s1ProfId = if (s1IsTpe) null else s1SelectedProfId

                            val s2Matiere = if (s2IsTpe) "TPE" else s2MatiereInput
                            val s2ProfId = if (s2IsTpe) null else s2SelectedProfId

                            // Save Séance 1
                            if (s1IsTpe || s1Matiere.isNotEmpty()) {
                                vm.enregistrerSeance(
                                    idSeance = s1?.idSeance,
                                    semaineId = sem.idSemaine,
                                    nomMatiere = s1Matiere,
                                    classeId = sem.classeId,
                                    dateJour = dayToEdit.toString(),
                                    heureDebut = s1HeureDebut,
                                    heureFin = s1HeureFin,
                                    enseignantId = s1ProfId
                                )
                            }

                            // Save Séance 2
                            if (s2IsTpe || s2Matiere.isNotEmpty()) {
                                vm.enregistrerSeance(
                                    idSeance = s2?.idSeance,
                                    semaineId = sem.idSemaine,
                                    nomMatiere = s2Matiere,
                                    classeId = sem.classeId,
                                    dateJour = dayToEdit.toString(),
                                    heureDebut = s2HeureDebut,
                                    heureFin = s2HeureFin,
                                    enseignantId = s2ProfId
                                )
                            }

                            selectedJourPourConfig = null
                        },
                        enabled = (s1IsTpe || s1MatiereInput.isNotEmpty()) && (s2IsTpe || s2MatiereInput.isNotEmpty())
                    ) {
                        Text("Enregistrer", fontSize = 12.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (dayToEdit != null) {
                            selectedJourPourConfig = null
                        } else {
                            showSeancesDialog = false
                        }
                    }
                ) {
                    Text(if (dayToEdit != null) "Annuler" else "Fermer", fontSize = 12.sp)
                }
            }
        )
    }

    // ===== DIALOGUE CALENDRIER DE SELECTION DE DATE =====
    if (showCalendarDialog) {
        CalendarDialog(
            onDismissRequest = { showCalendarDialog = false },
            onDateSelected = { date ->
                val weekFields = WeekFields.ISO
                val weekNumber = date.get(weekFields.weekOfWeekBasedYear())
                val weekBasedYear = date.get(weekFields.weekBasedYear())
                semaineIso = "$weekBasedYear-W${String.format("%02d", weekNumber)}"
                showCalendarDialog = false
            }
        )
    }
}

// Composable de calendrier graphique
@Composable
fun CalendarDialog(
    initialDate: LocalDate = LocalDate.now(),
    onDismissRequest: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    var currentMonth by remember { mutableStateOf(initialDate.withDayOfMonth(1)) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Mois précédent")
                }
                val monthName = currentMonth.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.FRENCH)
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.FRENCH) else it.toString() }
                Text(
                    text = "$monthName ${currentMonth.year}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Mois suivant")
                }
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // En-têtes des jours
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    val daysLetters = listOf("Lu", "Ma", "Me", "Je", "Ve", "Sa", "Di")
                    daysLetters.forEach { letter ->
                        Text(
                            text = letter,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(32.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                val firstDayOfWeek = currentMonth.dayOfWeek.value // 1 = Lundi, 7 = Dimanche
                val daysInMonth = currentMonth.lengthOfMonth()
                val totalSlots = 42

                val daysList = mutableListOf<LocalDate?>()
                for (i in 1 until firstDayOfWeek) {
                    daysList.add(null)
                }
                for (i in 1..daysInMonth) {
                    daysList.add(currentMonth.withDayOfMonth(i))
                }
                while (daysList.size < totalSlots) {
                    daysList.add(null)
                }

                val weeks = daysList.chunked(7)

                weeks.forEach { weekDays ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        weekDays.forEach { date ->
                            Box(
                                modifier = Modifier.size(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (date != null) {
                                    val isToday = date == LocalDate.now()
                                    OutlinedButton(
                                        onClick = { onDateSelected(date) },
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.fillMaxSize(),
                                        border = if (isToday) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = date.dayOfMonth.toString(),
                                            fontSize = 11.sp,
                                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Fermer")
            }
        }
    )
}

// Fonction utilitaire pour calculer le lundi d'une semaine ISO
fun getMondayOfIsoWeek(semaineIso: String): LocalDate? {
    return try {
        val parts = semaineIso.split("-W")
        if (parts.size != 2) return null
        val year = parts[0].toIntOrNull() ?: return null
        val week = parts[1].toIntOrNull() ?: return null
        val weekFields = WeekFields.ISO
        val date = LocalDate.of(year, 1, 4)
        val currentWeek = date.get(weekFields.weekOfWeekBasedYear())
        date.plusWeeks((week - currentWeek).toLong()).with(weekFields.dayOfWeek(), 1)
    } catch (e: Exception) {
        null
    }
}
