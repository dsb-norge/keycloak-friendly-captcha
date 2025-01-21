
package no.dsb.keycloak.keycloak_friendly_captcha.wrappers

import no.dsb.keycloak.keycloak_friendly_captcha.client.CaptchaHttpClient
import org.apache.http.message.BasicNameValuePair
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class HttpClientWrapperTest {

    @Test
    fun `should send the correct information when doing a V1 api call`() {
        val config = mock(ConfigWrapper::class.java).apply {
            `when`(useVersion2()).thenReturn(false)
            `when`(getSiteKey()).thenReturn("site-key")
            `when`(getSecretKey()).thenReturn("secret-key")
            `when`(getApiDomain()).thenReturn("https://api-domain.com")
        }
        val httpClient = HttpClientWrapper(
            httpClient(200, """{"success": true}""") { api, headers, formData ->

                assertThat(api).isEqualTo("https://api-domain.com/api/v1/siteverify")

                assertThat(headers).hasSize(1)
                assertThat(headers).containsEntry("Content-Type", "application/x-www-form-urlencoded")

                assertThat(formData).containsExactlyInAnyOrder(
                    BasicNameValuePair("solution", "captcha-response"),
                    BasicNameValuePair("sitekey", "site-key"),
                    BasicNameValuePair("secret", "secret-key")
                )
            },
            config
        )

        val response = httpClient.validateFriendlyCaptcha("captcha-response")
        assertThat(response.success).isTrue()
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.errors).isEmpty()
        assertThat(response.error).isNull()

        assertThat(httpClient.verifyFriendlyCaptchaResponse(response)).isTrue()
    }

    @Test
    fun `should send the correct information when doing a V2 api call`() {
        val config = mock(ConfigWrapper::class.java).apply {
            `when`(useVersion2()).thenReturn(true)
            `when`(getSiteKey()).thenReturn("site-key")
            `when`(getSecretKey()).thenReturn("secret-key")
            `when`(getApiDomain()).thenReturn("https://api-domain.com")
        }
        val httpClient = HttpClientWrapper(
            httpClient(200, """{"success": true}""") { api, headers, formData ->

                assertThat(api).isEqualTo("https://api-domain.com/api/v2/captcha/siteverify")

                assertThat(headers).hasSize(2)
                assertThat(headers).containsEntry("Content-Type", "application/x-www-form-urlencoded")
                assertThat(headers).containsEntry("X-API-Key", "secret-key")

                assertThat(formData).containsExactlyInAnyOrder(
                    BasicNameValuePair("response", "captcha-response"),
                    BasicNameValuePair("sitekey", "site-key")
                )
            },
            config
        )

        val response = httpClient.validateFriendlyCaptcha("captcha-response")
        assertThat(response.success).isTrue()
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.errors).isEmpty()
        assertThat(response.error).isNull()

        assertThat(httpClient.verifyFriendlyCaptchaResponse(response)).isTrue()
    }

    @Test
    fun `should not fail on http status 400 with failOnError false`() {
        val config = mock(ConfigWrapper::class.java).apply {
            `when`(useVersion2()).thenReturn(true)
            `when`(getSiteKey()).thenReturn("site-key")
            `when`(getSecretKey()).thenReturn("secret-key")
            `when`(getApiDomain()).thenReturn("https://api-domain.com")
            `when`(getFailOnError()).thenReturn(false)
        }
        val httpClient = HttpClientWrapper(
            httpClient(400, """{"success": false, "errors": ["error1", "error2"]}"""),
            config
        )

        val response = httpClient.validateFriendlyCaptcha("captcha-response")
        assertThat(response.success).isFalse()
        assertThat(response.statusCode).isEqualTo(400)
        assertThat(response.errors).containsExactly("error1", "error2")
        assertThat(response.error).isNull()

        assertThat(httpClient.verifyFriendlyCaptchaResponse(response)).isTrue()
    }

    @Test
    fun `should fail on http status 400 with failOnError true`() {
        val config = mock(ConfigWrapper::class.java).apply {
            `when`(useVersion2()).thenReturn(true)
            `when`(getSiteKey()).thenReturn("site-key")
            `when`(getSecretKey()).thenReturn("secret-key")
            `when`(getApiDomain()).thenReturn("https://api-domain.com")
            `when`(getFailOnError()).thenReturn(true)
        }
        val httpClient = HttpClientWrapper(
            httpClient(400, """{"success": false, "errors": ["error1", "error2"]}"""),
            config
        )

        val response = httpClient.validateFriendlyCaptcha("captcha-response")
        assertThat(response.success).isFalse()
        assertThat(response.statusCode).isEqualTo(400)
        assertThat(response.errors).containsExactly("error1", "error2")
        assertThat(response.error).isNull()

        assertThat(httpClient.verifyFriendlyCaptchaResponse(response)).isFalse()
    }

    @Test
    fun `should fail on non-success and log v1 errors`() {
        val config = mock(ConfigWrapper::class.java).apply {
            `when`(useVersion2()).thenReturn(false)
            `when`(getSiteKey()).thenReturn("site-key")
            `when`(getSecretKey()).thenReturn("secret-key")
            `when`(getApiDomain()).thenReturn("https://api-domain.com")
            `when`(getFailOnError()).thenReturn(true)
        }
        val httpClient = HttpClientWrapper(
            httpClient(200, """{"success": false, "errors": ["error1", "error2"]}"""),
            config
        )

        val response = httpClient.validateFriendlyCaptcha("captcha-response")
        assertThat(response.success).isFalse()
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.errors).containsExactly("error1", "error2")
        assertThat(response.error).isNull()

        assertThat(httpClient.verifyFriendlyCaptchaResponse(response)).isFalse()
    }

    @Test
    fun `should fail on non-success and log v2 error`() {
        val config = mock(ConfigWrapper::class.java).apply {
            `when`(useVersion2()).thenReturn(true)
            `when`(getSiteKey()).thenReturn("site-key")
            `when`(getSecretKey()).thenReturn("secret-key")
            `when`(getApiDomain()).thenReturn("https://api-domain.com")
            `when`(getFailOnError()).thenReturn(true)
        }
        val httpClient = HttpClientWrapper(
            httpClient(200, """{"success": false, "error": {"error_code": "error-code", "detail": "error-detail"}}"""),
            config
        )

        val response = httpClient.validateFriendlyCaptcha("captcha-response")
        assertThat(response.success).isFalse()
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.errors).isEmpty()
        assertThat(response.error).isNotNull
        assertThat(response.error!!.error_code).isEqualTo("error-code")
        assertThat(response.error.detail).isEqualTo("error-detail")

        assertThat(httpClient.verifyFriendlyCaptchaResponse(response)).isFalse()
    }

    @Test
    fun `should handle invalid non-JSON response and return success if failOnError is false`() {
        val config = mock(ConfigWrapper::class.java).apply {
            `when`(useVersion2()).thenReturn(true)
            `when`(getSiteKey()).thenReturn("site-key")
            `when`(getSecretKey()).thenReturn("secret-key")
            `when`(getApiDomain()).thenReturn("https://api-domain.com")
            `when`(getFailOnError()).thenReturn(false)
        }
        val httpClient = HttpClientWrapper(
            httpClient(200, """invalid-json"""),
            config
        )

        val response = httpClient.validateFriendlyCaptcha("captcha-response")
        assertThat(response.success).isTrue()
        assertThat(response.statusCode).isEqualTo(-1)
        assertThat(response.errors).isEmpty()
        assertThat(response.error).isNull()

        assertThat(httpClient.verifyFriendlyCaptchaResponse(response)).isTrue()
    }

    @Test
    fun `should handle invalid non-JSON response and return failed if failOnError is true`() {
        val config = mock(ConfigWrapper::class.java).apply {
            `when`(useVersion2()).thenReturn(true)
            `when`(getSiteKey()).thenReturn("site-key")
            `when`(getSecretKey()).thenReturn("secret-key")
            `when`(getApiDomain()).thenReturn("https://api-domain.com")
            `when`(getFailOnError()).thenReturn(true)
        }
        val httpClient = HttpClientWrapper(
            httpClient(200, """invalid-json"""),
            config
        )

        val response = httpClient.validateFriendlyCaptcha("captcha-response")
        assertThat(response.success).isFalse()
        assertThat(response.statusCode).isEqualTo(-1)
        assertThat(response.errors).isEmpty()
        assertThat(response.error).isNull()

        assertThat(httpClient.verifyFriendlyCaptchaResponse(response)).isFalse()
    }

    @Test
    fun `should handle invalid JSON response`() {
        val config = mock(ConfigWrapper::class.java).apply {
            `when`(useVersion2()).thenReturn(true)
            `when`(getSiteKey()).thenReturn("site-key")
            `when`(getSecretKey()).thenReturn("secret-key")
            `when`(getApiDomain()).thenReturn("https://api-domain.com")
        }
        val httpClient = HttpClientWrapper(
            httpClient(200, """{"something-else": "not-success"}"""),
            config
        )

        val response = httpClient.validateFriendlyCaptcha("captcha-response")
        assertThat(response.success).isFalse()
        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.errors).isEmpty()
        assertThat(response.error).isNull()

        assertThat(httpClient.verifyFriendlyCaptchaResponse(response)).isFalse()
    }

    private fun httpClient(
        statusCode: Int,
        responseString: String,
        asserter: (api: String, headers: Map<String, String>, formData: List<BasicNameValuePair>) -> Unit = { _, _, _ -> }
    ): CaptchaHttpClient {
        return object : CaptchaHttpClient {
            override fun executePost(url: String, headers: Map<String, String>, formData: List<BasicNameValuePair>): Pair<Int, String> {
                asserter(url, headers, formData)
                return statusCode to responseString
            }
        }
    }
}