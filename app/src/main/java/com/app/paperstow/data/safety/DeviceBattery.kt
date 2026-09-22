package com.app.paperstow.data.safety

import android.content.Context
import android.os.BatteryManager

object DeviceBattery {
    /** Remaining battery as 0–100, or -1 if the device does not report it. */
    fun remainingPercent(context: Context): Int {
        val manager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return -1
        val percent = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return if (percent in 0..100) percent else -1
    }
}
