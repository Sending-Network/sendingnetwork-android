package org.sdn.android.sdk.api.meet

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.KeyEvent
import org.jitsi.meet.sdk.JitsiMeet
import org.jitsi.meet.sdk.JitsiMeetActivity
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions
import org.sdn.android.sdk.BuildConfig
import java.net.MalformedURLException
import java.net.URL


/**
 * The `Activity` is launched in `singleTask` mode, so it will be
 * created upon application initialization and there will be a single instance
 * of it. Further attempts at launching the application once it was already
 * launched will result in [SdnMeetActivity.onNewIntent] being called.
 */
class SdnMeetActivity : JitsiMeetActivity() {
    /**
     * Broadcast receiver for restrictions handling
     */
    private var broadcastReceiver: BroadcastReceiver? = null

    /**
     * Flag if configuration is provided by RestrictionManager
     */
    private var configurationByRestrictions = false

    /**
     * Default URL as could be obtained from RestrictionManager
     */
//    private var defaultURL: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        JitsiMeet.showSplashScreen(this)
        super.onCreate(null)
    }
    override fun initialize() {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                // As new restrictions including server URL are received,
                // conference should be restarted with new configuration.
                leave()
                recreate()
            }

        }

        registerReceiver(
            broadcastReceiver,
            IntentFilter(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED)
        )
//        defaultURL = url
        super.initialize()
    }

    override fun onDestroy() {
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver)
            broadcastReceiver = null
        }
        super.onDestroy()
    }




    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (BuildConfig.DEBUG && keyCode == KeyEvent.KEYCODE_MENU) {
            JitsiMeet.showDevOptions()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }


    private fun buildURL(urlStr: String?): URL? {
        return try {
            URL(urlStr)
        } catch (e: MalformedURLException) {
            null
        }
    }

    companion object {

        private fun setJitsiMeetConferenceDefaultOptions(url: String) {
            // Set default options
            val defaultOptions = JitsiMeetConferenceOptions.Builder()
                .setServerURL( URL(url))
                .setFeatureFlag("welcomepage.enabled", false)
                .setFeatureFlag("call-integration.enabled", false)
                .setFeatureFlag("server-url-change.enabled", true)
                .build()
            JitsiMeet.setDefaultConferenceOptions(defaultOptions)
        }
        fun launch(context: Context, roomID: String, url: String) {
            setJitsiMeetConferenceDefaultOptions(url)
            val options = JitsiMeetConferenceOptions.Builder()
                .setRoom(roomID)
                .build()

            val intent = Intent(context, JitsiMeetActivity::class.java)
            intent.action = "org.jitsi.meet.CONFERENCE"
            intent.putExtra("JitsiMeetConferenceOptions", options)
            if (context !is Activity) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}
