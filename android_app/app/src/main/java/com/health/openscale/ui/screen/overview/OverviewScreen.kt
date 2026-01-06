/*
 * openScale
 * Copyright (C) 2025 olie.xdev <olie.xdeveloper@googlemail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.health.openscale.ui.screen.overview

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.automirrored.rounded.BluetoothSearching
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.health.openscale.R
import com.health.openscale.core.data.ConnectionStatus
import com.health.openscale.core.data.EvaluationState
import com.health.openscale.core.data.InputFieldType
import com.health.openscale.core.data.MeasurementTypeIcon
import com.health.openscale.core.data.MeasurementTypeKey
import com.health.openscale.core.data.Trend
import com.health.openscale.core.data.UnitType
import com.health.openscale.core.data.User
import com.health.openscale.core.data.UserGoals
import com.health.openscale.core.model.MeasurementWithValues
import com.health.openscale.core.facade.SettingsPreferenceKeys
import com.health.openscale.core.model.EnrichedMeasurement
import com.health.openscale.core.model.UserEvaluationContext
import com.health.openscale.core.model.ValueWithDifference
import com.health.openscale.core.utils.ConverterUtils
import com.health.openscale.core.utils.LocaleUtils
import com.health.openscale.core.utils.LogManager
import com.health.openscale.ui.components.LinearGauge
import com.health.openscale.ui.components.RoundMeasurementIcon
import com.health.openscale.ui.navigation.Routes
import com.health.openscale.ui.shared.SharedViewModel
import com.health.openscale.ui.screen.settings.BluetoothViewModel
import com.health.openscale.ui.screen.components.MeasurementChart
import com.health.openscale.ui.screen.components.UserGoalChip
import com.health.openscale.ui.screen.components.provideFilterTopBarAction
import com.health.openscale.ui.screen.dialog.DeleteConfirmationDialog
import com.health.openscale.ui.screen.dialog.UserGoalDialog
import com.health.openscale.ui.screen.dialog.UserInputDialog
import com.health.openscale.ui.screen.history.NoMeasurementsCard
import com.health.openscale.ui.screen.statistics.StatisticsScreen
import com.health.openscale.ui.shared.TopBarAction
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.collections.filter
import kotlin.collections.find
import kotlin.collections.firstOrNull
import kotlin.collections.isNotEmpty
import kotlin.let
import kotlin.math.abs

/**
 * Determines the appropriate top bar action based on the Bluetooth connection status.
 * Uses the [SharedViewModel] to display Snackbars for user feedback.
 *
 * @param context The application context.
 * @param savedAddr The address of the currently saved Bluetooth scale, if any.
 * @param connStatusEnum The current connection status to the scale.
 * @param connectedDevice The address of the currently connected device, if any.
 * @param currentNavController The NavController for navigation actions.
 * @param bluetoothViewModel The ViewModel for controlling Bluetooth actions.
 * @param sharedViewModel The SharedViewModel for triggering global Snackbars.
 * @param currentDeviceName The name of the saved scale for more user-friendly messages.
 * @return A [SharedViewModel.TopBarAction] instance or null if no specific action is required.
 */
fun determineBluetoothTopBarAction(
    context : Context,
    savedAddr: String?,
    connStatusEnum: ConnectionStatus,
    connectedDevice: String?,
    currentNavController: NavController,
    bluetoothViewModel: BluetoothViewModel,
    sharedViewModel: SharedViewModel,
    currentDeviceName: String?,
    permissionsLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>>,
    enableBluetoothLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    currentUserForAssistedWeighing: User?,
    onShowReferenceDialog: (user: User) -> Unit
): TopBarAction? {
    val TAG = "BluetoothTopBar"
    val deviceNameForMessage = currentDeviceName ?: context.getString(R.string.fallback_device_name_saved_scale)

    // Busy while connecting/disconnecting to the currently saved device
    val isBusy = savedAddr != null &&
            (connStatusEnum == ConnectionStatus.CONNECTING || connStatusEnum == ConnectionStatus.DISCONNECTING) &&
            (connectedDevice == savedAddr || connStatusEnum == ConnectionStatus.CONNECTING ||
                    (connStatusEnum == ConnectionStatus.DISCONNECTING && connectedDevice == savedAddr))

    // Helper to request enabling Bluetooth (actual connect is done in onActivityResult when OK)
    val requestEnableBluetooth = {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        enableBluetoothLauncher.launch(intent)
    }

    // Helper to request runtime permissions (actual connect is done in onResult when granted)
    val requestBtPermissions = {
        permissionsLauncher.launch(
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        )
    }

    return when {
        // 1) Show non-interactive feedback while a user-initiated operation is ongoing
        isBusy -> TopBarAction(
            icon = Icons.AutoMirrored.Rounded.BluetoothSearching,
            contentDescription = context.getString(R.string.bluetooth_action_connecting_disconnecting_desc),
            onClick = {
                sharedViewModel.showSnackbar(
                    message = context.getString(
                        when (connStatusEnum) {
                            ConnectionStatus.CONNECTING    -> R.string.snackbar_bluetooth_connecting_to
                            ConnectionStatus.DISCONNECTING -> R.string.snackbar_bluetooth_disconnecting_from
                            else                           -> R.string.snackbar_bluetooth_processing_with
                        },
                        deviceNameForMessage
                    ),
                    duration = SnackbarDuration.Short
                )
            }
        )

        // 2) No saved device → navigate to Bluetooth settings
        savedAddr == null -> TopBarAction(
            icon = Icons.Rounded.Bluetooth,
            contentDescription = context.getString(R.string.bluetooth_action_no_scale_saved_desc),
            onClick = {
                sharedViewModel.setPendingReferenceUserForBle(null)
                sharedViewModel.showSnackbar(
                    message = context.getString(R.string.snackbar_bluetooth_no_scale_saved),
                    duration = SnackbarDuration.Short
                )
                currentNavController.navigate(Routes.BLUETOOTH_SETTINGS)
            }
        )

        // 3) Connected → offer disconnect
        savedAddr == connectedDevice && connStatusEnum == ConnectionStatus.CONNECTED -> TopBarAction(
            icon = Icons.Rounded.BluetoothConnected,
            contentDescription = context.getString(R.string.bluetooth_action_disconnect_desc, deviceNameForMessage),
            onClick = {
                sharedViewModel.setPendingReferenceUserForBle(null)
                bluetoothViewModel.disconnectDevice()
                sharedViewModel.showSnackbar(
                    message = context.getString(R.string.snackbar_bluetooth_disconnecting_from, deviceNameForMessage),
                    duration = SnackbarDuration.Short
                )
            }
        )

        // 4) Disconnected / Idle / None / Failed → guarded connect (request first, connect later via callbacks)
        savedAddr != null && (
                connStatusEnum == ConnectionStatus.DISCONNECTED ||
                        connStatusEnum == ConnectionStatus.IDLE ||
                        connStatusEnum == ConnectionStatus.NONE ||
                        connStatusEnum == ConnectionStatus.FAILED
                ) -> TopBarAction(
            icon = Icons.Rounded.BluetoothDisabled,
            contentDescription = context.getString(R.string.bluetooth_action_connect_to_desc, deviceNameForMessage),
            onClick = onClick@{
                val hasPermissions =
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

                if (!hasPermissions) {
                    requestBtPermissions()
                    return@onClick
                }
                if (!bluetoothViewModel.isBluetoothEnabled()) {
                    requestEnableBluetooth()
                    return@onClick
                }

                if (currentUserForAssistedWeighing != null && currentUserForAssistedWeighing.useAssistedWeighing) {
                    sharedViewModel.setPendingReferenceUserForBle(null)
                    onShowReferenceDialog(currentUserForAssistedWeighing)
                } else {
                    sharedViewModel.setPendingReferenceUserForBle(null)

                    sharedViewModel.showSnackbar(
                        message = context.getString(R.string.snackbar_bluetooth_attempting_connection, deviceNameForMessage),
                        duration = SnackbarDuration.Short
                    )
                    LogManager.d(TAG, "User clicked bluetooth icon connect → trying to connect to saved device $deviceNameForMessage")

                    bluetoothViewModel.connectToSavedDevice()
                }
            }
        )

        // 5) Fallback
        else -> TopBarAction(
            icon = Icons.Rounded.Bluetooth,
            contentDescription = context.getString(R.string.bluetooth_action_check_settings_desc),
            onClick = {
                sharedViewModel.setPendingReferenceUserForBle(null)
                sharedViewModel.showSnackbar(
                    message = context.getString(R.string.snackbar_bluetooth_check_settings),
                    duration = SnackbarDuration.Short
                )
                currentNavController.navigate(Routes.BLUETOOTH_SETTINGS)
            }
        )
    }
}

/**
 * The main screen for displaying an overview of measurements, user status, and Bluetooth controls.
 * It allows users to view their measurement history, add new measurements, and manage Bluetooth scale connections.
 *
 * @param navController The [NavController] used for navigating between screens.
 * @param sharedViewModel The [SharedViewModel] providing access to shared data like user selection,
 *                        measurements, and top bar configuration.
 * @param bluetoothViewModel The [BluetoothViewModel] for managing Bluetooth state and actions.
 */
@Composable
fun OverviewScreen(
    navController: NavController,
    sharedViewModel: SharedViewModel,
    bluetoothViewModel: BluetoothViewModel
) {
    val selectedUserId by sharedViewModel.selectedUserId.collectAsState()
    val context = LocalContext.current // Used for Toasts and string resources

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Time filter action for the top bar, specific to this screen's context
    val timeFilterAction = provideFilterTopBarAction(
        sharedViewModel = sharedViewModel,
        screenContextName = SettingsPreferenceKeys.OVERVIEW_SCREEN_CONTEXT
    )
    val overviewState by sharedViewModel.overviewUiState.collectAsState()
    val hasData = (overviewState as? SharedViewModel.UiState.Success)?.data?.isNotEmpty() == true

    // --- Chart selection logic reverted to local state management ---
    val allMeasurementTypes by sharedViewModel.measurementTypes.collectAsState()
    val goalDialogContextData by sharedViewModel.userGoalDialogContext.collectAsState()
    val userGoals by if (selectedUserId != null && selectedUserId != 0) {
        sharedViewModel.getAllGoalsForUser(selectedUserId!!).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList<UserGoals>()) }
    }
    val isGoalsSectionExpanded by sharedViewModel.myGoalsExpandedOverview.collectAsState(
        initial = true
    )


    val userEvalContext by sharedViewModel.userEvaluationContext.collectAsState()
    var showReferenceDialogForUser by remember { mutableStateOf<User?>(null) }
    val allUsers by sharedViewModel.allUsers.collectAsState(initial = emptyList())
    val currentSelectedUser by sharedViewModel.selectedUser.collectAsState()
    var currentSelectedMeasurementId by rememberSaveable { mutableStateOf<Int?>(null) }

    val goalReferenceMeasurement: MeasurementWithValues? = remember(currentSelectedMeasurementId, overviewState) {
        val currentData = if (overviewState is SharedViewModel.UiState.Success) {
            (overviewState as SharedViewModel.UiState.Success<List<EnrichedMeasurement>>).data
        } else {
            emptyList()
        }
        if (currentSelectedMeasurementId != null && currentData.isNotEmpty()) {
            currentData.find { it.measurementWithValues.measurement.id == currentSelectedMeasurementId }
                ?.measurementWithValues
        } else if (currentData.isNotEmpty()) {
            currentData.firstOrNull()?.measurementWithValues
        } else {
            null
        }
    }



    // --- End of reverted chart selection logic ---

    val savedDevice by bluetoothViewModel.savedDevice.collectAsState()
    val connectionStatus by bluetoothViewModel.connectionStatus.collectAsState()
    val connectedDeviceAddr by bluetoothViewModel.connectedDeviceAddress.collectAsState()

    val savedDeviceNameString = savedDevice?.name.orEmpty()
    val savedDeviceAddress    = savedDevice?.address

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val hasPermissions =
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

            if (hasPermissions) {
                bluetoothViewModel.connectToSavedDevice()
            } else {
                sharedViewModel.showSnackbar(
                    message = context.getString(R.string.bluetooth_enabled_permissions_missing),
                    duration = SnackbarDuration.Long
                )
            }
        } else {
            sharedViewModel.showSnackbar(
                message = context.getString(R.string.bluetooth_permissions_required_for_scan),
                duration = SnackbarDuration.Long
            )
        }
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val allGranted = result.values.all { it }
        if (allGranted) {
            if (bluetoothViewModel.isBluetoothEnabled()) {
                bluetoothViewModel.connectToSavedDevice()
            } else {
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                enableBluetoothLauncher.launch(intent)
            }
        } else {
            sharedViewModel.showSnackbar(
                messageResId = R.string.bluetooth_permissions_required_for_scan
            )
        }
    }

    showReferenceDialogForUser?.let { currentTargetUser ->
        val availableReferenceUsers = allUsers.filter { user ->
            user.id != currentTargetUser.id && !user.useAssistedWeighing
        }
        if (availableReferenceUsers.isEmpty()) {
            LaunchedEffect(currentTargetUser) {
                sharedViewModel.showSnackbar(messageResId = R.string.error_no_reference_users_available)
                showReferenceDialogForUser = null
                sharedViewModel.setPendingReferenceUserForBle(null)
            }
        } else {
            UserInputDialog(
                title = stringResource(R.string.dialog_title_select_reference_user_for, currentTargetUser.name),
                users = availableReferenceUsers,
                initialSelectedId = availableReferenceUsers.firstOrNull()?.id,
                measurementIcon = MeasurementTypeIcon.IC_USER,
                iconBackgroundColor = MaterialTheme.colorScheme.primaryContainer,
                onDismiss = {
                    showReferenceDialogForUser = null
                    sharedViewModel.setPendingReferenceUserForBle(null)
                },
                onConfirm = { selectedUserId ->
                    showReferenceDialogForUser = null
                    val selectedReferenceUser = availableReferenceUsers.find { it.id == selectedUserId }
                    if (selectedReferenceUser != null) {
                        sharedViewModel.setPendingReferenceUserForBle(selectedReferenceUser)

                        val deviceNameForMessage = savedDeviceNameString
                        sharedViewModel.showSnackbar(
                            message = context.getString(R.string.snackbar_bluetooth_attempting_connection, deviceNameForMessage),
                            duration = SnackbarDuration.Short
                        )
                        bluetoothViewModel.connectToSavedDevice()
                    } else {
                        sharedViewModel.setPendingReferenceUserForBle(null)
                    }
                }
            )
        }
    }

    // Determine the Bluetooth action for the top bar
    val bluetoothTopBarAction = determineBluetoothTopBarAction(
        context = context,
        savedAddr = savedDeviceAddress,
        connStatusEnum = connectionStatus,
        connectedDevice = connectedDeviceAddr,
        currentNavController = navController,
        bluetoothViewModel = bluetoothViewModel,
        sharedViewModel = sharedViewModel,
        currentDeviceName = savedDeviceNameString,
        permissionsLauncher = permissionsLauncher,
        enableBluetoothLauncher = enableBluetoothLauncher,
        currentUserForAssistedWeighing = currentSelectedUser,
        onShowReferenceDialog = { user ->
            showReferenceDialogForUser = user
        }
    )

    val lifecycleOwner = LocalLifecycleOwner.current

    // DisposableEffect to configure the top bar based on the current state
    DisposableEffect(
        lifecycleOwner,
        selectedUserId,
        hasData,
        bluetoothTopBarAction,
        timeFilterAction,
        savedDeviceAddress,
        connectionStatus,
        connectedDeviceAddr
    ) {
        fun updateTopBar() {
            sharedViewModel.setTopBarTitle(context.getString(R.string.route_title_overview))
            val actions = mutableListOf<TopBarAction>()

            // 0. Add Bluetooth action (if determined) at the beginning
            bluetoothTopBarAction?.let { btAction ->
                actions.add(btAction)
            }

            // 1. Add "Add Measurement" icon
            actions.add(
                TopBarAction(
                    icon = Icons.Rounded.Add,
                    contentDescription = context.getString(R.string.action_add_measurement_desc),
                    onClick = {
                        if (selectedUserId != null) {
                            navController.navigate(Routes.measurementDetail(measurementId = null, userId = selectedUserId!!))
                        } else {
                            Toast.makeText(context, context.getString(R.string.toast_select_user_first), Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            )

            timeFilterAction?.let { actions.add(it) }
            sharedViewModel.setTopBarActions(actions)
        }

        updateTopBar()

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                updateTopBar()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    when {
        // Case 1: Display a global loading indicator.
        // This remains true as long as the ViewModel's overviewUiState is UiState.Loading.
        overviewState is SharedViewModel.UiState.Loading ->  {
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


                            // Goals Section
                            if (userGoals.isNotEmpty()) {
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val newIsGoalsSectionExpanded =
                                                    !isGoalsSectionExpanded
                                                scope.launch {
                                                    sharedViewModel.setMyGoalsExpandedOverview(
                                                        newIsGoalsSectionExpanded
                                                    )
                                                }
                                            }
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(
                                            modifier = Modifier.weight(1f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = stringResource(R.string.my_goals_label),
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                if (!isGoalsSectionExpanded && userGoals.isNotEmpty()) {
                                                    Text(
                                                        text = "(${userGoals.size})",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                        Icon(
                                            imageVector = if (isGoalsSectionExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                            contentDescription = if (isGoalsSectionExpanded) stringResource(
                                                R.string.action_show_less_desc
                                            ) else stringResource(R.string.action_show_more_desc),
                                        )
                                    }

                                    AnimatedVisibility(visible = isGoalsSectionExpanded) {
                                        Column {
                                            if (userGoals.isNotEmpty()) {
                                                LazyRow(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    contentPadding = PaddingValues(
                                                        horizontal = 16.dp,
                                                    ),
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    items(
                                                        userGoals,
                                                        key = { goal -> "${goal.userId}_${goal.measurementTypeId}" }) { goal ->
                                                        if (goal.userId == currentSelectedUser!!.id) {
                                                            val measurementType =
                                                                allMeasurementTypes.find { it.id == goal.measurementTypeId }
                                                            if (measurementType != null) {
                                                                UserGoalChip(
                                                                    userGoal = goal,
                                                                    measurementType = measurementType,
                                                                    referenceMeasurement = goalReferenceMeasurement,
                                                                    onClick = {
                                                                        if (currentSelectedUser!!.id != 0 && goal.userId == currentSelectedUser!!.id) {
                                                                            sharedViewModel.showUserGoalDialogWithContext(
                                                                                type = measurementType,
                                                                                existingGoal = goal
                                                                            )
                                                                        }
                                                                    }
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            if (goalReferenceMeasurement?.measurement?.timestamp != null) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(
                                                            top = 4.dp,
                                                            bottom = 4.dp,
                                                            end = 16.dp
                                                        ),
                                                    horizontalArrangement = Arrangement.End,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val shortDateTimeFormatter = remember {
                                                        DateFormat.getDateTimeInstance(
                                                            DateFormat.MEDIUM,
                                                            DateFormat.SHORT,
                                                            Locale.getDefault()
                                                        )
                                                    }
                                                    Icon(
                                                        imageVector = Icons.Rounded.Link,
                                                        contentDescription = stringResource(R.string.my_goals_label),
                                                        modifier = Modifier.size(14.dp),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(
                                                        text = shortDateTimeFormatter.format(
                                                            Date(
                                                                goalReferenceMeasurement.measurement.timestamp
                                                            )
                                                        ),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    HorizontalDivider()
                                }
                            }

                            StatisticsScreen(sharedViewModel)

                        }
                    }
                    is SharedViewModel.UiState.Error -> {
                        Box(
                            modifier = Modifier
                                .weight(1f) // Takes remaining space in the Column
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(state.message ?: stringResource(R.string.error_loading_data))
                        }
                    }
                    SharedViewModel.UiState.Loading -> {
                        // This case should ideally not be reached if the outer 'when' condition is met,
                        // or only very briefly if data for a specific user is loading.
                        Box(
                            modifier = Modifier
                                .weight(1f) // Takes remaining space in the Column
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
        else -> {
            // Fallback for any unhandled state combination.
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { // Occupies the entire available space
                Text("Unexpected state")
            }
        }
    }

    if (goalDialogContextData.showDialog) {
        val dialogContext = goalDialogContextData

        val userIdForDialogDisplay = currentSelectedUser?.id

        if (dialogContext.typeForDialog == null || userIdForDialogDisplay == null || userIdForDialogDisplay == 0) {
            LaunchedEffect(goalDialogContextData.showDialog) {
                sharedViewModel.dismissUserGoalDialogWithContext()
            }
        } else {
            UserGoalDialog(
                navController = navController,
                existingUserGoal = dialogContext.existingGoalForDialog,
                allMeasurementTypes = allMeasurementTypes,
                allGoalsOfCurrentUser = userGoals,
                onDismiss = {
                    sharedViewModel.dismissUserGoalDialogWithContext()
                },
                onConfirm = { measurementTypeId, goalValueString, goalTargetDate ->
                    val finalGoalValueFloat = goalValueString.replace(',', '.').toFloatOrNull()
                    if (finalGoalValueFloat != null) {
                        val goalToProcess = UserGoals(
                            userId = userIdForDialogDisplay,
                            measurementTypeId = measurementTypeId,
                            goalValue = finalGoalValueFloat,
                            goalTargetDate = goalTargetDate
                        )
                        if (dialogContext.existingGoalForDialog != null) {
                            sharedViewModel.updateUserGoal(goalToProcess)
                        } else {
                            sharedViewModel.insertUserGoal(goalToProcess)
                        }
                    } else if (goalValueString.isBlank() && dialogContext.existingGoalForDialog != null) {
                        return@UserGoalDialog
                    } else if (goalValueString.isBlank()) {
                        Toast.makeText(
                            context,
                            R.string.toast_goal_value_cannot_be_empty,
                            Toast.LENGTH_SHORT
                        ).show()
                        return@UserGoalDialog
                    } else {
                        val typeName = allMeasurementTypes.find { it.id == measurementTypeId }
                            ?.getDisplayName(context) ?: "Value"
                        Toast.makeText(
                            context,
                            context.getString(R.string.toast_invalid_number_format_short, typeName),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@UserGoalDialog
                    }
                    sharedViewModel.dismissUserGoalDialogWithContext()
                },
                onDelete = { _, measurementTypeIdToDelete ->
                    sharedViewModel.deleteUserGoal(
                        userIdForDialogDisplay,
                        measurementTypeIdToDelete
                    )
                    Toast.makeText(context, R.string.toast_goal_deleted, Toast.LENGTH_SHORT).show()
                    sharedViewModel.dismissUserGoalDialogWithContext()
                })
        }
    }
}

suspend fun LazyListState.smartScrollTo(index: Int) {
    val dist = abs(firstVisibleItemIndex - index)
    if (dist > 20) scrollToItem(index) else animateScrollToItem(index)
}

/**
 * A Composable card displayed when no user is currently selected/active.
 * It prompts the user to add or select a user.
 *
 * @param navController The [NavController] for navigating to the user creation/selection screen.
 */
@Composable
fun NoUserSelectedCard(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f), // Take 90% of width
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
                    imageVector = Icons.Rounded.PersonSearch,
                    contentDescription = null, // Decorative icon
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.no_user_selected_title),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.no_user_selected_message),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = {
                        // Navigate to user detail screen with -1 to indicate new user creation
                        navController.navigate(Routes.userDetail(-1))
                    },
                    modifier = Modifier.fillMaxWidth(0.8f) // Take 80% of card width
                ) {
                    Icon(
                        Icons.Rounded.PersonAdd,
                        contentDescription = null, // Decorative icon within button
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.action_add_user))
                }
            }
        }
    }
}

