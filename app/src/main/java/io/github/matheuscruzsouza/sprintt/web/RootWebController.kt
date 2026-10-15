package io.github.matheuscruzsouza.sprintt.web

import com.github.matheuscruzsouza.nanospring.annotation.GetMethod
import com.github.matheuscruzsouza.nanospring.annotation.RestController
import com.github.matheuscruzsouza.nanospring.ui.ModelAndView

import com.github.matheuscruzsouza.nanospring.http.ResponseEntity
import io.github.matheuscruzsouza.sprintt.SprinttApp
import java.io.InputStreamReader

@RestController("/")
class RootWebController {

    @GetMethod("")
    fun index(): ModelAndView {
        return JiraWebController.renderProjectsList()
    }

    @GetMethod("/")
    fun indexSlash(): ModelAndView {
        return index()
    }

    @GetMethod("/rootCA.pem")
    fun downloadRootCa(): ResponseEntity<String> {
        return serveCaCertificate()
    }

    @GetMethod("/cert/rootCA.pem")
    fun downloadRootCaAlt(): ResponseEntity<String> {
        return serveCaCertificate()
    }

    private fun serveCaCertificate(): ResponseEntity<String> {
        return try {
            val stream = SprinttApp.instance.assets.open("static/rootCA.pem")
            val content = stream.bufferedReader().use { it.readText() }
            ResponseEntity.ok(content)
        } catch (_: Exception) {
            ResponseEntity.status(404).body("rootCA.pem não encontrado nos assets estáticos.")
        }
    }
}

