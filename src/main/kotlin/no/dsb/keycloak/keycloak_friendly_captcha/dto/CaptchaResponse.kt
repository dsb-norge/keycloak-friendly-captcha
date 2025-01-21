package no.dsb.keycloak.keycloak_friendly_captcha.dto

data class CaptchaResponse(
    val statusCode: Int,
    val success: Boolean,
    val errors: List<String> = emptyList(),
    val error: ErrorResponse? = null
)
