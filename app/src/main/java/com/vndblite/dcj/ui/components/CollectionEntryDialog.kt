package com.vndblite.dcj.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.domain.model.CollectionEntry
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.domain.model.ThemeId
import com.vndblite.dcj.domain.model.VnSummary
import com.vndblite.dcj.ui.format.VoteFormatters
import com.vndblite.dcj.ui.theme.VndbTheme
import com.vndblite.dcj.ui.theme.VndbThemeTokens
import com.vndblite.dcj.ui.theme.vndbOutlinedTextFieldColors
import java.time.LocalDate

/**
 * Pure, unit-testable rules for pre-filling and validating collection entry dates (F8).
 *
 * - **Never** pre-fills `startedDate` for `WISHLIST` or `BLACKLIST` entries.
 * - Pre-fills `startedDate = today` **only** for brand-new `PLAYING` entries (`existingEntry == null`).
 * - Validates `finishedDate >= startedDate` when both dates are present.
 */
object CollectionEntryFormRules {

    fun initialStatus(existingEntry: CollectionEntry?): CollectionStatus =
        existingEntry?.status ?: CollectionStatus.PLAYING

    fun initialStartedDate(
        existingEntry: CollectionEntry?,
        status: CollectionStatus,
        today: LocalDate
    ): LocalDate? {
        if (existingEntry != null) {
            return existingEntry.startedDate
        }
        return if (status == CollectionStatus.PLAYING) today else null
    }

    /**
     * Adjusts `startedDate` when the user switches status on a brand-new entry:
     * - If the user had the auto-prefilled `today` date on a brand-new entry and switches to
     *   `WISHLIST` or `BLACKLIST`, clears the auto-prefilled start date so Wishlist/Blacklist
     *   never accidentally sync a start date (F8).
     */
    fun onStatusChanged(
        isNewEntry: Boolean,
        previousStatus: CollectionStatus,
        newStatus: CollectionStatus,
        currentStartedDate: LocalDate?,
        today: LocalDate
    ): LocalDate? {
        if (!isNewEntry) return currentStartedDate
        return when {
            (newStatus == CollectionStatus.WISHLIST || newStatus == CollectionStatus.BLACKLIST) &&
                currentStartedDate == today -> null
            newStatus == CollectionStatus.PLAYING &&
                (previousStatus == CollectionStatus.WISHLIST || previousStatus == CollectionStatus.BLACKLIST) &&
                currentStartedDate == null -> today
            else -> currentStartedDate
        }
    }

    fun isDateRangeValid(startedDate: LocalDate?, finishedDate: LocalDate?): Boolean {
        if (startedDate == null || finishedDate == null) return true
        return !finishedDate.isBefore(startedDate)
    }
}

/**
 * Submitted form payload from [CollectionEntryDialog] (Rule 2: <= 8 parameters).
 */
data class CollectionEntryFormDraft(
    val status: CollectionStatus,
    /** Vote in VNDB's `10..100` integer scale, or `null` if unvoted (A7, Y3). */
    val vote: Int?,
    val notes: String?,
    val startedDate: LocalDate?,
    val finishedDate: LocalDate?
)

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

@Composable
private fun CollectionEntryDatesRow(
    startedDate: LocalDate?,
    finishedDate: LocalDate?,
    isDateValid: Boolean,
    onStartedDateChange: (LocalDate?) -> Unit,
    onFinishedDateChange: (LocalDate?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        VndbDatePickerField(
            label = stringResource(R.string.collection_started_label),
            date = startedDate,
            onDateChange = onStartedDateChange,
            isError = !isDateValid,
            fieldTestTag = "collection_started_date_field",
            clearButtonTestTag = "collection_started_date_clear",
            modifier = Modifier.weight(1f)
        )
        VndbDatePickerField(
            label = stringResource(R.string.collection_finished_label),
            date = finishedDate,
            onDateChange = onFinishedDateChange,
            isError = !isDateValid,
            fieldTestTag = "collection_finished_date_field",
            clearButtonTestTag = "collection_finished_date_clear",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CollectionEntrySaveButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    val semanticColors = VndbThemeTokens.colors
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = semanticColors.onAccentButton
        ),
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("save_collection_record_button")
    ) {
        Text(
            text = stringResource(R.string.collection_save_button),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CollectionEntryDismissRow(
    showRemove: Boolean,
    onRequestRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    val semanticColors = VndbThemeTokens.colors
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (showRemove) {
            TextButton(
                onClick = onRequestRemove,
                colors = ButtonDefaults.textButtonColors(contentColor = semanticColors.danger),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("remove_collection_record_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.collection_remove_button))
            }
        }
        TextButton(
            onClick = onDismiss,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.minimumInteractiveComponentSize()
        ) {
            Text(stringResource(R.string.dialog_cancel))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoteSelectorField(
    selectedVote: Int?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onVoteSelected: (Int?) -> Unit
) {
    val semanticColors = VndbThemeTokens.colors
    val currentLabel = stringResource(VoteFormatters.labelResIdForVote(selectedVote))

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        OutlinedTextField(
            value = currentLabel,
            onValueChange = {},
            readOnly = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = semanticColors.starGold
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = vndbOutlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            VoteFormatters.options.forEach { (voteValue, labelRes) ->
                DropdownMenuItem(
                    text = { Text(stringResource(labelRes)) },
                    onClick = { onVoteSelected(voteValue) }
                )
            }
        }
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
