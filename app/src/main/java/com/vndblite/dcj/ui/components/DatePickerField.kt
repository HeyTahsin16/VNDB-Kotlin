package com.vndblite.dcj.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vndblite.dcj.R
import com.vndblite.dcj.domain.model.ThemeId
import com.vndblite.dcj.ui.theme.VndbTheme
import com.vndblite.dcj.ui.theme.vndbOutlinedTextFieldColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Material 3 DatePicker field with a dedicated Clear button, ISO `"YYYY-MM-DD"` output,
 * and >= 48.dp touch targets, replacing unvalidated free-text date inputs (F8, U1, U2, U6).
 */
@Composable
fun VndbDatePickerField(
    label: String,
    date: LocalDate?,
    onDateChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    fieldTestTag: String? = null,
    clearButtonTestTag: String? = null
) {
    var showPickerDialog by rememberSaveable { mutableStateOf(false) }
    val notSetPlaceholder = stringResource(R.string.collection_date_not_set)
    val pickContentDesc = stringResource(R.string.collection_date_pick_cd, label)
    val clearContentDesc = stringResource(R.string.collection_date_clear_cd, label)

    Box(modifier = modifier) {
        OutlinedTextField(
            value = date?.toString().orEmpty(),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            isError = isError,
            label = { Text(label) },
            placeholder = { Text(notSetPlaceholder) },
            leadingIcon = {
                IconButton(
                    onClick = { showPickerDialog = true },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = pickContentDesc
                    )
                }
            },
            trailingIcon = {
                if (date != null) {
                    val clearMod = Modifier
                        .minimumInteractiveComponentSize()
                        .let { if (clearButtonTestTag != null) it.testTag(clearButtonTestTag) else it }
                    IconButton(onClick = { onDateChange(null) }, modifier = clearMod) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = clearContentDesc)
                    }
                }
            },
            colors = vndbOutlinedTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    role = Role.Button
                    contentDescription = pickContentDesc
                }
                .clickable(role = Role.Button) { showPickerDialog = true }
                .let { if (fieldTestTag != null) it.testTag(fieldTestTag) else it }
        )
    }

    if (showPickerDialog) {
        VndbDatePickerModalDialog(
            initialDate = date,
            onDatePicked = { picked ->
                onDateChange(picked)
                showPickerDialog = false
            },
            onDismiss = { showPickerDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VndbDatePickerModalDialog(
    initialDate: LocalDate?,
    onDatePicked: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val initialMillis = initialDate
        ?.atStartOfDay(ZoneOffset.UTC)
        ?.toInstant()
        ?.toEpochMilli()
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedMillis = pickerState.selectedDateMillis
                    if (selectedMillis != null) {
                        val pickedDate = Instant.ofEpochMilli(selectedMillis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                        onDatePicked(pickedDate)
                    } else {
                        onDismiss()
                    }
                },
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text(stringResource(R.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    ) {
        DatePicker(state = pickerState)
    }
}

@Preview(name = "VndbDatePickerField - With Date", showBackground = true)
@Composable
private fun VndbDatePickerFieldSetPreview() {
    VndbTheme(themeId = ThemeId.AGL, includeBackground = true) {
        VndbDatePickerField(
            label = "Started",
            date = LocalDate.of(2026, 3, 15),
            onDateChange = {},
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Preview(name = "VndbDatePickerField - Empty Light", showBackground = true)
@Composable
private fun VndbDatePickerFieldEmptyPreview() {
    VndbTheme(themeId = ThemeId.AIR, includeBackground = true) {
        VndbDatePickerField(
            label = "Finished",
            date = null,
            onDateChange = {},
            modifier = Modifier.padding(12.dp)
        )
    }
}
