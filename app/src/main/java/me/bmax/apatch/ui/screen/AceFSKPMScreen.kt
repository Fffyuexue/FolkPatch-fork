package me.bmax.apatch.ui.screen

import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import me.bmax.apatch.Natives
import me.bmax.apatch.R
import me.bmax.apatch.ui.viewmodel.KPModel
import me.bmax.apatch.ui.viewmodel.PatchesViewModel
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.util.AceFSConfig
import me.bmax.apatch.util.Version
import java.io.File

@Composable
private fun AceFSKPMLabel(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Destination<RootGraph>
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AceFSKPMScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isKpmLoaded by remember { mutableStateOf(false) }
    var isAceFSRunning by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    // KPM Info
    var kpmVersion by remember { mutableStateOf("") }
    var kpmDescription by remember { mutableStateOf("") }
    
    // Service Status
    var isHideServiceEnabled by remember { mutableStateOf(false) }
    
    // Kernel Info
    val kernelVersion = remember { System.getProperty("os.version") ?: "Unknown" }
    
    // Load all information on screen start
    LaunchedEffect(Unit) {
        isKpmLoaded = AceFSConfig.isAceFSKpmLoaded()
        isAceFSRunning = AceFSConfig.isAceFSRunning()
        
        // Get KPM info if loaded
        if (isKpmLoaded) {
            val kpmInfo = AceFSConfig.getAceFSKPMInfo()
            kpmInfo?.let {
                kpmVersion = it.version
                kpmDescription = it.description
            }
        }
        
        // Get Hide service status
        isHideServiceEnabled = AceFSConfig.getHideServiceStatus()
    }

    val isWallpaperMode = BackgroundConfig.isCustomBackgroundEnabled && (BackgroundConfig.customBackgroundUri != null || BackgroundConfig.isMultiBackgroundEnabled)
    val isDark = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R && !isSystemInDarkTheme()
    val opacity = if (isWallpaperMode) {
        BackgroundConfig.customBackgroundOpacity.coerceAtLeast(0.2f)
    } else {
        1f
    }
    
    val cardColor = if (isWallpaperMode) {
        MaterialTheme.colorScheme.surface.copy(alpha = opacity)
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.acefs_kpm_title)) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Running Status Card (Migrated KPM UI Style)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = cardColor,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Labels row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        val labelOpacity = (opacity + 0.1f).coerceAtMost(1f)
                        
                        AceFSKPMLabel(
                            text = "AceFS",
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = labelOpacity),
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        if (isAceFSRunning) {
                            AceFSKPMLabel(
                                text = stringResource(R.string.acefs_kpm_service_status),
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = labelOpacity),
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    // Title and status icon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.acefs_kpm_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Status indicator
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = if (isKpmLoaded) Icons.Filled.CheckCircle else Icons.Filled.Circle,
                            contentDescription = null,
                            tint = if (isKpmLoaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }

                    // Version Info
                    Text(
                        text = if (kpmVersion.isNotEmpty()) "${stringResource(R.string.acefs_kpm_version_prefix)}$kpmVersion" else if (isKpmLoaded) stringResource(R.string.acefs_kpm_status_running) else stringResource(R.string.acefs_kpm_status_not_loaded),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons
                    if (isLoading) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        isLoading = true
                                        if (isKpmLoaded) {
                                            withContext(Dispatchers.IO) {
                                                Natives.unloadKernelPatchModule("AceFS")
                                            }
                                        } else {
                                            withContext(Dispatchers.IO) {
                                                try {
                                                    val assetManager = context.assets
                                                    val cacheFile = File(context.cacheDir, "AceFS.kpm")
                                                    
                                                    try {
                                                        assetManager.open("AceFS/AceFS.kpm").use { input ->
                                                            cacheFile.outputStream().use { output ->
                                                                input.copyTo(output)
                                                            }
                                                        }
                                                        Natives.loadKernelPatchModule(cacheFile.absolutePath, "")
                                                    } catch (e: Exception) {
                                                        Log.e("AceFSKPM", "Failed to load kpm", e)
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e("AceFSKPM", "Failed to setup kpm", e)
                                                }
                                            }
                                        }
                                        // Refresh status
                                        isKpmLoaded = AceFSConfig.isAceFSKpmLoaded()
                                        isAceFSRunning = AceFSConfig.isAceFSRunning()
                                        
                                        // Refresh KPM info
                                        if (isKpmLoaded) {
                                            val kpmInfo = AceFSConfig.getAceFSKPMInfo()
                                            kpmInfo?.let {
                                                kpmVersion = it.version
                                                kpmDescription = it.description
                                            }
                                        } else {
                                            kpmVersion = ""
                                            kpmDescription = ""
                                        }
                                        
                                        isLoading = false
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isKpmLoaded) 
                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = (opacity + 0.3f).coerceAtMost(1f))
                                    else 
                                        MaterialTheme.colorScheme.primary.copy(alpha = (opacity + 0.3f).coerceAtMost(1f)),
                                    contentColor = if (isKpmLoaded) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(
                                    modifier = Modifier.size(18.dp),
                                    imageVector = if (isKpmLoaded) Icons.Filled.VisibilityOff else Icons.Filled.AutoFixHigh,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isKpmLoaded) stringResource(R.string.acefs_kpm_unload) else stringResource(R.string.acefs_kpm_load))
                            }
                        }
                    }
                }
            }

            // Info Card
            InfoCard(
                title = stringResource(R.string.acefs_kpm_info_title),
                icon = Icons.Outlined.Info,
                isWallpaperMode = isWallpaperMode
            ) {
                InfoRow(
                    label = stringResource(R.string.acefs_kpm_kernel_version),
                    value = kernelVersion
                )
                InfoRow(
                    label = stringResource(R.string.acefs_kpm_s_status),
                    value = if (isKpmLoaded) stringResource(R.string.acefs_kpm_status_running) else stringResource(R.string.acefs_kpm_status_not_running)
                )
                InfoRow(
                    label = stringResource(R.string.acefs_kpm_c_status),
                    value = if (isAceFSRunning) stringResource(R.string.acefs_kpm_status_running) else stringResource(R.string.acefs_kpm_status_not_running)
                )
                InfoRow(
                    label = stringResource(R.string.acefs_kpm_hide_status),
                    value = if (isHideServiceEnabled) stringResource(R.string.acefs_kpm_hide_enabled) else stringResource(R.string.acefs_kpm_hide_disabled)
                )
                if (kpmVersion.isNotEmpty()) {
                    InfoRow(    
                        label = stringResource(R.string.acefs_kpm_s_version),
                        value = kpmVersion
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun InfoCard(
    title: String,
    icon: ImageVector,
    isWallpaperMode: Boolean,
    content: @Composable () -> Unit
) {
    val opacity = if (isWallpaperMode) {
        BackgroundConfig.customBackgroundOpacity.coerceAtLeast(0.2f)
    } else {
        1f
    }
    
    val cardColor = if (isWallpaperMode) {
        MaterialTheme.colorScheme.surface.copy(alpha = opacity)
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = cardColor,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            
            // Content
            content()
        }
    }
}