package org.sdn.android.sdk.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.util.*
import java.util.logging.Logger
import kotlin.random.Random
import radixmobile.RadixMonolith

class RadixService : Service() {
    private lateinit var timer: Timer
    private var notificationManager: NotificationManager? = null
    private var notification: Notification? = null
    private val notificationId = 545
    private var multicastLock: WifiManager.MulticastLock? = null
    private val logger = Logger.getLogger(RadixService::class.java.name)

    companion object {
        private val PASSWORD_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toList()
        private const val PASSWORD_LENGTH = 16

        // From Synapse
        private val DEVICE_ID_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toList()
        private const val DEVICE_ID_LENGTH = 10

        private fun randomString(charPool: List<Char>, size: Int): String {
            return (0..size).map { charPool[Random.nextInt(charPool.size)] }
                    .joinToString("")
        }

        fun generateRandomPassword(): String {
            return randomString(PASSWORD_CHARS, PASSWORD_LENGTH)
        }

        fun generateDeviceId(): String {
            return randomString(DEVICE_ID_CHARS, DEVICE_ID_LENGTH)
        }
    }

    private val binder = RadixLocalBinder()
    private var monolith: RadixMonolith? = null

    inner class RadixLocalBinder : Binder() {
        fun getService(): RadixService {
            return this@RadixService
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    private fun ByteArray.toHex(): String {
        return joinToString("") { "%02x".format(it) }
    }

    private fun clearNotification() {
        if (notificationManager == null) {
            return
        }
//        timer.cancel()
        notificationManager!!.cancel(notificationId)
    }

    fun updateNotification() {
        if (notificationManager == null || monolith == null) {
            return
        }

        val remotePeers = monolith!!.peerCount().toInt()

        val title = "Peer-to-peer service running"

        val text: String = if (remotePeers == 0) {
            "No connectivity"
        } else {
            "Connected to $remotePeers peers"
        }

        notification = NotificationCompat.Builder(
            applicationContext,
            applicationContext.packageName
        )
                .setContentTitle(title)
                .setContentText(text)
                .setSilent(false)
                .setOngoing(false)
                .setCategory(Notification.CATEGORY_STATUS)
                .setOnlyAlertOnce(true)
                .build()
        notificationManager!!.notify(notificationId, notification)
    }

    override fun onCreate() {
        logger.info("RadixService.onCreate")

        notificationManager = applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = applicationInfo.loadLabel(packageManager)
            val notificationChannel = NotificationChannel(
                applicationContext.packageName,
                name, NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager!!.createNotificationChannel(notificationChannel)
        }

        if (monolith == null) {
            monolith = RadixMonolith()
        }
        // See issue #735: Android 11 blocks local discovery if we did not acquire MulticastLock.
//        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
//        multicastLock = wifiManager.createMulticastLock("multicastLock")
//        multicastLock?.setReferenceCounted(true)
//        multicastLock?.acquire()
        monolith!!.storageDirectory = applicationContext.filesDir.toString()
        monolith!!.cacheDirectory = applicationContext.cacheDir.toString()
        monolith!!.testNet = true
        monolith!!.setStaticPeer("/ip4/111.200.195.194/tcp/8082/ws/p2p/12D3KooWEbqSC2vSudddmPARBK1nbypStxvo9wWGoqhzrBRc7N1e,/ip4/47.122.17.23/tcp/8081/p2p/12D3KooWJaTahvj7arN6DSoq8bBoDMqSY5f3ubTyASNGDMxd8gm2,/ip4/47.122.17.23/tcp/9086/p2p/12D3KooWEK2jA9hFSrSp8EqbWfqAXoCZXNAfVNZccuUpUZV1CGsD")

        logger.info("RadixService: start monolith...")
        Thread {
            logger.info("RadixService: start monolith begin")
            monolith!!.start()
            logger.info("RadixService: start monolith finished")

//            timer = Timer()
//            timer.schedule(object : TimerTask() {
//                override fun run() {
//                    updateNotification()
//                }
//            }, 0, 3000)

        }.start()

        super.onCreate()
    }

    private fun stopMonolith() {
        if (monolith == null) {
            return
        }

        if (notificationManager != null) {
            notificationManager!!.cancel(notificationId)
        }

        monolith?.stop()
        monolith = null
    }

    override fun onDestroy() {
        logger.info("RadixService.onDestroy")
        logger.info("RadixService.onDestroy # release resources")
        clearNotification()
        if (multicastLock != null) {
            multicastLock!!.release()
        }
        logger.info("RadixService.onDestroy # Stopping monolith")
        stopMonolith()
        logger.info("RadixService.onDestroy # Call super.onDestroy")
        super.onDestroy()
    }
}