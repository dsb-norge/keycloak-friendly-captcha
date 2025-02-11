package no.dsb.keycloak.keycloak_friendly_captcha

import no.dsb.keycloak.keycloak_friendly_captcha.wrappers.*
import org.jboss.logging.Logger
import org.keycloak.Config
import org.keycloak.authentication.FormAction
import org.keycloak.authentication.FormActionFactory
import org.keycloak.authentication.FormContext
import org.keycloak.authentication.ValidationContext
import org.keycloak.connections.httpclient.HttpClientProvider
import org.keycloak.forms.login.LoginFormsProvider
import org.keycloak.models.*
import org.keycloak.provider.ProviderConfigProperty
import org.keycloak.services.validation.Validation

class RegistrationFriendlyCaptcha : FormAction, FormActionFactory {

    companion object {
        private val LOGGER = Logger.getLogger(RegistrationFriendlyCaptcha::class.java)

        const val PROVIDER_ID = "friendly-captcha"
        private const val DISPLAY_NAME = "Friendly Captcha"
    }

    override fun getId() = PROVIDER_ID
    override fun getDisplayType() = DISPLAY_NAME
    override fun getHelpText() = "Validates that the user is human using Friendly Captcha v1 or v2"
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
            ConfigWrapper.SITE_KEY,
            "Site Key",
            "Your Friendly Captcha site key",
            ProviderConfigProperty.STRING_TYPE,
            null,
            false,
            true
        ),
        ProviderConfigProperty(
            ConfigWrapper.SECRET_KEY,
            "Secret Key",
            "Your Friendly Captcha secret key",
            ProviderConfigProperty.STRING_TYPE,
            null,
            true,
            true
        ),
        ProviderConfigProperty(
            ConfigWrapper.API_DOMAIN,
            "Friendly Captcha API domain",
            "The domain of the Friendly Captcha API. V1 https://api.friendlycaptcha.com or V2 https://global.frcapi.com (or EU endpoint for the correct version)",
            ProviderConfigProperty.STRING_TYPE,
            "https://global.frcapi.com",
            false,
            true
        ),
        ProviderConfigProperty(
            ConfigWrapper.FORM_CAPTCHA_SOLUTION,
            "Form field name for captcha solution",
            "The name of the form field that will contain the captcha solution",
            ProviderConfigProperty.STRING_TYPE,
            "frc-captcha-response",
            false,
            true
        ),
        ProviderConfigProperty(
            ConfigWrapper.FAIL_ON_ERROR,
            "Fail on HTTP status error",
            "Whether to fail if the HTTP status code is an error. According to the Friendly Captcha best practices we allow the user to continue on failure, unless this value is true.",
            ProviderConfigProperty.BOOLEAN_TYPE,
            "false",
            false,
            true
        ),
        ProviderConfigProperty(
            ConfigWrapper.VERSION,
            "Use Friendly Captcha v2",
            "Whether to use Friendly Captcha v2. If false, v1 will be used.",
            ProviderConfigProperty.BOOLEAN_TYPE,
            "true",
            false,
            true
        )
    )

    override fun buildPage(
        context: FormContext,
        form: LoginFormsProvider
    ) {
        val formContextWrapper = FormContextWrapper(context)
        val loginFormsProviderWrapper = LoginFormsProviderWrapper(form)
        val config = ConfigWrapper(context.authenticatorConfig.config)
        LOGGER.trace("Building page with Friendly Captcha")
        if (!config.validateConfig()) {
            loginFormsProviderWrapper.addError("recaptchaNotConfigured")
        } else {
            formContextWrapper.initializeFriendlyCaptcha(config, loginFormsProviderWrapper)
        }
    }

    override fun validate(context: ValidationContext) {
        val config = ConfigWrapper(context.authenticatorConfig.config)
        val contextWrapper = ValidationContextWrapper(context, config)
        val captchaResponse = contextWrapper.captchaResponse
        val httpClientWrapper = HttpClientWrapper(
            httpClient = context.session.getProvider(HttpClientProvider::class.java).httpClient,
            config = config
        )
        validate(captchaResponse, httpClientWrapper, contextWrapper)
    }

    fun validate(
        captchaResponse: String,
        httpClientWrapper: HttpClientWrapper,
        contextWrapper: ValidationContextWrapper
    ) {
        LOGGER.trace("Got captcha: $captchaResponse")
        if (!Validation.isBlank(captchaResponse) && httpClientWrapper.verifyFriendlyCaptcha(captchaResponse)) {
            LOGGER.info("Captcha validation successful")
            contextWrapper.triggerSuccess()
        } else {
            LOGGER.warn("Captcha validation failed")
            contextWrapper.triggerError("recaptchaFailed")
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