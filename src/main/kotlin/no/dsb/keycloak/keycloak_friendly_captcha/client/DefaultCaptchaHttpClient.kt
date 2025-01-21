package no.dsb.keycloak.keycloak_friendly_captcha.client

import org.apache.http.client.HttpClient
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils

class DefaultCaptchaHttpClient(private val httpClient: HttpClient) : CaptchaHttpClient {
    override fun executePost(url: String, headers: Map<String, String>, formData: List<BasicNameValuePair>): Pair<Int, String> {
        val post = HttpPost(url)
        headers.forEach { (key, value) -> post.setHeader(key, value) }
        post.entity = UrlEncodedFormEntity(formData)
        val response = httpClient.execute(post)
        return Pair(response.statusLine.statusCode, EntityUtils.toString(response.entity))
    }
}