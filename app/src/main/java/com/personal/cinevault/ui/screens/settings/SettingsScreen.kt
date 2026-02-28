package com.personal.cinevault.ui.screens.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
    importViewModel: ImportViewModel = koinViewModel()
) {
    val context = LocalContext.current

    val isDarkTheme     by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val fileUri         by viewModel.backupFileUri.collectAsStateWithLifecycle()
    val lastBackupDate  by viewModel.lastBackupDate.collectAsStateWithLifecycle()
    val backupStatus    by viewModel.backupStatus.collectAsStateWithLifecycle()
    val manualState     by viewModel.manualBackupState.collectAsStateWithLifecycle()
    val restoreState    by viewModel.restoreState.collectAsStateWithLifecycle()

    val isImporting by importViewModel.isImporting.collectAsStateWithLifecycle()
    val importProgress by importViewModel.importProgress.collectAsStateWithLifecycle()
    val importResult by importViewModel.importResult.collectAsStateWithLifecycle()
    val importError by importViewModel.importError.collectAsStateWithLifecycle()

    var showImportDialog by remember { mutableStateOf(false) }

    val snackbar = remember { SnackbarHostState() }

    // ── Snackbar feedback from backup / restore ────────────────────────────────
    LaunchedEffect(manualState) {
        when (val s = manualState) {
            is ManualBackupState.Success -> {
                snackbar.showSnackbar(s.message)
                viewModel.clearManualBackupState()
            }
            is ManualBackupState.Error -> {
                snackbar.showSnackbar("Backup failed: ${s.message}")
                viewModel.clearManualBackupState()
            }
            else -> Unit
        }
    }
    LaunchedEffect(restoreState) {
        when (val s = restoreState) {
            is RestoreState.Success -> {
                snackbar.showSnackbar("Restore complete — please restart the app")
                viewModel.clearRestoreState()
            }
            is RestoreState.Error -> {
                snackbar.showSnackbar("Restore failed: ${s.message}")
                viewModel.clearRestoreState()
            }
            else -> Unit
        }
    }

    // ── Activity result launchers ──────────────────────────────────────────────
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        uri?.let { viewModel.onBackupFileSelected(context, it) }
    }

    val restorePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.restoreFromBackup(context, it) }
    }

    val importPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        uri?.let { importViewModel.startImport(context, it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ── Appearance ────────────────────────────────────────────────────
            SettingsSection(title = "Appearance") {
                SettingsRow(
                    icon = {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.DarkMode
                                          else             Icons.Default.LightMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    label = "Dark theme",
                    subtitle = if (isDarkTheme) "Dark mode enabled" else "Light mode enabled"
                ) {
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = viewModel::onThemeToggle
                    )
                }
            }

            // ── Backup ────────────────────────────────────────────────────────
            SettingsSection(title = "Backup") {

                // Folder picker row
                SettingsRow(
                    icon = {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    label = "Backup file",
                    subtitle = fileUri
                        ?.let { Uri.parse(it).lastPathSegment ?: "File selected" }
                        ?: "Not set"
                ) {
                    OutlinedButton(
                        onClick = { filePicker.launch(viewModel.getBackupFileIntent()) },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Set File on Google Drive")
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 4.dp))

                // Last backup date
                SettingsRow(
                    icon = {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    label = "Last backup",
                    subtitle = lastBackupDate ?: "Never"
                ) {
                    Text(
                        text = backupStatus,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                HorizontalDivider(Modifier.padding(vertical = 4.dp))

                // Backup Now button
                Button(
                    onClick = { viewModel.runManualBackup(context) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = manualState !is ManualBackupState.Running && fileUri != null,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (manualState is ManualBackupState.Running) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Backing up…")
                    } else {
                        Icon(
                            Icons.Default.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Backup Now", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // ── Restore ───────────────────────────────────────────────────────
            SettingsSection(title = "Restore") {
                Text(
                    text = "Restoring overwrites all local data. The app will need to be restarted after restore.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = { restorePicker.launch(arrayOf("application/octet-stream", "*/*")) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = restoreState !is RestoreState.Running,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    if (restoreState is RestoreState.Running) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Restoring…")
                    } else {
                        Icon(
                            Icons.Default.Restore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Restore from Backup", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // ── Import ───────────────────────────────────────────────────────
            SettingsSection(title = "Import") {
                if (importResult != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Import Complete", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text("✓ ${importResult!!.logsImported} films imported")
                            Text("✓ ${importResult!!.reviewsImported} reviews imported")
                            Text("✓ ${importResult!!.watchlistImported} watchlist items")
                            Text("✓ ${importResult!!.listsImported} lists created")
                            if (importResult!!.failedLookups > 0) {
                                Spacer(Modifier.height(4.dp))
                                Text("⚠ ${importResult!!.failedLookups} movies not found on TMDB", color = MaterialTheme.colorScheme.error)
                            }
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { importViewModel.dismissResult() }) {
                                Text("Dismiss")
                            }
                        }
                    }
                } else if (importError != null) {
                    Text("Error: $importError", color = MaterialTheme.colorScheme.error)
                    Button(onClick = { importViewModel.dismissError() }) {
                        Text("Dismiss")
                    }
                } else if (isImporting) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        if (importProgress != null && importProgress!!.second > 0) {
                            Text("Importing ${importProgress!!.first} of ${importProgress!!.second} movies...")
                        } else {
                            Text("Starting import...")
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Import Letterboxd Data", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import from Letterboxd") },
            text = { 
                Text("This will add all your Letterboxd data to CineVault. Existing entries will not be overwritten. Continue?") 
            },
            confirmButton = {
                Button(onClick = {
                    showImportDialog = false
                    importPicker.launch(importViewModel.getImportIntent())
                }) {
                    Text("Import")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ── Reusable sub-composables ──────────────────────────────────────────────────

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = androidx.compose.ui.unit.TextUnit(1.2f, androidx.compose.ui.unit.TextUnitType.Sp)
            )
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    icon: @Composable () -> Unit,
    label: String,
    subtitle: String,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        icon()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        trailing()
    }
}
