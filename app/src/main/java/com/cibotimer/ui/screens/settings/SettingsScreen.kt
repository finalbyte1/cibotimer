package com.cibotimer.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cibotimer.BuildConfig
import com.cibotimer.R
import com.cibotimer.data.models.AiConfig
import com.cibotimer.data.models.UserProfile
import com.cibotimer.data.repository.AppRepository
import com.cibotimer.notifications.NotificationHelper
import com.cibotimer.ui.components.*
import com.cibotimer.ui.theme.*
import com.cibotimer.update.UpdateManager
import com.cibotimer.update.UpdateState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: AppRepository,
    updateManager: UpdateManager? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val userProfile by repository.userProfile.collectAsState()
    val aiConfig by repository.aiConfig.collectAsState()

    val updateState = updateManager?.updateState?.collectAsState()?.value ?: UpdateState.Idle
    var updateServerUrlText by remember { mutableStateOf(updateManager?.serverUrl ?: "https://cibotimer.duckdns.org") }
    var autoCheckUpdates by remember { mutableStateOf(updateManager?.autoCheckEnabled ?: true) }

    // Local mutable state for forms
    var name by remember(userProfile.name) { mutableStateOf(userProfile.name) }
    var heightCm by remember(userProfile.heightCm) { mutableStateOf(userProfile.heightCm.toInt().toString()) }
    var targetWeightKg by remember(userProfile.targetWeightKg) { mutableStateOf(userProfile.targetWeightKg.toString()) }
    var waterTargetMl by remember(userProfile.waterTargetMl) { mutableStateOf(userProfile.waterTargetMl.toString()) }

    var isAiEnabled by remember(aiConfig.isEnabled) { mutableStateOf(aiConfig.isEnabled) }
    var aiBaseUrl by remember(aiConfig.baseUrl) { mutableStateOf(aiConfig.baseUrl) }
    var aiApiKey by remember(aiConfig.apiKey) { mutableStateOf(aiConfig.apiKey) }
    var aiModelName by remember(aiConfig.modelName) { mutableStateOf(aiConfig.modelName) }
    var showApiKey by remember { mutableStateOf(false) }

    var driveStatusMessage by remember { mutableStateOf<String?>(null) }
    var showJsonImportDialog by remember { mutableStateOf(false) }
    var jsonImportText by remember { mutableStateOf("") }
    var importStatusMessage by remember { mutableStateOf<String?>(null) }

    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var resetStatusMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            try {
                                val intent = Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = stringResource(R.string.settings_content_desc_notifications),
                            tint = FastingBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Info Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher),
                    contentDescription = stringResource(R.string.settings_content_desc_app_icon),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "CiboTimer",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = stringResource(R.string.settings_version_format, BuildConfig.VERSION_NAME),
                    fontSize = 14.sp,
                    color = TextSecondaryLight
                )
            }

            // Section 1: User Profile
            SettingsSectionCard(title = stringResource(R.string.settings_section_profile), icon = Icons.Default.Person) {
                AppOutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        repository.updateUserProfile(userProfile.copy(name = it))
                    },
                    label = { Text(stringResource(R.string.settings_profile_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppOutlinedTextField(
                        value = heightCm,
                        onValueChange = {
                            heightCm = it
                            val h = it.toDoubleOrNull()
                            if (h != null && h > 0) repository.updateUserProfile(userProfile.copy(heightCm = h))
                        },
                        label = { Text(stringResource(R.string.settings_profile_height)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    AppOutlinedTextField(
                        value = targetWeightKg,
                        onValueChange = {
                            targetWeightKg = it
                            val w = it.toDoubleOrNull()
                            if (w != null && w > 0) repository.updateUserProfile(userProfile.copy(targetWeightKg = w))
                        },
                        label = { Text(stringResource(R.string.settings_profile_target_weight)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                AppOutlinedTextField(
                    value = waterTargetMl,
                    onValueChange = {
                        waterTargetMl = it
                        val ml = it.toIntOrNull()
                        if (ml != null && ml > 0) repository.updateUserProfile(userProfile.copy(waterTargetMl = ml))
                    },
                    label = { Text(stringResource(R.string.settings_profile_water_target)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Section 2: Coach AI Settings (ChatGPT / OpenAI API)
            SettingsSectionCard(title = stringResource(R.string.settings_section_ai), icon = Icons.Default.AutoAwesome) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_ai_enable), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            stringResource(R.string.settings_ai_desc),
                            fontSize = 12.sp,
                            color = TextSecondaryLight
                        )
                    }
                    Switch(
                        checked = isAiEnabled,
                        onCheckedChange = {
                            isAiEnabled = it
                            repository.updateAiConfig(aiConfig.copy(isEnabled = it))
                        },
                        colors = appSwitchColors()
                    )
                }

                if (isAiEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))

                    AppOutlinedTextField(
                        value = aiApiKey,
                        onValueChange = {
                            aiApiKey = it
                            repository.updateAiConfig(aiConfig.copy(apiKey = it))
                        },
                        label = { Text(stringResource(R.string.settings_ai_apikey_label)) },
                        singleLine = true,
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    AppOutlinedTextField(
                        value = aiBaseUrl,
                        onValueChange = {
                            aiBaseUrl = it
                            repository.updateAiConfig(aiConfig.copy(baseUrl = it))
                        },
                        label = { Text(stringResource(R.string.settings_ai_baseurl_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    AppOutlinedTextField(
                        value = aiModelName,
                        onValueChange = {
                            aiModelName = it
                            repository.updateAiConfig(aiConfig.copy(modelName = it))
                        },
                        label = { Text(stringResource(R.string.settings_ai_model_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Section 3: Notifiche
            SettingsSectionCard(title = stringResource(R.string.settings_section_notifications), icon = Icons.Default.Notifications) {
                NotificationToggleRow(
                    title = stringResource(R.string.settings_notif_fasting),
                    checked = userProfile.notificationsFastingEnabled,
                    onCheckedChange = { repository.updateUserProfile(userProfile.copy(notificationsFastingEnabled = it)) }
                )
                NotificationToggleRow(
                    title = stringResource(R.string.settings_notif_stages),
                    checked = userProfile.notificationsStagesEnabled,
                    onCheckedChange = { repository.updateUserProfile(userProfile.copy(notificationsStagesEnabled = it)) }
                )
                NotificationToggleRow(
                    title = stringResource(R.string.settings_notif_water),
                    checked = userProfile.notificationsWaterEnabled,
                    onCheckedChange = { repository.updateUserProfile(userProfile.copy(notificationsWaterEnabled = it)) }
                )
                NotificationToggleRow(
                    title = stringResource(R.string.settings_notif_weighin),
                    checked = userProfile.notificationsWeighInEnabled,
                    onCheckedChange = { repository.updateUserProfile(userProfile.copy(notificationsWeighInEnabled = it)) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                AppSecondaryButton(
                    text = stringResource(R.string.settings_config_system_notif),
                    icon = Icons.Default.NotificationsActive,
                    onClick = {
                        NotificationHelper.openChannelSettings(context)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.settings_config_system_notif_desc),
                    fontSize = 11.5.sp,
                    color = CiboTextSecondary,
                    lineHeight = 16.sp
                )
            }

            // Section 4: Google Drive & Auto Backup Android
            SettingsSectionCard(title = stringResource(R.string.settings_section_cloud), icon = Icons.Default.CloudSync) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_cloud_auto_backup), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            stringResource(R.string.settings_cloud_auto_backup_desc),
                            fontSize = 12.sp,
                            color = TextSecondaryLight
                        )
                    }
                    Switch(
                        checked = userProfile.googleDriveAutoBackupEnabled,
                        onCheckedChange = {
                            repository.updateUserProfile(userProfile.copy(googleDriveAutoBackupEnabled = it))
                            if (it) {
                                repository.triggerAutoBackupIfEnabled()
                            }
                        },
                        colors = appSwitchColors()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppPrimaryButton(
                        text = stringResource(R.string.settings_cloud_backup_now),
                        icon = Icons.Default.CloudUpload,
                        onClick = {
                            val result = repository.exportAndShareBackup(context)
                            driveStatusMessage = if (result.isSuccess) {
                                context.getString(R.string.settings_cloud_share_started)
                            } else {
                                context.getString(R.string.settings_cloud_share_error, result.exceptionOrNull()?.localizedMessage ?: "")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    AppSecondaryButton(
                        text = stringResource(R.string.settings_cloud_restore),
                        icon = Icons.Default.CloudDownload,
                        onClick = { showJsonImportDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (driveStatusMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = driveStatusMessage ?: "",
                        fontSize = 12.sp,
                        color = FastingBlue,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Section 5: Backup Locale (Export / Import JSON)
            SettingsSectionCard(title = stringResource(R.string.settings_section_local_backup), icon = Icons.Default.SaveAlt) {
                Text(
                    text = stringResource(R.string.settings_local_backup_desc),
                    fontSize = 12.sp,
                    color = TextSecondaryLight,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppPrimaryButton(
                        text = stringResource(R.string.settings_local_export),
                        icon = Icons.Default.Share,
                        onClick = {
                            repository.exportAndShareBackup(context)
                        },
                        modifier = Modifier.weight(1f)
                    )

                    AppSecondaryButton(
                        text = stringResource(R.string.settings_local_import),
                        icon = Icons.Default.Download,
                        onClick = { showJsonImportDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (importStatusMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = importStatusMessage ?: "",
                        fontSize = 12.sp,
                        color = FastingBlue,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Section 6: Area Pericolo & Reset Dati Locali
            SettingsSectionCard(title = stringResource(R.string.settings_section_danger), icon = Icons.Default.DeleteForever) {
                Text(
                    text = stringResource(R.string.settings_danger_desc),
                    fontSize = 12.sp,
                    color = TextSecondaryLight,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                AppDangerButton(
                    text = stringResource(R.string.settings_danger_button),
                    icon = Icons.Default.DeleteForever,
                    onClick = { showResetConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth()
                )

                if (resetStatusMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = resetStatusMessage ?: "",
                        fontSize = 12.sp,
                        color = if (resetStatusMessage?.startsWith("⚠️") == true) CiboStopRed else FastingBlue,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Section: Live Update & Aggiornamenti
            val isPlayStore = com.cibotimer.BuildConfig.IS_PLAY_STORE_BUILD
            SettingsSectionCard(
                title = if (isPlayStore) stringResource(R.string.settings_update_title_play) else stringResource(R.string.settings_update_title_live),
                icon = Icons.Default.SystemUpdate
            ) {
                Text(
                    text = if (isPlayStore) {
                        stringResource(R.string.settings_update_desc_play)
                    } else {
                        stringResource(R.string.settings_update_desc_live)
                    },
                    fontSize = 12.sp,
                    color = TextSecondaryLight,
                    lineHeight = 17.sp
                )

                if (!isPlayStore) {
                    Spacer(modifier = Modifier.height(10.dp))
                    AppOutlinedTextField(
                        value = updateServerUrlText,
                        onValueChange = {
                            updateServerUrlText = it
                            updateManager?.serverUrl = it
                        },
                        label = { Text(stringResource(R.string.settings_update_server_url)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings_update_auto_check),
                        fontSize = 13.sp,
                        color = Color.White
                    )
                    Switch(
                        checked = autoCheckUpdates,
                        onCheckedChange = {
                            autoCheckUpdates = it
                            updateManager?.autoCheckEnabled = it
                        },
                        colors = appSwitchColors()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                AppPrimaryButton(
                    text = if (isPlayStore) stringResource(R.string.settings_update_check_play) else stringResource(R.string.settings_update_check_now),
                    icon = Icons.Default.Refresh,
                    onClick = {
                        if (isPlayStore) {
                            updateManager?.openPlayStore()
                        } else {
                            updateManager?.checkForUpdates(manual = true)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                when (updateState) {
                    is UpdateState.Checking -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.update_state_checking),
                            fontSize = 12.sp,
                            color = FastingBlue
                        )
                    }
                    is UpdateState.UpToDate -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.update_state_uptodate),
                            fontSize = 12.sp,
                            color = FastingBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    is UpdateState.Available -> {
                        val info = updateState.info
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.update_state_available_format, info.versionName, info.size),
                            fontSize = 12.sp,
                            color = FastingBlue,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        AppPrimaryButton(
                            text = if (isPlayStore) stringResource(R.string.update_button_play) else stringResource(R.string.update_button_direct),
                            icon = if (isPlayStore) Icons.Default.OpenInNew else Icons.Default.Download,
                            onClick = { updateManager?.downloadAndInstall(info) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    is UpdateState.Downloading -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.update_state_downloading_format, updateState.progressPercent),
                            fontSize = 12.sp,
                            color = FastingBlue
                        )
                    }
                    is UpdateState.Error -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "⚠️ ${updateState.message}",
                            fontSize = 12.sp,
                            color = CiboTimerOrange
                        )
                    }
                    else -> {}
                }
            }

            // Section: Disclaimer Medico & Salute (Google Play Compliance)
            SettingsSectionCard(title = stringResource(R.string.health_disclaimer_title), icon = Icons.Default.Info) {
                Text(
                    text = stringResource(R.string.health_disclaimer_text_full),
                    fontSize = 11.sp,
                    color = TextSecondaryLight,
                    lineHeight = 16.sp
                )
            }

            // App info footer
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.settings_footer_version_format, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextSecondaryLight
                )
                Text(
                    text = stringResource(R.string.settings_footer_subtitle),
                    fontSize = 11.sp,
                    color = TextSecondaryLight
                )
            }
        }
    }

    // Dialog: Import JSON string
    if (showJsonImportDialog) {
        AlertDialog(
            onDismissRequest = { showJsonImportDialog = false },
            title = { Text(stringResource(R.string.settings_dialog_import_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.settings_dialog_import_text), fontSize = 12.sp, color = TextSecondaryLight)
                    Spacer(modifier = Modifier.height(8.dp))
                    AppOutlinedTextField(
                        value = jsonImportText,
                        onValueChange = { jsonImportText = it },
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        singleLine = false,
                        maxLines = 10
                    )
                }
            },
            confirmButton = {
                AppPrimaryButton(
                    text = stringResource(R.string.settings_dialog_import_button),
                    onClick = {
                        val result = repository.importBackupJson(jsonImportText.trim())
                        if (result.isSuccess) {
                            importStatusMessage = context.getString(R.string.settings_local_import_success)
                            showJsonImportDialog = false
                            jsonImportText = ""
                        } else {
                            importStatusMessage = context.getString(R.string.settings_local_import_error, result.exceptionOrNull()?.localizedMessage ?: "")
                        }
                    }
                )
            },
            dismissButton = {
                AppSecondaryButton(
                    text = stringResource(R.string.button_cancel),
                    onClick = { showJsonImportDialog = false }
                )
            }
        )
    }

    // Dialog: Conferma Reset Dati Locali
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = CiboStopRed,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.settings_dialog_reset_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.settings_dialog_reset_confirm_q),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = stringResource(R.string.settings_dialog_reset_details),
                        fontSize = 13.sp,
                        color = TextSecondaryLight,
                        lineHeight = 19.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings_dialog_reset_warning),
                        fontSize = 12.5.sp,
                        color = CiboStopRed,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmDialog = false
                        val result = repository.resetAllLocalData()
                        if (result.isSuccess) {
                            resetStatusMessage = context.getString(R.string.settings_reset_success)
                        } else {
                            resetStatusMessage = context.getString(R.string.settings_reset_error, result.exceptionOrNull()?.localizedMessage ?: "")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CiboStopRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(stringResource(R.string.settings_dialog_reset_button), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                AppSecondaryButton(
                    text = stringResource(R.string.button_cancel),
                    onClick = { showResetConfirmDialog = false }
                )
            },
            containerColor = SurfaceDark,
            tonalElevation = 8.dp
        )
    }
}

@Composable
fun SettingsSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = FastingBlue, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
fun NotificationToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, fontSize = 14.sp, color = TextPrimary)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = appSwitchColors()
        )
    }
}
