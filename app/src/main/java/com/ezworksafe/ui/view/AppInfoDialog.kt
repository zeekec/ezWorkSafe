// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Erik Zeek

package com.ezworksafe.ui.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp

/**
 * Modal bottom sheet with About, How It Works, and Permissions sections.
 *
 * The About section shows the app version and a summary of its purpose.
 * How It Works explains the real-time (WiFi/BT) vs snapshot (Mic/Cam) architecture.
 * Permissions lists each permission with an explanation specific to this app's use.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppInfoDialog(
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "About & Help",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            ExpandableSection(
                title = "About",
                initiallyExpanded = true
            ) {
                val context = LocalContext.current
                val versionName = if (Build.VERSION.SDK_INT >= 33) {
                    context.packageManager.getPackageInfo(
                        context.packageName,
                        PackageManager.PackageInfoFlags.of(0)
                    ).versionName
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                } ?: "?"
                Text(
                    text = "ezWorkSafe v$versionName",
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Real-time visibility into WiFi, Bluetooth, " +
                            "Microphone, and Camera access status for " +
                            "workplace safety and privacy.",
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Copyright 2026 Erik Zeek",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Licensed under Apache License 2.0",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            ExpandableSection(
                title = "How It Works"
            ) {
                Text(
                    text = "A foreground service keeps WiFi and Bluetooth " +
                            "status updated in real-time, and checks " +
                            "Microphone and Camera access each time you " +
                            "open the app.",
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "WiFi and Bluetooth status are detected via " +
                            "system broadcasts and update in real-time.",
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Microphone and Camera status show whether " +
                            "those sensors are accessible to this app, based " +
                            "on permission grants and system privacy toggles.",
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "A home screen widget shows status at a glance.",
                    fontWeight = FontWeight.Medium,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Left section (WiFi, BT): updates in real-time " +
                            "via system broadcasts.",
                    lineHeight = 20.sp
                )
                Text(
                    text = "Right section (Mic, Cam): reflects the state " +
                            "from the last time you opened the app.",
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Android 16 restricts background detection of " +
                            "Mic/Cam privacy toggles. To refresh this " +
                            "section, open the app or tap \"Refresh\" in " +
                            "the notification.",
                    color = MaterialTheme.colorScheme.error,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "While the app is open, all sensor states refresh " +
                            "every 2 seconds to detect privacy toggle " +
                            "changes.",
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tapping the widget opens the app.",
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            ExpandableSection(
                title = "Permissions"
            ) {
                PermissionRow(
                    permission = "ACCESS_WIFI_STATE",
                    explanation = "Required to display whether WiFi is " +
                            "enabled. Auto-granted by the system."
                )
                PermissionRow(
                    permission = "BLUETOOTH / BLUETOOTH_CONNECT",
                    explanation = "Required to display whether Bluetooth is " +
                            "enabled. BLUETOOTH_CONNECT requires runtime " +
                            "permission on Android 12+."
                )
                PermissionRow(
                    permission = "RECORD_AUDIO",
                    explanation = "Required to verify microphone privacy " +
                            "toggle status. The app never records audio."
                )
                PermissionRow(
                    permission = "CAMERA",
                    explanation = "Required to verify camera privacy " +
                            "toggle status. The app never captures images."
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ExpandableSection(
    title: String,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (expanded) "Collapse" else "Expand"
        )
    }

    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun PermissionRow(
    permission: String,
    explanation: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = permission,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = explanation,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
    }
}
