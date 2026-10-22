package com.vndblite.dcj.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.vndblite.dcj.R
import com.vndblite.dcj.domain.model.ThemeId
import com.vndblite.dcj.ui.theme.VndbTheme
import com.vndblite.dcj.ui.theme.vndbOutlinedTextFieldColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Material 3 DatePicker field with Clear button and saveable picker dialog state (D6).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    fieldTestTag: String = "date_picker_field",
    clearTestTag: String = "date_picker_clear"
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }

    OutlinedTextField(
        value = selectedDate?.toString().orEmpty(),
        onValueChange = {},
        readOnly = true,
        singleLine = true,
        isError = isError,
        label = { Text(label) },
        placeholder = { Text(stringResource(R.string.label_none)) },
        leadingIcon = {
            IconButton(
                onClick = { showPicker = true },
                modifier = Modifier.testTag("${fieldTestTag}_open")
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = stringResource(R.string.collection_date_select_cd, label)
                )
            }
        },
        trailingIcon = {
            if (selectedDate != null) {
                IconButton(
                    onClick = { onDateSelected(null) },
                    modifier = Modifier.testTag(clearTestTag)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.collection_date_clear_cd, label)
                    )
                }
            }
        },
        colors = vndbOutlinedTextFieldColors(),
        modifier = modifier
            .fillMaxWidth()
            .semantics { role = Role.Button }
            .clickable { showPicker = true }
            .testTag(fieldTestTag)
    )

    if (showPicker) {
        val initialMillis = selectedDate
            ?.atStartOfDay(ZoneOffset.UTC)
            ?.toInstant()
            ?.toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pickedMillis = pickerState.selectedDateMillis
                        val pickedDate = pickedMillis?.let {
                            Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        onDateSelected(pickedDate)
                        showPicker = false
                    },
                    modifier = Modifier.testTag("${fieldTestTag}_confirm")
                ) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DatePickerFieldPreview() {
    VndbTheme(themeId = ThemeId.AGL) {
        DatePickerField(
            label = "Started Date",
            selectedDate = LocalDate.of(2025, 5, 1),
            onDateSelected = {}
        )
    }
}
