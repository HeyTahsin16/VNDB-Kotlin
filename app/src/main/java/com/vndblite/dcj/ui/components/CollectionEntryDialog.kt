package com.vndblite.dcj.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.domain.model.CollectionEntry
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.ThemeId
import com.vndblite.dcj.domain.model.VnSummary
import com.vndblite.dcj.ui.theme.VndbTheme
import com.vndblite.dcj.ui.theme.VndbThemeTokens
import com.vndblite.dcj.ui.theme.vndbOutlinedTextFieldColors
import java.time.LocalDate

/**
 * Convenience overload for screens that pass `vnTitle`, `isAccountSynced`, and `onRemove`,
 * automatically displaying [RemoveSingleEntryConfirmDialog] before removing an entry (F8, F9).
 */
@Composable
fun CollectionEntryDialog(
    vnTitle: String,
    existingEntry: CollectionEntry?,
    isAccountSynced: Boolean = false,
    today: LocalDate = LocalDate.now(),
    onSave: (CollectionEntryFormPayload) -> Unit,
    onRemove: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var showRemoveConfirm by rememberSaveable { mutableStateOf(false) }
    val syntheticVn = VnSummary(
        id = existingEntry?.vnId ?: "v0",
        title = vnTitle
    )
    CollectionEntryDialog(
        vn = syntheticVn,
        existingEntry = existingEntry,
        today = today,
        onDismiss = onDismiss,
        onSave = onSave,
        onRequestRemove = if (existingEntry != null && onRemove != null) {
            { showRemoveConfirm = true }
        } else {
            null
        }
    )
    if (showRemoveConfirm && onRemove != null) {
        RemoveSingleEntryConfirmDialog(
            vnTitle = vnTitle,
            isAccountSignedIn = isAccountSynced,
            onConfirmRemove = {
                showRemoveConfirm = false
                onRemove()
            },
            onDismiss = { showRemoveConfirm = false }
        )
    }
}

/**
 * Domain-model-backed dialog for adding or editing a [CollectionEntry] (F8, F9, U1, U2, U6, U7).
 */
@Composable
fun CollectionEntryDialog(
    vn: VnSummary,
    existingEntry: CollectionEntry?,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSave: (CollectionEntryFormDraft) -> Unit,
    onRequestRemove: (() -> Unit)? = null
) {
    val isNewEntry = existingEntry == null
    var selectedStatusName by rememberSaveable(vn.id, existingEntry?.status) {
        mutableStateOf(CollectionEntryFormRules.initialStatus(existingEntry).name)
    }
    val selectedStatus = CollectionStatus.fromStorageKeyOrDefault(selectedStatusName)

    var selectedVote by rememberSaveable(vn.id, existingEntry?.vote) {
        mutableStateOf(CollectionEntry.normalizeVote(existingEntry?.vote))
    }
    var notes by rememberSaveable(vn.id, existingEntry?.notes) {
        mutableStateOf(existingEntry?.notes.orEmpty())
    }
    var startedDateIso by rememberSaveable(vn.id, existingEntry?.startedDate) {
        val initial = CollectionEntryFormRules.initialStartedDate(existingEntry, selectedStatus, today)
        mutableStateOf(initial?.toString().orEmpty())
    }
    var finishedDateIso by rememberSaveable(vn.id, existingEntry?.finishedDate) {
        mutableStateOf(existingEntry?.finishedDate?.toString().orEmpty())
    }

    val startedDate = CollectionEntry.parseLocalDateOrNull(startedDateIso)
    val finishedDate = CollectionEntry.parseLocalDateOrNull(finishedDateIso)
    val isDateValid = CollectionEntryFormRules.isDateRangeValid(startedDate, finishedDate)
    val draft = CollectionEntryFormDraft(
        status = selectedStatus,
        vote = selectedVote,
        notes = notes.trim().takeIf { it.isNotEmpty() },
        startedDate = startedDate,
        finishedDate = finishedDate
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { CollectionEntryDialogTitle(isNewEntry = isNewEntry, vnTitle = vn.title) },
        text = {
            CollectionEntryDialogBody(
                draft = draft,
                rawNotes = notes,
                isDateValid = isDateValid,
                onStatusSelected = { newStatus ->
                    val updatedStart = CollectionEntryFormRules.onStatusChanged(
                        isNewEntry = isNewEntry,
                        previousStatus = selectedStatus,
                        newStatus = newStatus,
                        currentStartedDate = startedDate,
                        today = today
                    )
                    selectedStatusName = newStatus.name
                    startedDateIso = updatedStart?.toString().orEmpty()
                },
                onVoteSelected = { selectedVote = it },
                onStartedDateChange = { startedDateIso = it?.toString().orEmpty() },
                onFinishedDateChange = { finishedDateIso = it?.toString().orEmpty() },
                onNotesChange = { notes = it }
            )
        },
        confirmButton = {
            CollectionEntrySaveButton(
                enabled = isDateValid,
                onClick = { if (isDateValid) onSave(draft) }
            )
        },
        dismissButton = {
            CollectionEntryDismissRow(
                showRemove = !isNewEntry && onRequestRemove != null,
                onRequestRemove = { onRequestRemove?.invoke() },
                onDismiss = onDismiss
            )
        }
    )
}

@Composable
private fun CollectionEntryDialogTitle(
    isNewEntry: Boolean,
    vnTitle: String
) {
    Column {
        Text(
            text = if (isNewEntry) {
                stringResource(R.string.add_to_collection)
            } else {
                stringResource(R.string.edit_collection_entry)
            },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = vnTitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CollectionEntryDialogBody(
    draft: CollectionEntryFormDraft,
    rawNotes: String,
    isDateValid: Boolean,
    onStatusSelected: (CollectionStatus) -> Unit,
    onVoteSelected: (Int?) -> Unit,
    onStartedDateChange: (LocalDate?) -> Unit,
    onFinishedDateChange: (LocalDate?) -> Unit,
    onNotesChange: (String) -> Unit
) {
    val semanticColors = VndbThemeTokens.colors
    var voteExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.collection_status_label),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        StatusSelectionGrid(
            selectedStatus = draft.status,
            onStatusSelected = onStatusSelected
        )

        Text(
            text = stringResource(R.string.collection_vote_label),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        VoteSelectorField(
            selectedVote = draft.vote,
            expanded = voteExpanded,
            onExpandedChange = { voteExpanded = it },
            onVoteSelected = { vote ->
                onVoteSelected(vote)
                voteExpanded = false
            }
        )

        CollectionEntryDatesRow(
            startedDate = draft.startedDate,
            finishedDate = draft.finishedDate,
            isDateValid = isDateValid,
            onStartedDateChange = onStartedDateChange,
            onFinishedDateChange = onFinishedDateChange
        )

        if (!isDateValid) {
            Text(
                text = stringResource(R.string.collection_date_validation_error),
                style = MaterialTheme.typography.labelMedium,
                color = semanticColors.danger,
                modifier = Modifier.testTag("collection_date_validation_error_text")
            )
        }

        OutlinedTextField(
            value = rawNotes,
            onValueChange = onNotesChange,
            label = { Text(stringResource(R.string.collection_notes_label)) },
            minLines = 2,
            maxLines = 5,
            colors = vndbOutlinedTextFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(name = "CollectionEntryDialog - New Entry Dark Theme", showBackground = true)
@Composable
private fun CollectionEntryDialogNewPreview() {
    VndbTheme(themeId = ThemeId.AGL) {
        CollectionEntryDialog(
            vn = PreviewData.sampleVnSummary,
            existingEntry = null,
            today = LocalDate.of(2026, 3, 15),
            onDismiss = {},
            onSave = {}
        )
    }
}

@Preview(name = "CollectionEntryDialog - Edit Entry Light Theme", showBackground = true)
@Composable
private fun CollectionEntryDialogEditPreview() {
    VndbTheme(themeId = ThemeId.LILBUSTS) {
        CollectionEntryDialog(
            vn = PreviewData.sampleVnSummary,
            existingEntry = PreviewData.sampleCollectionEntry,
            today = LocalDate.of(2026, 3, 15),
            onDismiss = {},
            onSave = {},
            onRequestRemove = {}
        )
    }
}
