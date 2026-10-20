package com.vndblite.dcj.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.data.model.CollectionStatusCode
import com.vndblite.dcj.data.model.VnDataPhase01
import com.vndblite.dcj.data.model.VnRecord
import com.vndblite.dcj.data.model.VoteData
import com.vndblite.dcj.ui.theme.LocalAppThemeCode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionRecordDialog(
    vn: VnDataPhase01,
    existingRecord: VnRecord?,
    onDismiss: () -> Unit,
    onSave: (status: String, vote: Int, notes: String?, started: String?, finished: String?) -> Unit,
    onRemove: (() -> Unit)? = null
) {
    val theme = LocalAppThemeCode.current
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    var selectedStatus by remember {
        mutableStateOf(existingRecord?.status ?: CollectionStatusCode.PLAYING.statusName)
    }
    var selectedVote by remember {
        mutableIntStateOf(existingRecord?.vote ?: 0)
    }
    var notes by remember {
        mutableStateOf(existingRecord?.notes ?: "")
    }
    var startedDate by remember {
        mutableStateOf(existingRecord?.started ?: today)
    }
    var finishedDate by remember {
        mutableStateOf(existingRecord?.finished ?: "")
    }
    var voteExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.primary,
        titleContentColor = theme.tertiary,
        textContentColor = theme.tertiary,
        title = {
            Column {
                Text(
                    text = if (existingRecord == null) "Add to Collection" else "Edit Collection Entry",
                    style = MaterialTheme.typography.titleLarge,
                    color = theme.secondary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = vn.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = theme.tertiary.copy(alpha = 0.85f)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Status",
                    style = MaterialTheme.typography.titleMedium,
                    color = theme.secondary
                )

                // Status chips grid (2 columns x 3 rows)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CollectionStatusCode.entries.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { statusItem ->
                                val isChosen = selectedStatus.equals(statusItem.statusName, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isChosen) statusItem.color.copy(alpha = 0.28f) else theme.tertiary.copy(alpha = 0.08f),
                                    border = if (isChosen) {
                                        androidx.compose.foundation.BorderStroke(1.5.dp, statusItem.color)
                                    } else null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedStatus = statusItem.statusName }
                                        .testTag("status_option_${statusItem.statusName}")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        AssetImage(
                                            assetPath = statusItem.imagePath,
                                            contentDescription = statusItem.displayTitle,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = statusItem.displayTitle,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                            color = theme.tertiary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Vote dropdown
                Text(
                    text = "Your Rating (Vote)",
                    style = MaterialTheme.typography.titleMedium,
                    color = theme.secondary
                )
                ExposedDropdownMenuBox(
                    expanded = voteExpanded,
                    onExpandedChange = { voteExpanded = !voteExpanded }
                ) {
                    OutlinedTextField(
                        value = VoteData.VOTE_LABELS[selectedVote] ?: "- (None)",
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD54F))
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = voteExpanded)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = theme.tertiary,
                            unfocusedTextColor = theme.tertiary,
                            focusedBorderColor = theme.secondary,
                            unfocusedBorderColor = theme.tertiary.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = voteExpanded,
                        onDismissRequest = { voteExpanded = false }
                    ) {
                        VoteData.VOTE_LABELS.forEach { (score, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    selectedVote = score
                                    voteExpanded = false
                                }
                            )
                        }
                    }
                }

                // Started & Finished Dates (YYYY-MM-DD)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startedDate,
                        onValueChange = { startedDate = it },
                        label = { Text("Started (YYYY-MM-DD)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = theme.tertiary,
                            unfocusedTextColor = theme.tertiary,
                            focusedBorderColor = theme.secondary,
                            unfocusedBorderColor = theme.tertiary.copy(alpha = 0.4f),
                            focusedLabelColor = theme.secondary,
                            unfocusedLabelColor = theme.tertiary.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = finishedDate,
                        onValueChange = { finishedDate = it },
                        label = { Text("Finished (YYYY-MM-DD)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = theme.tertiary,
                            unfocusedTextColor = theme.tertiary,
                            focusedBorderColor = theme.secondary,
                            unfocusedBorderColor = theme.tertiary.copy(alpha = 0.4f),
                            focusedLabelColor = theme.secondary,
                            unfocusedLabelColor = theme.tertiary.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Personal Review / Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Personal Review / Notes") },
                    minLines = 2,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = theme.tertiary,
                        unfocusedTextColor = theme.tertiary,
                        focusedBorderColor = theme.secondary,
                        unfocusedBorderColor = theme.tertiary.copy(alpha = 0.4f),
                        focusedLabelColor = theme.secondary,
                        unfocusedLabelColor = theme.tertiary.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        selectedStatus,
                        selectedVote,
                        notes.takeIf { it.isNotBlank() },
                        startedDate.takeIf { it.isNotBlank() },
                        finishedDate.takeIf { it.isNotBlank() }
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = theme.secondary,
                    contentColor = Color.Black
                ),
                modifier = Modifier.testTag("save_collection_record_button")
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (existingRecord != null && onRemove != null) {
                    TextButton(
                        onClick = onRemove,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF5252)),
                        modifier = Modifier.testTag("remove_collection_record_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Remove")
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = theme.tertiary)
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}
