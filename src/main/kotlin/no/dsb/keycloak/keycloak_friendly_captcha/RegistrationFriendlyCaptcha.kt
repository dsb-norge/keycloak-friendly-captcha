package no.dsb.keycloak.keycloak_friendly_captcha

import com.google.gson.JsonParser
import org.apache.http.client.HttpClient
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import org.jboss.logging.Logger
import org.keycloak.Config
import org.keycloak.authentication.FormAction
import org.keycloak.authentication.FormActionFactory
import org.keycloak.authentication.FormContext
import org.keycloak.authentication.ValidationContext
import org.keycloak.connections.httpclient.HttpClientProvider
import org.keycloak.forms.login.LoginFormsProvider
import org.keycloak.models.*
import org.keycloak.models.utils.FormMessage
import org.keycloak.provider.ProviderConfigProperty
import org.keycloak.services.Urls
import org.keycloak.services.validation.Validation
import org.keycloak.utils.StringUtil

class RegistrationFriendlyCaptcha : FormAction, FormActionFactory {

    companion object {
        private val LOGGER = Logger.getLogger(RegistrationFriendlyCaptcha::class.java)

        const val PROVIDER_ID = "friendly-captcha"
        private const val DISPLAY_NAME = "Friendly Captcha"

        const val SITE_KEY = "site.key"
        const val SECRET_KEY = "secret.key"
        const val API_DOMAIN = "captcha.domain"
        const val FORM_CAPTCHA_SOLUTION = "captcha.form.attribute"
        const val FAIL_ON_ERROR = "captcha.fail_on_error"
    }

    override fun getId() = PROVIDER_ID
    override fun getDisplayType() = DISPLAY_NAME
    override fun getHelpText() = "Validates that the user is human using Friendly Captcha v1"
    override fun getReferenceCategory() = "recaptcha"
    override fun isConfigurable() = true
    override fun isUserSetupAllowed() = false
    override fun requiresUser() = false
    override fun configuredFor(session: KeycloakSession, realm: RealmModel, user: UserModel) = true
    override fun create(session: KeycloakSession) = this

    override fun getRequirementChoices() = arrayOf(
        AuthenticationExecutionModel.Requirement.REQUIRED,
        AuthenticationExecutionModel.Requirement.DISABLED
    )

    override fun getConfigProperties() = listOf(
        ProviderConfigProperty(
            SITE_KEY,
            "Site Key",
            "Your Friendly Captcha site key",
            ProviderConfigProperty.STRING_TYPE,
            null,
            false,
            true
        ),
        ProviderConfigProperty(
            SECRET_KEY,
            "Secret Key",
            "Your Friendly Captcha secret key",
            ProviderConfigProperty.STRING_TYPE,
            null,
            true,
            true
        ),
        ProviderConfigProperty(
            API_DOMAIN,
            "Friendly Captcha API domain",
            "The domain of the Friendly Captcha API",
            ProviderConfigProperty.STRING_TYPE,
            "https://api.friendlycaptcha.com",
            false,
            true
        ),
        ProviderConfigProperty(
            FORM_CAPTCHA_SOLUTION,
            "Form field name for captcha solution",
            "The name of the form field that will contain the captcha solution",
            ProviderConfigProperty.STRING_TYPE,
            "frc-captcha-solution",
            false,
            true
        ),
        ProviderConfigProperty(
            FAIL_ON_ERROR,
            "Fail on HTTP status error",
            "Whether to fail if the HTTP status code is an error. According to the Friendly Captcha best practices we allow the user to continue on failure, unless this value is true.",
            ProviderConfigProperty.BOOLEAN_TYPE,
            "false",
            false,
            true
        )
    )

    override fun buildPage(
        context: FormContext,
        form: LoginFormsProvider
    ) {
        LOGGER.trace("Building page with Friendly Captcha")
        val config = context.authenticatorConfig.config
        if (config == null) {
            form.addError(FormMessage(null, "recaptchaNotConfigured"))
        } else if (!validateConfig(config)) {
            form.addError(FormMessage(null, "recaptchaNotConfigured"))
        } else {
            val userLanguageTag = context.session.context.resolveLocale(context.user).toLanguageTag()
            val themeRoot = Urls.themeRoot(context.uriInfo.baseUri)
            form.setAttribute("friendlyCaptchaRequired", true)
            form.setAttribute("friendlyCaptchaSiteKey", config[SITE_KEY])
            form.setAttribute("friendlyCaptchaLang", userLanguageTag)
            form.setAttribute("friendlyCaptchaFormAttribute", config[FORM_CAPTCHA_SOLUTION])
            form.setAttribute("friendlyCaptchaApiDomain", config[API_DOMAIN])
            form.addScript("${themeRoot.path}/login/base/js/widget.min.js")
            form.addScript("${themeRoot.path}/login/base/js/widget.module.min.js")
        }
    }

    fun validateConfig(config: Map<String, String>): Boolean {
        return !(StringUtil.isNullOrEmpty(config[SITE_KEY])
                || StringUtil.isNullOrEmpty(config[SECRET_KEY]))
                || StringUtil.isNullOrEmpty(config[API_DOMAIN])
                || StringUtil.isNullOrEmpty(config[FORM_CAPTCHA_SOLUTION])
    }

    override fun validate(context: ValidationContext) {
        val formData = context.httpRequest.decodedFormParameters
        val config = context.authenticatorConfig.config
        val captchaResponse = formData.getFirst(config[FORM_CAPTCHA_SOLUTION])
        val httpClient = context.session.getProvider(HttpClientProvider::class.java).httpClient
        LOGGER.trace("Got captcha: $captchaResponse")
        if (!Validation.isBlank(captchaResponse)
            && httpClient != null
            && validate(captchaResponse, config, httpClient)
        ) {
            context.success()
        } else {
            val errors = listOf(FormMessage(null, "recaptchaFailed"))
            formData.remove(FORM_CAPTCHA_SOLUTION)
            context.validationError(formData, errors)
            context.excludeOtherErrors()
        }
    }

    fun validate(
        captcha: String,
        config: Map<String, String>,
        httpClient: HttpClient
    ): Boolean {
        val apiUrl = "${config[API_DOMAIN]}/api/v1/siteverify"
        val post = HttpPost(apiUrl)
        val formData = listOf(
            BasicNameValuePair("solution", captcha),
            BasicNameValuePair("secret", config[SECRET_KEY]),
            BasicNameValuePair("sitekey", config[SITE_KEY])
        )

        post.entity = UrlEncodedFormEntity(formData)
        post.setHeader("Content-Type", "application/x-www-form-urlencoded")

        return try {
            val response = httpClient.execute(post)
            val entity = response.entity
            val responseString = EntityUtils.toString(entity)
            val jsonResponse = JsonParser.parseString(responseString).asJsonObject
            val errors = jsonResponse.getAsJsonArray("errors")

            if (jsonResponse.get("success").asBoolean) {
                LOGGER.trace("Captcha validation successful")
                true
            } else if (response.statusLine.statusCode >= 400 && config[FAIL_ON_ERROR] == "false") {
                LOGGER.error("Captcha validation failed with HTTP status code ${response.statusLine.statusCode}, errors: $errors, but allowing user to continue. You should look into this.")
                true
            } else {
                LOGGER.warn("Captcha validation failed with errors: $errors")
                false
            }
        } catch (e: Exception) {
            LOGGER.error("Error validating captcha", e)
            false
        }
    }

    override fun success(context: FormContext) {
        // Nothing to do
    }

    override fun setRequiredActions(
        session: KeycloakSession?,
        realm: RealmModel?,
        user: UserModel?
    ) {
        // Nothing to do
    }

    override fun init(p0: Config.Scope?) {
        // Nothing to do
    }

    override fun postInit(p0: KeycloakSessionFactory?) {
        // Nothing to do
    }

    override fun close() {
        // Nothing to do
    }
}