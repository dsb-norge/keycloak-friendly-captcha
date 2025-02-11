package no.dsb.keycloak.keycloak_friendly_captcha.wrappers

import com.google.gson.JsonParser
import no.dsb.keycloak.keycloak_friendly_captcha.client.CaptchaHttpClient
import no.dsb.keycloak.keycloak_friendly_captcha.client.DefaultCaptchaHttpClient
import no.dsb.keycloak.keycloak_friendly_captcha.dto.CaptchaResponse
import no.dsb.keycloak.keycloak_friendly_captcha.dto.ErrorResponse
import org.apache.http.client.HttpClient
import org.apache.http.message.BasicNameValuePair
import org.jboss.logging.Logger

class HttpClientWrapper(
    private val httpClient: CaptchaHttpClient,
    private val config: ConfigWrapper
) {
    constructor(httpClient: HttpClient, config: ConfigWrapper) : this(DefaultCaptchaHttpClient(httpClient), config)

    companion object {
        private val LOGGER = Logger.getLogger(HttpClientWrapper::class.java)
    }

    private val formSolutionKey = if (config.useVersion2()) "response" else "solution"

    private val apiUrl = if (config.useVersion2()) {
        "${config.getApiDomain()}/api/v2/captcha/siteverify"
    } else {
        "${config.getApiDomain()}/api/v1/siteverify"
    }

    fun verifyFriendlyCaptcha(captcha: String): Boolean {
        val response = validateFriendlyCaptcha(captcha)
        return verifyFriendlyCaptchaResponse(response)
    }

    fun validateFriendlyCaptcha(captcha: String): CaptchaResponse {
        val headers = mutableMapOf(
            "Content-Type" to "application/x-www-form-urlencoded"
        )

        val formData = mutableListOf(
            BasicNameValuePair(formSolutionKey, captcha),
            BasicNameValuePair("sitekey", config.getSiteKey())
        )

        if (config.useVersion2()) {
            headers["X-API-Key"] = config.getSecretKey()
        } else {
            formData.add(BasicNameValuePair("secret", config.getSecretKey()))
        }

        return try {
            val (httpStatus, responseString) = httpClient.executePost(apiUrl, headers, formData)
            val jsonResponse = JsonParser.parseString(responseString).asJsonObject

            val captchaResponse = CaptchaResponse(
                statusCode = httpStatus,
                success = jsonResponse.get("success")?.asBoolean == true,
                errors = if (jsonResponse.has("errors")) {
                    jsonResponse.getAsJsonArray("errors").map { it.asString }
                } else {
                    emptyList()
                },
                error = if (jsonResponse.has("error")) {
                    val errorObj = jsonResponse.getAsJsonObject("error")
                    ErrorResponse(
                        error_code = errorObj.get("error_code").asString,
                        detail = errorObj.get("detail").asString
                    )
                } else {
                    null
                }
            )
            if (!captchaResponse.success) {
                LOGGER.warn("Captcha validation failed: $captchaResponse")
            }
            captchaResponse
        } catch (e: IllegalStateException) {
            LOGGER.error("Error parsing captcha response", e)
            CaptchaResponse(success = !config.getFailOnError(), statusCode = -1)
        } catch (e: Exception) {
            LOGGER.error("Error validating captcha", e)
            CaptchaResponse(success = !config.getFailOnError(), statusCode = -1)
        }
    }

    fun verifyFriendlyCaptchaResponse(response: CaptchaResponse): Boolean {
        return when {
            response.success -> true
            !config.getFailOnError() && response.statusCode >= 400 -> {
                LOGGER.error("Captcha validation failed with HTTP status code ${response.statusCode}, response: $response, but allowing user to continue. You should look into this.")
                true
            }
            response.errors.isNotEmpty() -> {
                LOGGER.warn("Captcha validation failed with v1 errors: ${response.errors}")
                false
            }
            response.error != null -> {
                LOGGER.warn("Captcha validation failed with v2 error: code=${response.error.error_code}, detail=${response.error.detail}")
                false
            }
            else -> {
                LOGGER.warn("Unexpected response format: $response")
                false
            }
        }
    }
}