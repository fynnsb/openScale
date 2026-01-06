package com.health.openscale.ui.screen.history

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.health.openscale.R
import com.health.openscale.core.data.EvaluationState
import com.health.openscale.core.data.InputFieldType
import com.health.openscale.core.data.Measurement
import com.health.openscale.core.data.MeasurementTypeKey
import com.health.openscale.core.data.Trend
import com.health.openscale.core.data.UnitType
import com.health.openscale.core.model.EnrichedMeasurement
import com.health.openscale.core.model.MeasurementWithValues
import com.health.openscale.core.model.UserEvaluationContext
import com.health.openscale.core.model.ValueWithDifference
import com.health.openscale.core.utils.ConverterUtils
import com.health.openscale.core.utils.LocaleUtils
import com.health.openscale.ui.components.LinearGauge
import com.health.openscale.ui.components.RoundMeasurementIcon
import com.health.openscale.ui.navigation.Routes
import com.health.openscale.ui.screen.dialog.DeleteConfirmationDialog
import com.health.openscale.ui.screen.settings.BluetoothViewModel
import com.health.openscale.ui.shared.SharedViewModel
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.health.openscale.core.data.MeasurementTypeIcon
import com.health.openscale.core.data.User
import com.health.openscale.core.data.UserGoals
import com.health.openscale.core.facade.SettingsPreferenceKeys
import com.health.openscale.ui.screen.components.provideFilterTopBarAction
import com.health.openscale.ui.screen.dialog.UserInputDialog
import com.health.openscale.ui.screen.overview.NoUserSelectedCard
import com.health.openscale.ui.screen.overview.determineBluetoothTopBarAction
import com.health.openscale.ui.screen.overview.smartScrollTo
import com.health.openscale.ui.shared.TopBarAction
import kotlinx.coroutines.delay


@Composable
fun HistoryScreen(
    navController: NavController,
    sharedViewModel: SharedViewModel,
) {

    val context = LocalContext.current
    sharedViewModel.setTopBarTitle(context.getString(R.string.route_title_history))
    sharedViewModel.setTopBarActions(listOfNotNull())


    val selectedUserId by sharedViewModel.selectedUserId.collectAsState()

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()


    val overviewState by sharedViewModel.overviewUiState.collectAsState()
    val hasData = (overviewState as? SharedViewModel.UiState.Success)?.data?.isNotEmpty() == true


    val userEvalContext by sharedViewModel.userEvaluationContext.collectAsState()

    var measurementToDelete by remember { mutableStateOf<EnrichedMeasurement?>(null) }

    measurementToDelete?.let { enrichedItem ->
        val weightValue = enrichedItem.valuesWithTrend.find {
            it.currentValue.type.key == MeasurementTypeKey.WEIGHT
        }
        val weightString = weightValue?.currentValue?.let {
            LocaleUtils.formatValueForDisplay(it.value.floatValue.toString(), it.type.unit)
        } ?: ""

        val formattedDate = remember(enrichedItem.measurementWithValues.measurement.timestamp) {
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault())
                .format(Date(enrichedItem.measurementWithValues.measurement.timestamp))
        }

        DeleteConfirmationDialog(
            onDismissRequest = { measurementToDelete = null },
            onConfirm = {
                scope.launch {
                    sharedViewModel.deleteMeasurement(enrichedItem.measurementWithValues.measurement)
                }
            },
            title = stringResource(R.string.dialog_title_delete_item),
            text = stringResource(R.string.dialog_message_delete_item, formattedDate, weightString)
        )
    }

    when {
        // Case 1: Display a global loading indicator.
        // This remains true as long as the ViewModel's overviewUiState is UiState.Loading.
        overviewState is SharedViewModel.UiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(), // Occupies the entire available space
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // Case 2: User restoration is complete (overviewState is not Loading),
        //         and no user is selected.
        selectedUserId == null && overviewState !is SharedViewModel.UiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize() // Occupies the entire available space
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                NoUserSelectedCard(navController = navController)
            }
        }

        // Case 3: User restoration is complete (overviewState is not Loading),
        //         and a user IS selected.
        selectedUserId != null && overviewState !is SharedViewModel.UiState.Loading -> {
            Column(modifier = Modifier.fillMaxSize()) {
                when (val state = overviewState) {
                    is SharedViewModel.UiState.Success -> {
                        val items = state.data
                        if (items.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f) // Takes remaining space in the Column
                                    .fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                NoMeasurementsCard(
                                    navController = navController,
                                    selectedUserId = selectedUserId // Not null here
                                )
                            }
                        } else {
                            val topId = items.firstOrNull()?.measurementWithValues?.measurement?.id
                            LaunchedEffect(topId, items.size) { // items.size added as a key
                                if (topId != null && !listState.isScrollInProgress) {
                                    delay(60)
                                    listState.smartScrollTo(0)
                                }
                            }
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(
                                    items = items,
                                    key = { _, item -> item.measurementWithValues.measurement.id }
                                ) { _, enrichedItem ->
                                    MeasurementCard(
                                        sharedViewModel = sharedViewModel,
                                        measurementWithValues = enrichedItem.measurementWithValues,
                                        processedValuesForDisplay = enrichedItem.valuesWithTrend,
                                        userEvaluationContext = userEvalContext, // Ensure this is available
                                        onClick = {
                                        },
                                        onEdit = {
                                            navController.navigate(
                                                Routes.measurementDetail(
                                                    enrichedItem.measurementWithValues.measurement.id,
                                                    selectedUserId!! // Not null here
                                                )
                                            )
                                        },
                                        onDelete = {
                                            measurementToDelete = enrichedItem
                                        }
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        // Fallback for any unhandled state combination.
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) { // Occupies the entire available space
                            Text("Unexpected state")
                        }
                    }


                }
            }
        }
    }
}

    /**
     * A Composable card displayed when a user is selected but has no measurements recorded yet.
     * It prompts the user to add their first measurement.
     *
     * @param navController The [NavController] for navigating to the measurement creation screen.
     * @param selectedUserId The ID of the currently selected user, to pass to the measurement creation screen.
     */
    @Composable
    fun NoMeasurementsCard(navController: NavController, selectedUserId: Int?) {
        Box(
            modifier = Modifier
                .fillMaxSize() // Important: To occupy the space assigned by Box(weight(1f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(0.9f),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Assessment, // Icon suggesting measurement/stats
                        contentDescription = null, // Decorative icon
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = stringResource(R.string.no_measurements_title),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.no_measurements_message),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilledTonalButton( // A less prominent button style
                        onClick = {
                            if (selectedUserId != null) {
                                // Navigate to measurement detail screen for new measurement
                                navController.navigate(
                                    Routes.measurementDetail(
                                        measurementId = null,
                                        userId = selectedUserId
                                    )
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(0.8f),
                        enabled = selectedUserId != null // Button is enabled only if a user is selected
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = null, // Decorative icon within button
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                        Text(stringResource(R.string.action_add_measurement))
                    }
                }
            }
        }
    }


    /**
     * A Composable card that displays a single measurement entry, including its date,
     * pinned values, and an expandable section for non-pinned values.
     * Provides actions to edit or delete the measurement.
     *
     * @param measurementWithValues The [MeasurementWithValues] object containing the measurement data and its associated values.
     * @param processedValuesForDisplay A list of [ValueWithDifference] objects, derived from the measurement,
     *                                  including trend information and formatted for display.
     * @param onClick Callback function triggered when the measurement card is selected.
     * @param onEdit Callback function triggered when the edit action is selected.
     * @param onDelete Callback function triggered when the delete action is selected.
     */
    @Composable
    fun MeasurementCard(
        sharedViewModel: SharedViewModel,
        measurementWithValues: MeasurementWithValues,
        processedValuesForDisplay: List<ValueWithDifference>,
        userEvaluationContext: UserEvaluationContext?,
        onClick: () -> Unit,
        onEdit: () -> Unit,
        onDelete: () -> Unit,
        isHighlighted: Boolean = false
    ) {
        val surfaceAtElevation = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        val isLightSurface = surfaceAtElevation.luminance() > 0.5f
        val tintAlpha = if (isLightSurface) 0.16f else 0.12f
        val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = tintAlpha)
            .compositeOver(surfaceAtElevation)

        val highlightBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)

        val measuredAtMillis = measurementWithValues.measurement.timestamp
        val expandedTypeIds = remember { mutableStateMapOf<Int, Boolean>() }

        val dateFormatted =
            remember(measurementWithValues.measurement.timestamp, Locale.getDefault()) {
                val timestamp = measurementWithValues.measurement.timestamp
                val currentLocale = Locale.getDefault()
                val dateFormatter = DateFormat.getDateInstance(DateFormat.MEDIUM, currentLocale)
                val timeFormatter = DateFormat.getTimeInstance(DateFormat.SHORT, currentLocale)
                "${dateFormatter.format(Date(timestamp))} ${timeFormatter.format(Date(timestamp))}"
            }

        var isExpanded by rememberSaveable { mutableStateOf(false) }

        // Separate values into pinned and non-pinned lists for distinct display logic
        val pinnedValues = remember(processedValuesForDisplay) {
            processedValuesForDisplay.filter { it.currentValue.type.isPinned && it.currentValue.type.isEnabled }
        }
        val nonPinnedValues = remember(processedValuesForDisplay) {
            processedValuesForDisplay.filter { !it.currentValue.type.isPinned && it.currentValue.type.isEnabled }
        }
        // All active (enabled) values to check if any data should be displayed
        val allActiveProcessedValues = remember(processedValuesForDisplay) {
            processedValuesForDisplay.filter { it.currentValue.type.isEnabled }
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            border = if (isHighlighted) highlightBorder else null,
            colors = if (isHighlighted) {
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            } else {
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            },
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                // Header row: Date and action buttons (Edit, Delete, Expand/Collapse)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp)
                ) {
                    Text(
                        text = dateFormatted,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f) // Date takes available space
                    )
                    val iconButtonSize = 36.dp // Standard size for action icons
                    val actionIconColor =
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)

                    IconButton(onClick = onEdit, modifier = Modifier.size(iconButtonSize)) {
                        Icon(
                            Icons.Rounded.Edit,
                            contentDescription = stringResource(
                                R.string.action_edit_measurement_desc,
                                dateFormatted
                            ),
                            tint = actionIconColor
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(iconButtonSize)) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = stringResource(
                                R.string.action_delete_measurement_desc,
                                dateFormatted
                            ),
                            tint = actionIconColor
                        )
                    }

                    // Conditional expand/collapse icon button for non-pinned values,
                    // only shown if there are non-pinned values and no pinned values (to avoid duplicate expand button logic)
                    if (nonPinnedValues.isNotEmpty() && pinnedValues.isEmpty()) {
                        IconButton(
                            onClick = { isExpanded = !isExpanded },
                            modifier = Modifier.size(iconButtonSize)
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = stringResource(if (isExpanded) R.string.action_show_less_desc else R.string.action_show_more_desc)
                            )
                        }
                    }
                }

                // Section for pinned measurement values (always visible if present)
                Column(
                    modifier = Modifier.padding(
                        start = 16.dp, end = 16.dp,
                        top = if (pinnedValues.isNotEmpty()) 8.dp else 0.dp, // Add top padding only if there are pinned values
                        bottom = 0.dp // Bottom padding handled by AnimatedVisibility or Spacer later
                    )
                ) {
                    if (pinnedValues.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                            pinnedValues.forEach { valueWithTrend ->
                                MeasurementRowExpandable(
                                    sharedViewModel = sharedViewModel,
                                    valueWithTrend = valueWithTrend,
                                    userEvaluationContext = userEvaluationContext,
                                    measuredAtMillis = measuredAtMillis,
                                    expandedTypeIds = expandedTypeIds
                                )
                            }
                        }
                    }
                }


                // Animated section for non-pinned measurement values (collapsible)
                if (nonPinnedValues.isNotEmpty()) {
                    AnimatedVisibility(visible = isExpanded || pinnedValues.isEmpty()) { // Also visible if no pinned values and not expanded (default state)
                        Column(
                            modifier = Modifier.padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = 0.dp,
                                bottom = 0.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            nonPinnedValues.forEach { valueWithTrend ->
                                MeasurementRowExpandable(
                                    sharedViewModel = sharedViewModel,
                                    valueWithTrend = valueWithTrend,
                                    userEvaluationContext = userEvaluationContext,
                                    measuredAtMillis = measuredAtMillis,
                                    expandedTypeIds = expandedTypeIds
                                )
                            }
                        }
                    }
                }


                // Footer: Expand/Collapse TextButton (only if there are non-pinned values and also pinned values,
                // or if there are non-pinned values and it's not the default expanded state for only non-pinned).
                if (nonPinnedValues.isNotEmpty() && (pinnedValues.isNotEmpty() || !isExpanded)) {
                    // Show divider if the expandable section is visible or if pinned items are present (button will always be there)
                    if (isExpanded || pinnedValues.isNotEmpty()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(
                                top = if (isExpanded && nonPinnedValues.isNotEmpty()) 4.dp else if (pinnedValues.isNotEmpty()) 8.dp else 0.dp,
                                bottom = 0.dp
                            )
                        )
                    }

                    TextButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp), // Consistent height for the button
                        shape = MaterialTheme.shapes.extraSmall // Less rounded corners for a subtle look
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            tint = MaterialTheme.colorScheme.secondary, // Use secondary color for emphasis
                            contentDescription = stringResource(if (isExpanded) R.string.action_show_less_desc else R.string.action_show_more_desc)
                        )
                    }
                }

                // Message if no active measurement values are present for this entry
                if (allActiveProcessedValues.isEmpty()) {
                    Text(
                        stringResource(R.string.no_active_values_for_measurement),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                // Add padding at the end of the card if only pinned values are shown and no footer (expand/collapse button) is present
                if (pinnedValues.isNotEmpty() && nonPinnedValues.isEmpty() && allActiveProcessedValues.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    /**
     * Displays one measurement row: icon + name + (optional) trend on the left,
     * value and an evaluation symbol on the right.
     *
     * Symbol rules:
     *  - ▲ / ▼ / ●: based on the evaluation state (HIGH / LOW / NORMAL/UNDEFINED)
     *  - ! (error color): shown if there is no matching age band at measurement time
     *    OR if a percentage value is outside a plausible range (0–100%).
     *
     * Note:
     *  - Non-numeric types (TEXT/DATE/TIME) are not evaluated (show ● if not flagged).
     */
    @Composable
    fun MeasurementValueRow(
        sharedViewModel: SharedViewModel,
        valueWithTrend: ValueWithDifference,
        userEvaluationContext: UserEvaluationContext?,
        measuredAtMillis: Long
    ) {
        val type = valueWithTrend.currentValue.type
        val originalValue = valueWithTrend.currentValue.value
        val difference = valueWithTrend.difference
        val trend = valueWithTrend.trend
        val unitName = type.unit.displayName

        // Localized display value for each input type
        val displayValue = when (type.inputType) {
            InputFieldType.FLOAT -> originalValue.floatValue
                ?.let { LocaleUtils.formatValueForDisplay(it.toString(), type.unit) }

            InputFieldType.INT -> originalValue.intValue
                ?.let { LocaleUtils.formatValueForDisplay(it.toString(), type.unit) }

            InputFieldType.TEXT -> originalValue.textValue
            InputFieldType.DATE -> originalValue.dateValue?.let {
                DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()).format(Date(it))
            }

            InputFieldType.TIME -> originalValue.dateValue?.let {
                DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(Date(it))
            }

            InputFieldType.USER -> null
        } ?: "-"

        val context = LocalContext.current
        val iconMeasurementType = remember(type.icon) { type.icon }

        // Extract numeric value only for evaluable numeric types
        val numeric: Float? = when (type.inputType) {
            InputFieldType.FLOAT -> originalValue.floatValue
            InputFieldType.INT -> originalValue.intValue?.toFloat()
            else -> null
        }

        // Compute evaluation if possible
        val evalResult = remember(valueWithTrend, userEvaluationContext, measuredAtMillis) {
            if (userEvaluationContext != null && numeric != null) {
                sharedViewModel.evaluateMeasurement(
                    type = type,
                    value = numeric,
                    userEvaluationContext = userEvaluationContext,
                    measuredAtMillis = measuredAtMillis
                )
            } else null
        }

        // Flag 1: no matching age band (limits are negative)
        val noAgeBand: Boolean = evalResult?.let { it.lowLimit < 0f || it.highLimit < 0f } ?: false

        // Flag 2: percent outside a plausible range (0..100)
        val plausible = sharedViewModel.getPlausiblePercentRange(type.key)
        val outOfPlausibleRange =
            if (numeric == null) {
                false
            } else {
                plausible?.let { numeric < it.start || numeric > it.endInclusive }
                    ?: (unitName == "%" && (numeric < 0f || numeric > 100f)) // Fallback
            }

        val flagged = noAgeBand || outOfPlausibleRange

        // Base evaluation state (falls back to UNDEFINED when not evaluable)
        val evalState = evalResult?.state ?: EvaluationState.UNDEFINED

        // Symbol selection
        val evalSymbol = if (flagged) {
            "!"
        } else {
            when (evalState) {
                EvaluationState.LOW -> "▼"
                EvaluationState.NORMAL -> "●"
                EvaluationState.HIGH -> "▲"
                EvaluationState.UNDEFINED -> "●"
            }
        }

        // Symbol color: error for "!", otherwise mapped from eval state
        val evalColor = if (flagged) {
            MaterialTheme.colorScheme.error
        } else {
            evalState.toColor()
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: icon + labels
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundMeasurementIcon(
                    icon = iconMeasurementType.resource,
                    backgroundTint = Color(type.color).copy(alpha = 0.2f),
                    iconTint = Color(type.color)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = type.getDisplayName(context),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1
                    )

                    // Show trend only for numeric types with a difference
                    if (difference != null && trend != Trend.NOT_APPLICABLE) {
                        Spacer(modifier = Modifier.height(1.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val trendIconVector = when (trend) {
                                Trend.UP -> Icons.Rounded.ArrowUpward
                                Trend.DOWN -> Icons.Rounded.ArrowDownward
                                Trend.NONE -> null
                                else -> null
                            }
                            val subtle =
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            if (trendIconVector != null) {
                                Icon(
                                    imageVector = trendIconVector,
                                    contentDescription = trend.name,
                                    tint = subtle,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                            }
                            Text(
                                text = when (type.inputType) {
                                    InputFieldType.FLOAT, InputFieldType.INT ->
                                        LocaleUtils.formatValueForDisplay(
                                            value = difference.toString(),
                                            unit = type.unit,
                                            includeSign = (trend != Trend.NONE)
                                        )

                                    else -> ""
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = subtle
                            )
                        }
                    } else if (type.inputType == InputFieldType.FLOAT || type.inputType == InputFieldType.INT) {
                        // Keep vertical spacing consistent when no trend is shown
                        Spacer(modifier = Modifier.height((MaterialTheme.typography.bodySmall.fontSize.value + 2).dp))
                    }
                }
            }

            // Right side: value + evaluation symbol
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = displayValue,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.End
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = evalSymbol,
                    color = evalColor,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }


    /**
     * Small, prominent banner for evaluation problems (e.g., no age band or implausible value).
     *
     * @param message Localized message to display inside the banner.
     */
    @Composable
    private fun EvaluationErrorBanner(message: String) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }


    /**
     * One measurement row that can expand to show a gauge or an info banner.
     *
     * Behavior:
     * - If a normal evaluation is possible, the row expands to a LinearGauge.
     * - If no age band exists or the value is outside a plausible range, the row expands to an info banner.
     * - The clickable state of the row follows the above rules (only clickable when there is something meaningful to show).
     *
     * @param valueWithTrend The value and meta info for this row.
     * @param userEvaluationContext The context needed to evaluate the value (gender, age, etc.); can be null.
     * @param measuredAtMillis Timestamp of the measurement (used by the evaluator).
     * @param expandedTypeIds State map holding expand/collapse flags per measurement type id.
     * @param modifier Optional modifier for the container column.
     * @param gaugeHeightDp Height of the gauge when shown.
     */
    @Composable
    fun MeasurementRowExpandable(
        sharedViewModel: SharedViewModel,
        valueWithTrend: ValueWithDifference,
        userEvaluationContext: UserEvaluationContext?,
        measuredAtMillis: Long,
        expandedTypeIds: MutableMap<Int, Boolean>,
        modifier: Modifier = Modifier,
        gaugeHeightDp: Dp = 80.dp,
    ) {
        val type = valueWithTrend.currentValue.type

        // Extract numeric value for evaluation / plausibility checks
        val numeric: Float? = when (type.inputType) {
            InputFieldType.FLOAT -> valueWithTrend.currentValue.value.floatValue
            InputFieldType.INT -> valueWithTrend.currentValue.value.intValue?.toFloat()
            else -> null
        }

        // Run evaluation (or keep null when not possible)
        val evalResult = remember(valueWithTrend, userEvaluationContext, measuredAtMillis) {
            if (userEvaluationContext == null || numeric == null) {
                null
            } else {
                sharedViewModel.evaluateMeasurement(
                    type = type,
                    value = numeric,
                    userEvaluationContext = userEvaluationContext,
                    measuredAtMillis = measuredAtMillis
                )
            }
        }

        // Special cases:
        // 1) No age band available -> evaluator returns negative limits
        val noAgeBand = evalResult?.let { it.lowLimit < 0f || it.highLimit < 0f } ?: false

        // 2) Implausible value for percentage-based metrics
        val unitName = type.unit.displayName
        val plausible = sharedViewModel.getPlausiblePercentRange(type.key)
        val outOfPlausibleRange =
            if (numeric == null) {
                false
            } else {
                plausible?.let { numeric < it.start || numeric > it.endInclusive }
                    ?: (unitName == "%" && (numeric < 0f || numeric > 100f))
            }

        // Expand is allowed when:
        // - a normal evaluation exists (valid limits), OR
        // - we have one of the special cases (to show the info banner)
        val canExpand = (evalResult != null && !noAgeBand) || noAgeBand || outOfPlausibleRange

        Column(modifier) {
            // The main row – clickable only when `canExpand` is true.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = canExpand) {
                        val cur = expandedTypeIds[type.id] ?: false
                        expandedTypeIds[type.id] = !cur
                    }
            ) {
                // Uses your existing row (with ●/▲/▼ or ! logic inside).
                MeasurementValueRow(
                    sharedViewModel = sharedViewModel,
                    valueWithTrend = valueWithTrend,
                    userEvaluationContext = userEvaluationContext,
                    measuredAtMillis = measuredAtMillis
                )
            }

            val unit = type.unit.displayName

            // Expanded content:
            AnimatedVisibility(visible = canExpand && (expandedTypeIds[type.id] == true)) {
                when {
                    noAgeBand -> {
                        EvaluationErrorBanner(
                            message = stringResource(R.string.eval_no_age_band)
                        )
                    }

                    outOfPlausibleRange -> {
                        val plausible =
                            sharedViewModel.getPlausiblePercentRange(type.key) ?: (0f..100f)
                        EvaluationErrorBanner(
                            message = stringResource(
                                R.string.eval_out_of_plausible_range_percent,
                                plausible.start,
                                plausible.endInclusive
                            )
                        )
                    }
                    // Normal evaluation → show gauge
                    evalResult != null -> {

                        val (displayValue, displayLow, displayHigh) = remember(
                            evalResult,
                            type.unit
                        ) {
                            val targetUnit = type.unit
                            val baseUnit = when (type.key) {
                                MeasurementTypeKey.WEIGHT, MeasurementTypeKey.LBM -> UnitType.KG
                                MeasurementTypeKey.WAIST -> UnitType.CM
                                else -> UnitType.PERCENT
                            }

                            if (baseUnit != targetUnit) {
                                val convertedValue = ConverterUtils.convertFloatValueUnit(
                                    evalResult.value,
                                    baseUnit,
                                    targetUnit
                                )
                                val convertedLow =
                                    if (evalResult.lowLimit >= 0f) ConverterUtils.convertFloatValueUnit(
                                        evalResult.lowLimit,
                                        baseUnit,
                                        targetUnit
                                    ) else null
                                val convertedHigh = ConverterUtils.convertFloatValueUnit(
                                    evalResult.highLimit,
                                    baseUnit,
                                    targetUnit
                                )
                                Triple(convertedValue, convertedLow, convertedHigh)
                            } else {
                                Triple(
                                    evalResult.value,
                                    if (evalResult.lowLimit < 0f) null else evalResult.lowLimit,
                                    evalResult.highLimit
                                )
                            }
                        }

                        Column(
                            Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 2.dp)
                        ) {
                            LinearGauge(
                                value = displayValue,
                                lowLimit = displayLow,
                                highLimit = displayHigh,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(gaugeHeightDp),
                                labelProvider = { value ->
                                    LocaleUtils.formatValueForDisplay(value.toString(), type.unit)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
