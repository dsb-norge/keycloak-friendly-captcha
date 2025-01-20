
package no.dsb.keycloak.keycloak_friendly_captcha.wrappers

import com.google.gson.JsonParser
import org.apache.http.client.HttpClient
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import org.jboss.logging.Logger

class HttpClientWrapper(
    private val httpClient: HttpClient,
    private val config: ConfigWrapper
) {

    companion object {
        private val LOGGER = Logger.getLogger(HttpClientWrapper::class.java)
    }

    private val formSolutionKey = if (config.useVersion2()) "response" else "solution"

    private val apiUrl = if (config.useVersion2()) {
        "${config.getApiDomain()}/api/v2/captcha/siteverify"
    } else {
        "${config.getApiDomain()}/api/v1/siteverify"
    }

    private fun addSecret(formData: MutableList<BasicNameValuePair>, post: HttpPost) {
        if (config.useVersion2()) {
            post.setHeader("X-API-Key", config.getSecretKey())
        } else {
            formData.add(BasicNameValuePair("secret", config.getSecretKey()))
        }
    }

    fun verifyFriendlyCaptcha(
        captcha: String,
    ): Boolean {
        val post = HttpPost(apiUrl)
        val formData = mutableListOf(
            BasicNameValuePair(formSolutionKey, captcha),
            BasicNameValuePair("sitekey", config.getSiteKey())
        )

        addSecret(formData, post)

        post.entity = UrlEncodedFormEntity(formData)
        post.setHeader("Content-Type", "application/x-www-form-urlencoded")

        return try {
            val response = httpClient.execute(post)
            val responseString = EntityUtils.toString(response.entity)
            val jsonResponse = JsonParser.parseString(responseString).asJsonObject

            val errors = jsonResponse.getAsJsonArray("errors")

            if (jsonResponse.has("success")) {
                jsonResponse.get("success").asBoolean
            } else if (response.statusLine.statusCode >= 400 && config.getFailOnError()) {
                LOGGER.error("Captcha validation failed with HTTP status code ${response.statusLine.statusCode}, errors: $errors, but allowing user to continue. You should look into this.")
                true
            } else {
                LOGGER.warn("Captcha validation failed with errors: $errors")
                false
            }
        } catch (e: Exception) {
            LOGGER.error("Error validating captcha", e)
            false
        }
    }
}