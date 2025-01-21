package no.dsb.keycloak.keycloak_friendly_captcha.client

import org.apache.http.message.BasicNameValuePair

interface CaptchaHttpClient {
    fun executePost(url: String, headers: Map<String, String>, formData: List<BasicNameValuePair>): Pair<Int, String>
}