package no.dsb.keycloak.keycloak_friendly_captcha.wrappers

import org.keycloak.authentication.ValidationContext
import org.keycloak.models.utils.FormMessage


class ValidationContextWrapper(
    private val context: ValidationContext,
    private val config: ConfigWrapper
) {

    val captchaResponse = context.httpRequest.decodedFormParameters.getFirst(config.getFormCaptchaSolution())

    fun triggerError(message: String) {
        val formData = context.httpRequest.decodedFormParameters
        val errors = listOf(FormMessage(null, message))
        formData.remove(config.getFormCaptchaSolution())
        context.validationError(formData, errors)
        context.excludeOtherErrors()
    }

    fun triggerSuccess() {
        context.success()
    }
}