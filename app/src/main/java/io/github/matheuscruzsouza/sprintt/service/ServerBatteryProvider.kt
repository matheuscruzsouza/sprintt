package io.github.matheuscruzsouza.sprintt.service

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

data class ServerBatteryInfo(
    val level: Int,
    val isCharging: Boolean,
    val isFull: Boolean,
    val plugType: String,
    val statusText: String,
    val isLow: Boolean,
    val formatted: String,
    val icon: String,
    val timestamp: Long = System.currentTimeMillis()
)

object ServerBatteryProvider {

    fun getBatteryInfo(context: Context): ServerBatteryInfo {
        return try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val intent: Intent? = context.registerReceiver(null, filter)

            val rawLevel = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val level = if (rawLevel >= 0 && scale > 0) {
                ((rawLevel / scale.toFloat()) * 100).toInt()
            } else {
                100
            }

            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            val isFull = status == BatteryManager.BATTERY_STATUS_FULL

            val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: 0
            val plugType = when (plugged) {
                BatteryManager.BATTERY_PLUGGED_AC -> "Tomada (AC)"
                BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                4 -> "Sem fio"
                else -> "Bateria"
            }

            val statusText = when {
                isFull -> "Carga Completa"
                isCharging -> "Carregando ($plugType)"
                else -> "Descarregando"
            }

            val isLow = level <= 20 && !isCharging
            val icon = when {
                isCharging -> "⚡"
                level <= 20 -> "🪫"
                level <= 50 -> "🔋"
                else -> "🔋"
            }

            val formatted = "$level% $icon ($statusText)"

            ServerBatteryInfo(
                level = level,
                isCharging = isCharging,
                isFull = isFull,
                plugType = plugType,
                statusText = statusText,
                isLow = isLow,
                formatted = formatted,
                icon = icon
            )
        } catch (_: Exception) {
            ServerBatteryInfo(
                level = 100,
                isCharging = true,
                isFull = true,
                plugType = "AC",
                statusText = "Online",
                isLow = false,
                formatted = "100% ⚡ (Online)",
                icon = "⚡"
            )
        }
    }
}
