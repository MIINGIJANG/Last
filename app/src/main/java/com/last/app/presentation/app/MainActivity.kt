package com.last.app.presentation.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.last.app.LastApplication
import com.last.app.R
import com.last.app.data.repository.LastRepository
import com.last.app.infrastructure.ConnectionMonitorService
import com.last.app.infrastructure.platform.PermissionHelper
import com.last.app.presentation.permission.ConsentStep
import com.last.app.presentation.permission.PermissionConsentGate
import com.last.app.presentation.settings.SettingsPermissionCallbacks
import com.last.app.presentation.permission.nextConsentStep
import com.last.app.presentation.theme.LastBackground
import com.last.app.presentation.theme.LastTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var repository: LastRepository
    private lateinit var settingsPermissionCallbacks: SettingsPermissionCallbacks
    private var consentStep by mutableStateOf(ConsentStep.INTRO)
    private var permissionsReady by mutableStateOf(false)

    private var pendingSettingsLocationCallback: (() -> Unit)? = null
    private var pendingSettingsBluetoothCallback: (() -> Unit)? = null

    private val locationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { advanceAfter(ConsentStep.LOCATION) }

    private val backgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { advanceAfter(ConsentStep.BACKGROUND_LOCATION) }

    private val bluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { advanceAfter(ConsentStep.BLUETOOTH) }

    private val settingsForegroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val granted = results.isEmpty() || results.values.all { it }
        if (!granted) {
            showPermissionDeniedToast()
            pendingSettingsLocationCallback = null
            return@registerForActivityResult
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && needsBackgroundLocation()) {
            settingsBackgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            completeSettingsLocationRequest()
        }
    }

    private val settingsBackgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        completeSettingsLocationRequest()
    }

    private val settingsBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val granted = results.isEmpty() || results.values.all { it }
        if (granted) {
            pendingSettingsBluetoothCallback?.invoke()
        } else {
            showPermissionDeniedToast()
        }
        pendingSettingsBluetoothCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
        }
        @Suppress("DEPRECATION")
        window.statusBarColor = LastBackground.toArgb()

        repository = LastApplication.repositoryOf(this)
        settingsPermissionCallbacks = createSettingsPermissionCallbacks()
        if (!needsConsentFlow()) {
            consentStep = ConsentStep.COMPLETE
            permissionsReady = true
            startMonitoringIfEnabled()
        }

        setContent {
            LastTheme {
                PermissionConsentGate(
                    step = consentStep,
                    onConfirm = ::onConsentConfirm,
                    onSkip = ::onConsentSkip,
                ) {
                    LastApp(
                        repository = repository,
                        settingsPermissionCallbacks = settingsPermissionCallbacks,
                    )
                }
            }
        }
    }

    private fun onConsentConfirm(step: ConsentStep) {
        when (step) {
            ConsentStep.INTRO -> beginSystemPermissionFlow()
            ConsentStep.COMPLETE -> finishConsentFlow()
            else -> Unit
        }
    }

    private fun onConsentSkip(step: ConsentStep) {
        if (step == ConsentStep.INTRO) {
            finishConsentFlow()
        }
    }

    private fun beginSystemPermissionFlow() {
        val next = nextConsentStep(ConsentStep.INTRO)
        if (next == ConsentStep.COMPLETE) {
            finishConsentFlow()
        } else {
            consentStep = next
            requestSystemPermissionFor(next)
        }
    }

    private fun requestSystemPermissionFor(step: ConsentStep) {
        when (step) {
            ConsentStep.LOCATION -> {
                val permissions = locationPermissions()
                if (permissions.isEmpty()) {
                    advanceAfter(ConsentStep.LOCATION)
                } else {
                    locationLauncher.launch(permissions.toTypedArray())
                }
            }
            ConsentStep.BACKGROUND_LOCATION -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && needsBackgroundLocation()) {
                    backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                } else {
                    advanceAfter(ConsentStep.BACKGROUND_LOCATION)
                }
            }
            ConsentStep.BLUETOOTH -> {
                val permissions = bluetoothPermissions()
                if (permissions.isEmpty()) {
                    advanceAfter(ConsentStep.BLUETOOTH)
                } else {
                    bluetoothLauncher.launch(permissions.toTypedArray())
                }
            }
            ConsentStep.INTRO, ConsentStep.COMPLETE -> Unit
        }
    }

    private fun advanceAfter(current: ConsentStep) {
        val denied = deniedPermissionsForStep(current)
        if (denied.isNotEmpty()) {
            Toast.makeText(this, getString(R.string.permission_denied_hint), Toast.LENGTH_SHORT).show()
        }
        val next = nextConsentStep(current)
        if (next == ConsentStep.COMPLETE) {
            finishConsentFlow()
        } else {
            consentStep = next
            requestSystemPermissionFor(next)
        }
    }

    private fun finishConsentFlow() {
        consentStep = ConsentStep.COMPLETE
        permissionsReady = true
        startMonitoringIfEnabled()
    }

    private fun nextConsentStep(from: ConsentStep = consentStep): ConsentStep {
        return nextConsentStep(
            current = from,
            missingLocation = missingLocationPermissions(),
            needsBackgroundLocation = needsBackgroundLocation(),
            missingBluetooth = missingBluetoothPermissions(),
        )
    }

    private fun needsConsentFlow(): Boolean {
        return nextConsentStep(ConsentStep.INTRO) != ConsentStep.COMPLETE
    }

    private fun locationPermissions(): List<String> =
        PermissionHelper.locationPermissionsToRequest(this)

    private fun bluetoothPermissions(): List<String> =
        PermissionHelper.bluetoothPermissionsToRequest(this)

    private fun missingLocationPermissions(): Boolean = locationPermissions().isNotEmpty()

    private fun missingBluetoothPermissions(): Boolean = bluetoothPermissions().isNotEmpty()

    private fun needsBackgroundLocation(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        if (missingLocationPermissions()) return false
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        ) != PackageManager.PERMISSION_GRANTED
    }

    private fun deniedPermissionsForStep(step: ConsentStep): List<String> {
        return when (step) {
            ConsentStep.LOCATION -> locationPermissions()
            ConsentStep.BLUETOOTH -> bluetoothPermissions()
            else -> emptyList()
        }
    }

    private fun startMonitoringIfEnabled() {
        if (!permissionsReady) return
        lifecycleScope.launch {
            ConnectionMonitorService.startIfEnabled(this@MainActivity)
        }
    }

    private fun createSettingsPermissionCallbacks(): SettingsPermissionCallbacks {
        return object : SettingsPermissionCallbacks {
            override fun requestLocationPermission(onGranted: () -> Unit) {
                pendingSettingsLocationCallback = onGranted
                val foregroundPermissions = locationPermissions()
                when {
                    foregroundPermissions.isNotEmpty() -> {
                        settingsForegroundLocationLauncher.launch(foregroundPermissions.toTypedArray())
                    }
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && needsBackgroundLocation() -> {
                        settingsBackgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    }
                    else -> {
                        onGranted()
                        pendingSettingsLocationCallback = null
                    }
                }
            }

            override fun requestBluetoothPermission(onGranted: () -> Unit) {
                pendingSettingsBluetoothCallback = onGranted
                val permissions = bluetoothPermissions()
                if (permissions.isEmpty()) {
                    onGranted()
                    pendingSettingsBluetoothCallback = null
                } else {
                    settingsBluetoothLauncher.launch(permissions.toTypedArray())
                }
            }
        }
    }

    private fun completeSettingsLocationRequest() {
        if (!PermissionHelper.hasLocationPermission(this)) {
            showPermissionDeniedToast()
            pendingSettingsLocationCallback = null
            return
        }
        pendingSettingsLocationCallback?.invoke()
        pendingSettingsLocationCallback = null
    }

    private fun showPermissionDeniedToast() {
        Toast.makeText(this, getString(R.string.permission_denied_hint), Toast.LENGTH_SHORT).show()
    }
}
