package com.vndblite.dcj.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.domain.model.CollectionEntry
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.SyncState
import com.vndblite.dcj.domain.model.ThemeId
import com.vndblite.dcj.domain.model.VnSummary
import com.vndblite.dcj.ui.format.VnFormatters
import com.vndblite.dcj.ui.format.labelRes
import com.vndblite.dcj.ui.theme.ColorTokens
import com.vndblite.dcj.ui.theme.LocalThemeSpec
import com.vndblite.dcj.ui.theme.VndbTheme
import com.vndblite.dcj.ui.theme.badgeColor
import com.vndblite.dcj.ui.theme.vndbOutlinedTextFieldColors
import java.time.LocalDate

data class CollectionEntryDraft(
    val status: CollectionStatus,
    /** Vote in VNDB's 10..100 integer scale, or `null` if unvoted. */
    val vote: Int?,
    val notes: String?,
    val startedDate: LocalDate?,
    val finishedDate: LocalDate?
)

/**
 * Dialog for adding or editing a [CollectionEntry] (D6).
 * - Never prefills `startedDate` for Wishlist/Blacklist; prefills today's date only for a brand-new `PLAYING` entry.
 * - Uses Material 3 `DatePickerField` with Clear button.
 * - Validates `finishedDate >= startedDate`.
 * - Confirms before removing an existing entry, noting remote VNDB deletion when synced.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionEntryDialog(
    vn: VnSummary,
    existingEntry: CollectionEntry?,
    defaultToday: LocalDate = LocalDate.now(),
    onDismiss: () -> Unit,
    onSave: (CollectionEntryDraft) -> Unit,
    onRemove: (() -> Unit)? = null
) {
    val themeSpec = LocalThemeSpec.current
    val isBrandNew = existingEntry == null

    var statusName by rememberSaveable(vn.id, existingEntry?.status) {
        mutableStateOf((existingEntry?.status ?: CollectionStatus.PLAYING).name)
    }
    val selectedStatus = CollectionStatus.fromStorageKeyOrDefault(statusName)

    var tenPointVote by rememberSaveable(vn.id, existingEntry?.vote) {
        mutableIntStateOf(existingEntry?.vote?.let { (it / 10).coerceIn(1, 10) } ?: 0)
    }
    var notes by rememberSaveable(vn.id, existingEntry?.notes) {
        mutableStateOf(existingEntry?.notes.orEmpty())
    }
    var startedDateStr by rememberSaveable(vn.id, existingEntry?.startedDate) {
        val initialStarted = if (isBrandNew) {
            defaultToday.toString()
        } else {
            existingEntry?.startedDate?.toString().orEmpty()
        }
        mutableStateOf(initialStarted)
    }
    var userEditedStartedDate by rememberSaveable(vn.id) { mutableStateOf(!isBrandNew) }
    var finishedDateStr by rememberSaveable(vn.id, existingEntry?.finishedDate) {
        mutableStateOf(existingEntry?.finishedDate?.toString().orEmpty())
    }
    var voteExpanded by rememberSaveable { mutableStateOf(false) }
    var showRemoveConfirm by rememberSaveable { mutableStateOf(false) }

    val startedDate = CollectionEntry.parseLocalDateOrNull(startedDateStr)
    val finishedDate = CollectionEntry.parseLocalDateOrNull(finishedDateStr)
    val isDateOrderInvalid = startedDate != null && finishedDate != null && finishedDate.isBefore(startedDate)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = themeSpec.surfaceColor,
        titleContentColor = themeSpec.contentColor,
        textContentColor = themeSpec.contentColor,
        title = {
            Column {
                Text(
                    text = if (isBrandNew) {
                        stringResource(R.string.add_to_collection)
                    } else {
                        stringResource(R.string.edit_collection_entry)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = themeSpec.accentColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = vn.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = themeSpec.contentColor.copy(alpha = 0.85f)
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
                    text = stringResource(R.string.collection_field_status),
                    style = MaterialTheme.typography.titleMedium,
                    color = themeSpec.accentColor
                )

                StatusOptionGrid(
                    selectedStatus = selectedStatus,
                    onSelectStatus = { newStatus ->
                        statusName = newStatus.name
                        if (isBrandNew && !userEditedStartedDate) {
                            startedDateStr = if (newStatus == CollectionStatus.PLAYING) {
                                defaultToday.toString()
                            } else {
                                ""
                            }
                        }
                    }
                )

                Text(
                    text = stringResource(R.string.collection_field_vote),
                    style = MaterialTheme.typography.titleMedium,
                    color = themeSpec.accentColor
                )
                ExposedDropdownMenuBox(
                    expanded = voteExpanded,
                    onExpandedChange = { voteExpanded = !voteExpanded }
                ) {
                    OutlinedTextField(
                        value = stringResource(VnFormatters.voteScoreLabelRes(tenPointVote.takeIf { it in 1..10 })),
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = ColorTokens.RatingStarGold
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = voteExpanded)
                        },
                        colors = vndbOutlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag("collection_vote_dropdown")
                    )
                    ExposedDropdownMenu(
                        expanded = voteExpanded,
                        onDismissRequest = { voteExpanded = false }
                    ) {
                        for (score in 0..10) {
                            DropdownMenuItem(
                                text = {
                                    Text(stringResource(VnFormatters.voteScoreLabelRes(score.takeIf { it > 0 })))
                                },
                                onClick = {
                                    tenPointVote = score
                                    voteExpanded = false
                                },
                                modifier = Modifier.testTag("vote_option_$score")
                            )
                        }
                    }
                }

                DatePickerField(
                    label = stringResource(R.string.collection_field_started),
                    selectedDate = startedDate,
                    onDateSelected = { picked ->
                        userEditedStartedDate = true
                        startedDateStr = picked?.toString().orEmpty()
                    },
                    isError = isDateOrderInvalid,
                    fieldTestTag = "started_date_field",
                    clearTestTag = "started_date_clear"
                )

                DatePickerField(
                    label = stringResource(R.string.collection_field_finished),
                    selectedDate = finishedDate,
                    onDateSelected = { picked ->
                        finishedDateStr = picked?.toString().orEmpty()
                    },
                    isError = isDateOrderInvalid,
                    fieldTestTag = "finished_date_field",
                    clearTestTag = "finished_date_clear"
                )

                if (isDateOrderInvalid) {
                    Text(
                        text = stringResource(R.string.collection_date_invalid_order),
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorTokens.DestructiveRed,
                        modifier = Modifier.testTag("collection_date_order_error")
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.collection_field_notes)) },
                    minLines = 2,
                    maxLines = 5,
                    colors = vndbOutlinedTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("collection_notes_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        CollectionEntryDraft(
                            status = selectedStatus,
                            vote = if (tenPointVote in 1..10) tenPointVote * 10 else null,
                            notes = notes.trim().takeIf { it.isNotEmpty() },
                            startedDate = startedDate,
                            finishedDate = finishedDate
                        )
                    )
                },
                enabled = !isDateOrderInvalid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = themeSpec.accentColor,
                    contentColor = themeSpec.onAccentColor
                ),
                modifier = Modifier.testTag("save_collection_record_button")
            ) {
                Text(stringResource(R.string.action_save), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (existingEntry != null && onRemove != null) {
                    TextButton(
                        onClick = { showRemoveConfirm = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = ColorTokens.DestructiveRed),
                        modifier = Modifier.testTag("remove_collection_record_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.action_remove))
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = themeSpec.contentColor)
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    )

    if (showRemoveConfirm && existingEntry != null && onRemove != null) {
        val isSyncedEntry = existingEntry.remoteLastMod != null ||
            existingEntry.syncState == SyncState.CLEAN ||
            existingEntry.syncState == SyncState.DIRTY
        ConfirmDialog(
            title = stringResource(R.string.collection_remove_confirm_title),
            message = if (isSyncedEntry) {
                stringResource(R.string.collection_remove_confirm_synced, vn.title)
            } else {
                stringResource(R.string.collection_remove_confirm_local, vn.title)
            },
            confirmLabel = stringResource(R.string.action_remove),
            isDestructive = true,
            confirmTestTag = "confirm_remove_collection_record_button",
            onConfirm = {
                showRemoveConfirm = false
                onRemove()
            },
            onDismiss = { showRemoveConfirm = false }
        )
    }
}

@Composable
private fun StatusOptionGrid(
    selectedStatus: CollectionStatus,
    onSelectStatus: (CollectionStatus) -> Unit
) {
    val themeSpec = LocalThemeSpec.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        CollectionStatus.entries.chunked(2).forEach { rowStatuses ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowStatuses.forEach { statusItem ->
                    val isChosen = selectedStatus == statusItem
                    val statusColor = statusItem.badgeColor()
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isChosen) {
                            statusColor.copy(alpha = 0.28f)
                        } else {
                            themeSpec.contentColor.copy(alpha = 0.08f)
                        },
                        border = if (isChosen) BorderStroke(1.5.dp, statusColor) else null,
                        modifier = Modifier
                            .weight(1f)
                            .semantics {
                                role = Role.RadioButton
                                selected = isChosen
                            }
                            .clickable { onSelectStatus(statusItem) }
                            .testTag("status_option_${statusItem.storageKey}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            AssetImage(
                                assetPath = AssetPaths.collectionStatusIconPath(statusItem),
                                contentDescription = stringResource(statusItem.labelRes()),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(statusItem.labelRes()),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                color = themeSpec.contentColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CollectionEntryDialogPreview() {
    VndbTheme(themeId = ThemeId.AGL) {
        CollectionEntryDialog(
            vn = VnSummary(id = "v11", title = "Fate/stay night"),
            existingEntry = null,
            onDismiss = {},
            onSave = {}
        )
    }
}
