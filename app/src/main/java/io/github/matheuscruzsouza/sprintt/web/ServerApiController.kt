package io.github.matheuscruzsouza.sprintt.web

import com.github.matheuscruzsouza.nanospring.annotation.GetMethod
import com.github.matheuscruzsouza.nanospring.annotation.RestController
import com.github.matheuscruzsouza.nanospring.http.ResponseEntity
import com.github.matheuscruzsouza.nanospring.openapi.annotation.Operation
import com.github.matheuscruzsouza.nanospring.openapi.annotation.Tag
import io.github.matheuscruzsouza.sprintt.SprinttApp
import io.github.matheuscruzsouza.sprintt.service.ServerBatteryInfo
import io.github.matheuscruzsouza.sprintt.service.ServerBatteryProvider
import fi.iki.elonen.NanoHTTPD.IHTTPSession

@RestController("/api/v1/server")
@Tag(name = "Servidor", description = "Telemetria de Hardware e Estado do Servidor Android")
class ServerApiController {

    @GetMethod("/battery")
    @Operation(summary = "Status da bateria do servidor", description = "Retorna nível de bateria, carregamento e saúde energética")
    fun getBattery(session: IHTTPSession): Any {
        val info = ServerBatteryProvider.getBatteryInfo(SprinttApp.instance)
        val format = session.parms?.get("format")

        if (format == "html") {
            val color = when {
                info.isLow -> "#EF4444"
                info.isCharging -> "#10B981"
                info.level <= 30 -> "#F59E0B"
                else -> "#3B82F6"
            }
            val html = """
                <div class="server-battery-badge" style="display: inline-flex; align-items: center; gap: 6px; padding: 4px 10px; border-radius: 999px; background: rgba(255,255,255,0.08); border: 1px solid $color; font-size: 0.78rem; font-weight: 600; color: var(--text-primary);">
                    <span>${info.icon}</span>
                    <span>${info.level}%</span>
                    <span style="color: var(--text-secondary); font-size: 0.72rem;">(${info.statusText})</span>
                </div>
            """.trimIndent()
            return ResponseEntity.ok(html)
        }

        return ResponseEntity.ok(info)
    }

    @GetMethod("/telemetry")
    @Operation(summary = "Telemetria completa do servidor", description = "Dados consolidados de bateria, memória e storage")
    fun getTelemetry(): ResponseEntity<Map<String, Any>> {
        val battery = ServerBatteryProvider.getBatteryInfo(SprinttApp.instance)
        val runtime = Runtime.getRuntime()
        val usedRamMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val maxRamMb = runtime.maxMemory() / (1024 * 1024)

        return ResponseEntity.ok(
            mapOf(
                "battery" to battery,
                "ramUsedMb" to usedRamMb,
                "ramMaxMb" to maxRamMb,
                "serverHost" to "sprintt.local",
                "serverPort" to 8443,
                "sslEnabled" to true
            )
        )
    }
}
