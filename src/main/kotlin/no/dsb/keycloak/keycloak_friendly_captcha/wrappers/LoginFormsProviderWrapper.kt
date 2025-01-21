
package no.dsb.keycloak.keycloak_friendly_captcha.wrappers

import org.keycloak.forms.login.LoginFormsProvider
import org.keycloak.models.utils.FormMessage

class LoginFormsProviderWrapper(private val loginFormsProvider: LoginFormsProvider) {

    fun addError(error: String) {
        loginFormsProvider.addError(FormMessage(error))
    }

    fun setAttribute(key: String, value: Any) {
        loginFormsProvider.setAttribute(key, value)
    }

    fun addScript(scriptUrl: String) {
        loginFormsProvider.addScript(scriptUrl)
    }
}