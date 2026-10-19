package io.github.matheuscruzsouza.sprintt

import android.app.Application
import android.content.Intent
import android.os.Build
import io.github.matheuscruzsouza.sprintt.data.repository.TicketRepository
import io.github.matheuscruzsouza.sprintt.service.SprinttServerService

class SprinttApp : Application() {

    lateinit var ticketRepository: TicketRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        ticketRepository = TicketRepository(this)

        // Inicia o serviço HTTP do Nano-Spring em segundo plano com mDNS na porta 8000
        val serviceIntent = Intent(this, SprinttServerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        // Monitora mudanças de bateria em tempo real para disparar eventos SSE
        registerBatteryMonitor()
    }

    private fun registerBatteryMonitor() {
        val filter = android.content.IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        registerReceiver(object : android.content.BroadcastReceiver() {
            private var lastLevel = -1
            private var lastCharging = false

            override fun onReceive(context: android.content.Context?, intent: Intent?) {
                val info = io.github.matheuscruzsouza.sprintt.service.ServerBatteryProvider.getBatteryInfo(this@SprinttApp)
                if (info.level != lastLevel || info.isCharging != lastCharging) {
                    lastLevel = info.level
                    lastCharging = info.isCharging
                    io.github.matheuscruzsouza.sprintt.web.ServerApiController.broadcastBatteryUpdate(info)
                }
            }
        }, filter)
    }

    companion object {
        lateinit var instance: SprinttApp
            private set
    }
}
