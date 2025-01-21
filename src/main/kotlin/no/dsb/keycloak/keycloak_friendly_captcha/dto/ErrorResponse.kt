package no.dsb.keycloak.keycloak_friendly_captcha.dto

data class ErrorResponse(
    val error_code: String,
    val detail: String
)