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
import androidx.compose.ui.unit.sp
import com.ezworksafe.BuildConfig

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
                Text(
                    text = "ezWorkSafe v${BuildConfig.VERSION_NAME}",
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Real-time monitoring of WiFi, Bluetooth, " +
                            "Microphone, and Camera status for workplace " +
                            "safety and privacy monitoring.",
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
                    text = "The app runs a foreground service that " +
                            "continuously monitors four device sensors.",
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
                    text = "Microphone and Camera status use system " +
                            "callbacks to detect when another app is " +
                            "actively using them.",
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
                    explanation = "Checks whether WiFi is enabled or " +
                            "disabled. Auto-granted by the system."
                )
                PermissionRow(
                    permission = "BLUETOOTH / BLUETOOTH_CONNECT",
                    explanation = "Checks whether Bluetooth is enabled or " +
                            "disabled. BLUETOOTH_CONNECT requires runtime " +
                            "permission on Android 12+."
                )
                PermissionRow(
                    permission = "RECORD_AUDIO",
                    explanation = "Detects if any app is currently using " +
                            "the microphone. Requires runtime permission."
                )
                PermissionRow(
                    permission = "CAMERA",
                    explanation = "Detects if any app is currently using " +
                            "the camera. Requires runtime permission."
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
