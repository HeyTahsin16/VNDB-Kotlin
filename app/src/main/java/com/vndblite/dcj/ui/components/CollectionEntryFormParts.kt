package com.vndblite.dcj.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.domain.model.CollectionEntry
import com.vndblite.dcj.domain.model.CollectionStatus
import com.vndblite.dcj.ui.format.VoteFormatters
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

typealias CollectionEntryFormPayload = CollectionEntryFormDraft

@Composable
internal fun CollectionEntryDatesRow(
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
internal fun CollectionEntrySaveButton(
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
internal fun CollectionEntryDismissRow(
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
internal fun VoteSelectorField(
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
