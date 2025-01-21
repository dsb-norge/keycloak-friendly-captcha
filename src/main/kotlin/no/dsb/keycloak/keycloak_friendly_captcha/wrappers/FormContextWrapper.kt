
package no.dsb.keycloak.keycloak_friendly_captcha.wrappers

import org.keycloak.authentication.FormContext
import org.keycloak.services.Urls

class FormContextWrapper(private val formContext: FormContext) {

    fun initializeFriendlyCaptcha(
        config: ConfigWrapper,
        form: LoginFormsProviderWrapper
    ) {
        form.setAttribute("friendlyCaptchaRequired", true)
        form.setAttribute("friendlyCaptchaSiteKey", config.getSiteKey())
        form.setAttribute("friendlyCaptchaLang", userLanguageTag())
        form.setAttribute("friendlyCaptchaFormAttribute", config.getFormCaptchaSolution())
        form.setAttribute("friendlyCaptchaApiDomain", config.getApiDomain())
        form.setAttribute("friendlyCaptchaV2", config.useVersion2())
        form.addScript("${getThemeRoot()}/login/base/js/${widgetJs(config)}")
        form.addScript("${getThemeRoot()}/login/base/js/${widgetModuleJs(config)}")
    }

    private fun getThemeRoot() = Urls.themeRoot(formContext.uriInfo.baseUri).path
    private fun userLanguageTag() = formContext.session.context.resolveLocale(formContext.user).toLanguageTag()

    private fun widgetJs(config: ConfigWrapper) = if (config.useVersion2()) {
        "widget.v2.min.js"
    } else {
        "widget.min.js"
    }

    private fun widgetModuleJs(config: ConfigWrapper) = if (config.useVersion2()) {
        "widget.v2.module.min.js"
    } else {
        "widget.module.min.js"
    }
}