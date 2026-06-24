package com.example.bairesessence.core.ui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.bairesessence.core.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerDialog(
    initialCheckin: String = "",
    initialCheckout: String = "",
    onConfirm: (checkin: String, checkout: String) -> Unit,
    onDismiss: () -> Unit,
    onClearDates: (() -> Unit)? = null
) {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = runCatching { fmt.parse(initialCheckin)?.time }.getOrNull(),
        initialSelectedEndDateMillis = runCatching { fmt.parse(initialCheckout)?.time }.getOrNull()
    )

    val canConfirm = state.selectedStartDateMillis != null && state.selectedEndDateMillis != null

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val start = state.selectedStartDateMillis
                    val end = state.selectedEndDateMillis
                    if (start != null && end != null) {
                        onConfirm(fmt.format(Date(start)), fmt.format(Date(end)))
                    }
                },
                enabled = canConfirm,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BEPrimary)
            ) {
                Text("Aplicar", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            if (onClearDates != null) {
                TextButton(onClick = onClearDates) {
                    Text("Quitar fechas", color = BETextMuted)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar", color = BETextSecond)
                }
            }
        },
        colors = DatePickerDefaults.colors(
            containerColor = BESurface,
            titleContentColor = BETextPrimary,
            headlineContentColor = BETextPrimary,
            selectedDayContainerColor = BEPrimary,
            todayDateBorderColor = BEPrimary,
            dayInSelectionRangeContainerColor = BEPrimary.copy(alpha = 0.15f)
        ),
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        DateRangePicker(
            state = state,
            modifier = Modifier.heightIn(max = 500.dp)
        )
    }
}
