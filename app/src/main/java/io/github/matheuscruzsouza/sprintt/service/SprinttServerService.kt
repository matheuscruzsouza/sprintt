package io.github.matheuscruzsouza.sprintt.service

import com.github.matheuscruzsouza.nanospring.service.NanoSpringService

class SprinttServerService : NanoSpringService() {

    override fun getBasePackage(): String {
        return "io.github.matheuscruzsouza.sprintt"
    }

    override fun getPort(): Int {
        return 8443
    }

    override fun getNotificationTitle(): String {
        return "TicketMaster Jira Server"
    }

    override fun getNotificationText(): String {
        return "Disponível em https://sprintt.local:8443/projetos"
    }
}
