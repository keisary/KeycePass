package com.ak.keycepass.desktop.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ak.keycepass.desktop.Screen
import com.ak.keycepass.desktop.ui.screens.HistoriqueScreen
import com.ak.keycepass.desktop.ui.theme.*

@Composable
fun AdminLayout(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit
) {
    var cmdPaletteOpen by remember { mutableStateOf(false) }
    var cmdSearch by remember { mutableStateOf("") }

    Box(
        Modifier.fillMaxSize()
            .onPreviewKeyEvent { event ->
                // Raccourcis clavier globaux Ctrl+1..4
                if (event.type == KeyEventType.KeyUp && (event.isCtrlPressed || event.isMetaPressed)) {
                    when (event.key) {
                        Key.One -> { onNavigate(Screen.DASHBOARD); true }
                        Key.Two -> { onNavigate(Screen.QR_MANAGEMENT); true }
                        Key.Three -> { onNavigate(Screen.ENROLEMENT); true }
                        Key.Four -> { onNavigate(Screen.HISTORIQUE); true }
                        Key.K -> { cmdPaletteOpen = true; true }
                        else -> false
                    }
                } else false
            }
    ) {
        Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // ===== SIDEBAR =====
            Surface(
                modifier = Modifier
                    .width(240.dp)
                    .fillMaxHeight(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                shape = RoundedCornerShape(topEnd = 0.dp, bottomEnd = 16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(12.dp)
                ) {
                    Spacer(Modifier.height(20.dp))
                    // Logo + Brand
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("KP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "KeycePass",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Administration",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        thickness = 1.dp
                    )
                    Spacer(Modifier.height(16.dp))

                    // Navigation items
                    val navItems = buildList {
                        add(NavItem(Screen.DASHBOARD, Icons.Default.Dashboard, "Dashboard"))
                        add(NavItem(Screen.QR_MANAGEMENT, Icons.Default.QrCodeScanner, "QR Codes"))
                        add(NavItem(Screen.ENROLEMENT, Icons.Default.PhoneAndroid, "Pairages"))
                        add(NavItem(Screen.HISTORIQUE, Icons.Default.BarChart, "Historique"))
                    }

                    navItems.forEach { item ->
                        val isSelected = currentScreen == item.screen
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onNavigate(item.screen) },
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    item.label,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    // ── Profil Enseignant ──
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar
                            Surface(
                                modifier = Modifier.size(42.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        "TG",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "The Guy",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(StatusPresent)
                            )
                        }
                    }

                    // Server Status Badge
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = StatusPresent.copy(alpha = 0.1f),
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(StatusPresent)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Serveur actif", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StatusPresent)
                                Text("Port 8080", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // ===== MAIN CONTENT =====
            Column(Modifier.fillMaxSize()) {
                // Top bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                when (currentScreen) {
                                    Screen.DASHBOARD -> "Dashboard"
                                    Screen.QR_MANAGEMENT -> "Gestion des QR Codes"
                                    Screen.ENROLEMENT -> "Pairage des appareils"
                                    Screen.HISTORIQUE -> "Historique et Statistiques"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Seance en cours",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        // Cmd+K button
                        FilledTonalButton(
                            onClick = { cmdPaletteOpen = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(36.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            )
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Rechercher...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(20.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ) {
                                Text("Ctrl+K", fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    thickness = 0.5.dp
                )

                // Page content with transitions + glassmorphism container
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                            (slideInHorizontally { width -> width * direction } + fadeIn(tween(300)))
                                .togetherWith(
                                    slideOutHorizontally { width -> -width * direction } + fadeOut(tween(200))
                                )
                        },
                        label = "screen-crossfade"
                    ) { screen ->
                        // On remplace "content()" par le rendu direct selon l'ecran
                        when (screen) {
                            Screen.DASHBOARD -> com.ak.keycepass.desktop.ui.screens.DashboardScreen()
                            Screen.QR_MANAGEMENT -> com.ak.keycepass.desktop.ui.screens.QRManagementScreen()
                            Screen.ENROLEMENT -> com.ak.keycepass.desktop.ui.screens.GestionEnrolementScreen()
                            Screen.HISTORIQUE -> com.ak.keycepass.desktop.ui.screens.HistoriqueScreen()
                        }
                    }
                }
            }
        }

        // ===== COMMAND PALETTE OVERLAY =====
        if (cmdPaletteOpen) {
            CommandPalette(
                search = cmdSearch,
                onSearchChange = { cmdSearch = it },
                onSelect = { screen ->
                    onNavigate(screen)
                    cmdPaletteOpen = false
                    cmdSearch = ""
                },
                onDismiss = {
                    cmdPaletteOpen = false
                    cmdSearch = ""
                }
            )
        }
    }
}

// ── Navigation Item data class ──

private data class NavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)

// ── Commande Palette ──

@Composable
private fun CommandPalette(
    search: String,
    onSearchChange: (String) -> Unit,
    onSelect: (Screen) -> Unit,
    onDismiss: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val items = Screen.entries

    val filtered = remember(search) {
        if (search.isBlank()) items
        else items.filter { it.label.contains(search, ignoreCase = true) }
    }

    var selectedIndex by remember { mutableStateOf(0) }

    // Gestion du clavier
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(onClick = onDismiss)
            .onPreviewKeyEvent { event ->
                when {
                    event.key == Key.Escape && event.type == KeyEventType.KeyUp -> {
                        onDismiss(); true
                    }
                    event.key == Key.Enter && event.type == KeyEventType.KeyUp && filtered.isNotEmpty() -> {
                        onSelect(filtered[selectedIndex]); true
                    }
                    event.key == Key.DirectionDown && event.type == KeyEventType.KeyUp -> {
                        selectedIndex = (selectedIndex + 1).coerceAtMost(filtered.lastIndex); true
                    }
                    event.key == Key.DirectionUp && event.type == KeyEventType.KeyUp -> {
                        selectedIndex = (selectedIndex - 1).coerceAtLeast(0); true
                    }
                    else -> false
                }
            }
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
                .widthIn(max = 480.dp)
                .fillMaxWidth(0.5f),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = search,
                    onValueChange = {
                        onSearchChange(it)
                        selectedIndex = 0
                    },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    placeholder = { Text("Rechercher un ecran...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (filtered.isNotEmpty()) onSelect(filtered[selectedIndex])
                        }
                    )
                )

                Spacer(Modifier.height(8.dp))

                if (filtered.isEmpty()) {
                    Text(
                        "Aucun resultat",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                } else {
                    filtered.forEach { screen ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelect(screen) },
                            color = if (screen == filtered[selectedIndex])
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (screen) {
                                        Screen.DASHBOARD -> Icons.Default.Dashboard
                                        Screen.QR_MANAGEMENT -> Icons.Default.QrCodeScanner
                                        Screen.ENROLEMENT -> Icons.Default.PhoneAndroid
                                        Screen.HISTORIQUE -> Icons.Default.BarChart
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(screen.label, fontSize = 14.sp)
                                Spacer(Modifier.weight(1f))
                                Text(
                                    when (screen) {
                                        Screen.DASHBOARD -> "Ctrl+1"
                                        Screen.QR_MANAGEMENT -> "Ctrl+2"
                                        Screen.ENROLEMENT -> "Ctrl+3"
                                        Screen.HISTORIQUE -> "Ctrl+4"
                                    },
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
