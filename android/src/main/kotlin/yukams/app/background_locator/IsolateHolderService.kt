package yukams.app.background_locator

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.gyf.cactus.ext.cactus
import com.gyf.cactus.ext.cactusRestart
import com.gyf.cactus.ext.cactusUnregister
import io.flutter.FlutterInjector
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import yukams.app.background_locator.pluggables.DisposePluggable
import yukams.app.background_locator.pluggables.InitPluggable
import yukams.app.background_locator.pluggables.Pluggable
import yukams.app.background_locator.provider.AndroidLocationProviderClient
import yukams.app.background_locator.provider.BLLocationProvider
import yukams.app.background_locator.provider.LocationClient
import yukams.app.background_locator.provider.LocationRequestOptions
import yukams.app.background_locator.provider.LocationUpdateListener

class IsolateHolderService : MethodChannel.MethodCallHandler, LocationUpdateListener, Service() {
    companion object {
        @JvmStatic
        val ACTION_SHUTDOWN = "SHUTDOWN"

        @JvmStatic
        val ACTION_START = "START"

        @JvmStatic
        val ACTION_UPDATE_NOTIFICATION = "UPDATE_NOTIFICATION"

        @JvmStatic
        var backgroundEngine: FlutterEngine? = null

        @JvmStatic
        var isServiceRunning = false

        @JvmStatic
        var isServiceInitialized = false

        fun getBinaryMessenger(context: Context?): BinaryMessenger? {
            val messenger = backgroundEngine?.dartExecutor?.binaryMessenger
            return messenger
                ?: if (context != null) {
                    backgroundEngine = FlutterEngine(context)
                    backgroundEngine?.dartExecutor?.binaryMessenger
                } else {
                    messenger
                }
        }
    }

    private var notificationChannelName = "Flutter Locator Plugin"
    private var notificationTitle = "Start Location Tracking"
    private var notificationMsg = "Track location in background"
    private var notificationBigMsg =
        "Background location is on to keep the app up-tp-date with your location. This is required for main features to work properly when the app is not running."
    private var notificationIconColor = 0
    private var icon = 0
    private var wakeLockTime = 60 * 60 * 1000L // 1 hour default wake lock time
    private var locatorClient: BLLocationProvider? = null
    internal lateinit var backgroundChannel: MethodChannel
    internal var context: Context? = null
    private var pluggables: ArrayList<Pluggable> = ArrayList()

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        cactus {
            isDebug(true)
            setNotification(getNotification())
        }

        startLocatorService(this)
    }

    private fun start() {
        cactusRestart()
        pluggables.forEach {
            context?.let { it1 -> it.onServiceStart(it1) }
        }
    }

    private fun getNotification(): Notification {
        val channel = NotificationChannel(
            Keys.CHANNEL_ID, notificationChannelName,
            NotificationManager.IMPORTANCE_LOW
        )

        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)

        val intent = Intent(this, getMainActivityClass(this))
        intent.action = Keys.NOTIFICATION_ACTION

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this,
            1, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, Keys.CHANNEL_ID)
            .setContentTitle(notificationTitle)
            .setContentText(notificationMsg)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(notificationBigMsg)
            )
            .setSmallIcon(icon)
            .setColor(notificationIconColor)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true) // so when data is updated don't make sound and alert in android 8.0+
            .setOngoing(true)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e("IsolateHolderService", "onStartCommand => intent.action : ${intent?.action}")
        if (intent == null) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e("IsolateHolderService", "app has crashed, stopping it")
                stopSelf()
            } else {
                return super.onStartCommand(intent, flags, startId)
            }
        }

        when {
            ACTION_SHUTDOWN == intent?.action -> {
                isServiceRunning = false
                shutdownHolderService()
            }

            ACTION_START == intent?.action -> {
                if (isServiceRunning) {
                    isServiceRunning = false
                    shutdownHolderService()
                }

                if (!isServiceRunning) {
                    isServiceRunning = true
                    startHolderService(intent)
                }
            }

            ACTION_UPDATE_NOTIFICATION == intent?.action -> {
                if (isServiceRunning) {
                    updateNotification(intent)
                }
            }
        }

        return START_STICKY
    }

    private fun startHolderService(intent: Intent) {
        Log.e("IsolateHolderService", "startHolderService")
        notificationChannelName =
            intent.getStringExtra(Keys.SETTINGS_ANDROID_NOTIFICATION_CHANNEL_NAME).toString()
        notificationTitle =
            intent.getStringExtra(Keys.SETTINGS_ANDROID_NOTIFICATION_TITLE).toString()
        notificationMsg = intent.getStringExtra(Keys.SETTINGS_ANDROID_NOTIFICATION_MSG).toString()
        notificationBigMsg =
            intent.getStringExtra(Keys.SETTINGS_ANDROID_NOTIFICATION_BIG_MSG).toString()
        val iconNameDefault = "ic_launcher"
        var iconName = intent.getStringExtra(Keys.SETTINGS_ANDROID_NOTIFICATION_ICON)
        if (iconName == null || iconName.isEmpty()) {
            iconName = iconNameDefault
        }
        icon = resources.getIdentifier(iconName, "mipmap", packageName)
        notificationIconColor =
            intent.getLongExtra(Keys.SETTINGS_ANDROID_NOTIFICATION_ICON_COLOR, 0).toInt()
        wakeLockTime = intent.getIntExtra(Keys.SETTINGS_ANDROID_WAKE_LOCK_TIME, 60) * 60 * 1000L

        locatorClient = context?.let { getLocationClient(it) }
        locatorClient?.requestLocationUpdates(getLocationRequest(intent))

        // Fill pluggable list
        if (intent.hasExtra(Keys.SETTINGS_INIT_PLUGGABLE)) {
            pluggables.add(InitPluggable())
        }

        if (intent.hasExtra(Keys.SETTINGS_DISPOSABLE_PLUGGABLE)) {
            pluggables.add(DisposePluggable())
        }

        start()
    }

    private fun shutdownHolderService() {
        Log.e("IsolateHolderService", "shutdownHolderService")
        locatorClient?.removeLocationUpdates()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        cactusUnregister()
        pluggables.forEach {
            context?.let { it1 -> it.onServiceDispose(it1) }
        }
    }

    private fun updateNotification(intent: Intent) {
        Log.e("IsolateHolderService", "updateNotification")
        if (intent.hasExtra(Keys.SETTINGS_ANDROID_NOTIFICATION_TITLE)) {
            notificationTitle =
                intent.getStringExtra(Keys.SETTINGS_ANDROID_NOTIFICATION_TITLE).toString()
        }

        if (intent.hasExtra(Keys.SETTINGS_ANDROID_NOTIFICATION_MSG)) {
            notificationMsg =
                intent.getStringExtra(Keys.SETTINGS_ANDROID_NOTIFICATION_MSG).toString()
        }

        if (intent.hasExtra(Keys.SETTINGS_ANDROID_NOTIFICATION_BIG_MSG)) {
            notificationBigMsg =
                intent.getStringExtra(Keys.SETTINGS_ANDROID_NOTIFICATION_BIG_MSG).toString()
        }

        val notification = getNotification()
        cactus {
            setNotification(notification)
        }
    }

    private fun getMainActivityClass(context: Context): Class<*>? {
        val packageName = context.packageName
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        val className = launchIntent?.component?.className ?: return null

        return try {
            Class.forName(className)
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
            null
        }
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        try {
            when (call.method) {
                Keys.METHOD_SERVICE_INITIALIZED -> {
                    isServiceRunning = true
                }

                else -> result.notImplemented()
            }

            result.success(null)
        } catch (e: Exception) {

        }
    }

    override fun onDestroy() {
        isServiceRunning = false
        super.onDestroy()
    }


    private fun getLocationClient(context: Context): BLLocationProvider {
        return when (PreferencesManager.getLocationClient(context)) {
            LocationClient.Android -> AndroidLocationProviderClient(context, this)
        }
    }

    override fun onLocationUpdated(location: HashMap<String, Any>?) {
        try {
            context?.let {
                FlutterInjector.instance().flutterLoader().ensureInitializationComplete(
                    it, null
                )
            }

            //https://github.com/flutter/plugins/pull/1641
            //https://github.com/flutter/flutter/issues/36059
            //https://github.com/flutter/plugins/pull/1641/commits/4358fbba3327f1fa75bc40df503ca5341fdbb77d
            // new version of flutter can not invoke method from background thread
            if (location != null) {
                val callback =
                    context?.let {
                        PreferencesManager.getCallbackHandle(
                            it,
                            Keys.CALLBACK_HANDLE_KEY
                        )
                    } as Long

                val result: HashMap<Any, Any> =
                    hashMapOf(
                        Keys.ARG_CALLBACK to callback,
                        Keys.ARG_LOCATION to location
                    )

                sendLocationEvent(result)
            }
        } catch (e: Exception) {

        }
    }

    private fun sendLocationEvent(result: HashMap<Any, Any>) {
        //https://github.com/flutter/plugins/pull/1641
        //https://github.com/flutter/flutter/issues/36059
        //https://github.com/flutter/plugins/pull/1641/commits/4358fbba3327f1fa75bc40df503ca5341fdbb77d
        // new version of flutter can not invoke method from background thread

        if (backgroundEngine != null) {
            context?.let {
                val backgroundChannel =
                    MethodChannel(
                        getBinaryMessenger(it)!!,
                        Keys.BACKGROUND_CHANNEL_ID
                    )
                Handler(it.mainLooper)
                    .post {
                        Log.d("plugin", "sendLocationEvent $result")
                        backgroundChannel.invokeMethod(Keys.BCM_SEND_LOCATION, result)
                    }
            }
        }
    }

    fun getLocationRequest(intent: Intent): LocationRequestOptions {
        val interval: Long = (intent.getIntExtra(Keys.SETTINGS_INTERVAL, 10) * 1000).toLong()
        val distanceFilter = intent.getDoubleExtra(Keys.SETTINGS_DISTANCE_FILTER, 0.0)

        return LocationRequestOptions(interval, distanceFilter.toFloat())
    }
}
