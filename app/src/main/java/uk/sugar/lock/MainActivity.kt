package uk.sugar.lock

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.text.format.DateUtils
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var reading: TextView
    private lateinit var raw: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (20 * resources.displayMetrics.density).toInt()
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
        }
        fun label(size: Float) = TextView(this).apply { textSize = size; setPadding(0, pad / 2, 0, pad / 2) }

        reading = label(44f)
        status = label(16f)
        col.addView(reading)
        col.addView(status)

        col.addView(Button(this).apply {
            text = "1. Allow notification access"
            setOnClickListener { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        })
        col.addView(Button(this).apply {
            text = "2. Allow this app to post notifications"
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
                else openNotificationSettings()
            }
        })
        col.addView(Button(this).apply {
            text = "3. Lock-screen settings"
            setOnClickListener { openNotificationSettings() }
        })
        col.addView(label(14f).apply {
            text = "On the lock screen, make sure notifications are set to show all content " +
                "(not 'hide sensitive content').\n\nThis is a display only. CamAPS FX stays in charge of " +
                "alarms and insulin – always check CamAPS before acting on a number."
        })
        raw = label(12f)
        col.addView(raw)

        setContentView(ScrollView(this).apply { addView(col) })
    }

    override fun onResume() {
        super.onResume()
        val granted = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            ?.contains(packageName) == true
        if (granted) NotificationListenerService.requestRebind(ComponentName(this, CamApsListener::class.java))

        val notifOk = Build.VERSION.SDK_INT < 33 ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

        val r = Store.load(this)
        reading.text = r?.let { listOf(it.value, it.unit, it.trend).filter(String::isNotEmpty).joinToString(" ") } ?: "--"
        status.text = buildString {
            append(if (granted) "✓ Notification access on\n" else "✗ Notification access needed\n")
            append(if (notifOk) "✓ Can post notifications\n" else "✗ Notification permission needed\n")
            append(r?.let { "Last reading " + DateUtils.getRelativeTimeSpanString(it.timeMs) } ?: "No reading from CamAPS FX yet")
        }
        val last = Store.raw(this)
        raw.text = if (last.isNotEmpty()) "Last CamAPS notification text:\n$last" else ""

        if (notifOk) LockScreenNotifier.show(this, r)
    }

    private fun openNotificationSettings() = startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    )
}
