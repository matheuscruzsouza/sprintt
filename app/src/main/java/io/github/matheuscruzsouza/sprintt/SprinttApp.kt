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
    }

    companion object {
        lateinit var instance: SprinttApp
            private set
    }
}
