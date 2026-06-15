package com.last.app.infrastructure

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.last.app.LastApplication
import com.last.app.infrastructure.monitoring.ConnectionMonitor
import com.last.app.infrastructure.monitoring.PeriodicLocationRecorder
import com.last.app.data.repository.LastRepository
import com.last.app.external.system.AndroidSystemEventListener
import com.last.app.external.system.BluetoothEventActions
import com.last.app.external.location.FusedLocationService
import com.last.app.external.system.SystemEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import com.last.app.infrastructure.platform.PermissionHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ConnectionMonitorService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val channelId = "monitor_channel"

    private lateinit var repository: LastRepository
    private lateinit var connectionMonitor: ConnectionMonitor
    private lateinit var systemEventListener: AndroidSystemEventListener
    private var periodicLocationJob: Job? = null
    private var bluetoothSyncJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        repository = LastApplication.repositoryOf(this)
        connectionMonitor = ConnectionMonitor(
            repository = repository,
            locationService = FusedLocationService(this),
        )

        val monitorListener = SystemEventListener { deviceType, eventType, timestamp, deviceIdentifier ->
            serviceScope.launch {
                connectionMonitor.detectConnectionEvent(deviceType, eventType, timestamp, deviceIdentifier)
            }
        }
        systemEventListener = AndroidSystemEventListener(monitorListener)

        serviceScope.launch {
            connectionMonitor.refreshSettings()
            val missing = buildList {
                if (!PermissionHelper.hasBluetoothPermissions(this@ConnectionMonitorService)) add("Bluetooth")
                if (!PermissionHelper.hasLocationPermission(this@ConnectionMonitorService)) add("Location")
            }
            if (missing.isNotEmpty()) {
                val labels = buildList {
                    if (missing.contains("Bluetooth")) add("Bluetooth")
                    if (missing.contains("Location")) add("위치")
                }
                repository.saveLog(
                    message = "필요 권한이 없습니다: ${labels.joinToString()}",
                    logType = "SYSTEM",
                )
            }
            connectionMonitor.startMonitoring()
            startPeriodicLocationRecording()
            startBluetoothConnectionSync()
        }

        createNotificationChannel()
        startForegroundService()
        registerReceivers()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_APPLY_SETTINGS) {
            serviceScope.launch {
                connectionMonitor.refreshSettings()
                val settings = repository.getSettings()
                if (!settings.autoMonitoringEnabled) {
                    stopSelf()
                } else if (!connectionMonitor.monitoring) {
                    connectionMonitor.startMonitoring()
                    startPeriodicLocationRecording()
                    startBluetoothConnectionSync()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundService() {
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("LAST 백그라운드 모니터링")
            .setContentText("등록된 블루투스·USB 주변기기 연결 상태를 감시 중")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            }
            startForeground(1, notification, serviceType)
        } else {
            startForeground(1, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Monitor Service",
                NotificationManager.IMPORTANCE_LOW,
            )
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun registerReceivers() {
        val filter = IntentFilter().apply {
            BluetoothEventActions.ALL.forEach { addAction(it) }
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction("android.hardware.usb.action.USB_DEVICE_ATTACHED")
            addAction("android.hardware.usb.action.USB_DEVICE_DETACHED")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(systemEventListener, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(systemEventListener, filter)
        }
    }

    private fun startBluetoothConnectionSync() {
        bluetoothSyncJob?.cancel()
        bluetoothSyncJob = serviceScope.launch {
            while (isActive) {
                connectionMonitor.syncBluetoothConnectionStatuses()
                delay(BLUETOOTH_SYNC_INTERVAL_MS)
            }
        }
    }

    private fun startPeriodicLocationRecording() {
        periodicLocationJob?.cancel()
        val locationService = FusedLocationService(this)
        val recorder = PeriodicLocationRecorder(repository, locationService)
        periodicLocationJob = serviceScope.launch {
            recorder.runLoop()
        }
    }

    override fun onDestroy() {
        periodicLocationJob?.cancel()
        bluetoothSyncJob?.cancel()
        unregisterReceiver(systemEventListener)
        runBlocking { connectionMonitor.stopMonitoring() }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_APPLY_SETTINGS = "com.last.app.action.APPLY_SETTINGS"
        private const val BLUETOOTH_SYNC_INTERVAL_MS = 5_000L

        fun startIfEnabled(context: Context) {
            if (!PermissionHelper.hasBluetoothPermissions(context)) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                !PermissionHelper.hasLocationPermission(context)
            ) {
                return
            }
            val repository = LastApplication.repositoryOf(context)
            kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                if (repository.getSettings().autoMonitoringEnabled) {
                    ContextCompat.startForegroundService(
                        context,
                        Intent(context, ConnectionMonitorService::class.java),
                    )
                }
            }
        }

        fun applySettings(context: Context) {
            val intent = Intent(context, ConnectionMonitorService::class.java).apply {
                action = ACTION_APPLY_SETTINGS
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
