package no.dsb.keycloak.keycloak_friendly_captcha.wrappers

import org.keycloak.utils.StringUtil

class ConfigWrapper(private val map: Map<String, String>) {

    companion object {
        const val SITE_KEY = "site.key"
        const val SECRET_KEY = "secret.key"
        const val API_DOMAIN = "captcha.domain"
        const val FORM_CAPTCHA_SOLUTION = "captcha.form.attribute"
        const val FAIL_ON_ERROR = "captcha.fail_on_error"
        const val VERSION = "captcha.version"
    }

    fun getSiteKey(): String = map[SITE_KEY] ?: throw IllegalStateException("Site key not found")
    fun getSecretKey(): String = map[SECRET_KEY] ?: throw IllegalStateException("Secret key not found")
    fun getApiDomain(): String = map[API_DOMAIN] ?: throw IllegalStateException("API domain not found")
    fun getFormCaptchaSolution(): String = map[FORM_CAPTCHA_SOLUTION] ?: throw IllegalStateException("Form captcha solution not found")
    fun getFailOnError(): Boolean = map[FAIL_ON_ERROR] == "true"
    fun useVersion2(): Boolean = map[VERSION] == "true"

    fun validateConfig(): Boolean {
        return !(StringUtil.isNullOrEmpty(map[SITE_KEY])
                || StringUtil.isNullOrEmpty(map[SECRET_KEY])
                || StringUtil.isNullOrEmpty(map[API_DOMAIN])
                || StringUtil.isNullOrEmpty(map[FORM_CAPTCHA_SOLUTION])
                || StringUtil.isNullOrEmpty(map[FAIL_ON_ERROR])
                || StringUtil.isNullOrEmpty(map[VERSION]))
    }
}