package no.dsb.keycloak.keycloak_friendly_captcha

import jakarta.ws.rs.core.MultivaluedHashMap
import no.dsb.keycloak.keycloak_friendly_captcha.client.CaptchaHttpClient
import no.dsb.keycloak.keycloak_friendly_captcha.wrappers.ConfigWrapper
import no.dsb.keycloak.keycloak_friendly_captcha.wrappers.HttpClientWrapper
import no.dsb.keycloak.keycloak_friendly_captcha.wrappers.ValidationContextWrapper
import org.apache.http.message.BasicNameValuePair
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.keycloak.authentication.FormContext
import org.keycloak.authentication.ValidationContext
import org.keycloak.forms.login.LoginFormsProvider
import org.keycloak.http.HttpRequest
import org.keycloak.models.*
import org.keycloak.models.utils.FormMessage
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.net.URI
import java.util.*

class RegistrationFriendlyCaptchaTest {

    @Mock
    private val formContext = mock(FormContext::class.java)
    @Mock
    private val loginFormsProvider = mock(LoginFormsProvider::class.java)
    @Mock
    private val authenticatorConfigModel = mock(AuthenticatorConfigModel::class.java)
    @Mock
    private val keycloakSession = mock(KeycloakSession::class.java)
    @Mock
    private val keycloakContext = mock(KeycloakContext::class.java)
    @Mock
    private val user = mock(UserModel::class.java)
    @Mock
    private val uriInfo = mock(KeycloakUriInfo::class.java)
    @Mock
    private val validationContext = mock(ValidationContext::class.java)
    @Mock
    private val httpRequest = mock(HttpRequest::class.java)

    @Captor
    private lateinit var formMessageCaptor: ArgumentCaptor<FormMessage>

    private var config = mutableMapOf<String, String>()
    private var responseForm = mutableMapOf<String, String>()

    @BeforeEach
    fun setUp() {
        config = mutableMapOf()
        responseForm = mutableMapOf()

        MockitoAnnotations.openMocks(this)
        `when`(formContext.getUriInfo()).thenReturn(uriInfo)
        `when`(formContext.getSession()).thenReturn(keycloakSession)
        `when`(formContext.getUser()).thenReturn(user)
        `when`(keycloakSession.getContext()).thenReturn(keycloakContext)
        `when`(keycloakContext.resolveLocale(any(UserModel::class.java))).thenReturn(Locale.ENGLISH)
        `when`(uriInfo.delegate).thenReturn(uriInfo)
        `when`(uriInfo.baseUri).thenReturn(URI.create("https://example.com"))
        `when`(formContext.authenticatorConfig).thenReturn(authenticatorConfigModel)
        `when`(authenticatorConfigModel.config).thenReturn(config)
        `when`(validationContext.httpRequest).thenReturn(httpRequest)
        `when`(httpRequest.decodedFormParameters).thenReturn(MultivaluedHashMap(responseForm))
    }

    @Test
    fun `should fail buildPage if missing config`() {
        val action = RegistrationFriendlyCaptcha()

        action.buildPage(formContext, loginFormsProvider)

        verify(loginFormsProvider, times(1)).addError(formMessageCaptor.capture())
        assertThat(formMessageCaptor.value.message).isEqualTo("recaptchaNotConfigured")
        verify(loginFormsProvider, times(0)).setAttribute(anyString(), any())
        verify(loginFormsProvider, times(0)).addScript(anyString())
    }

    @Test
    fun `should buildPage if config is present`() {
        config[ConfigWrapper.SITE_KEY] = "siteKey"
        config[ConfigWrapper.SECRET_KEY] = "secretKey"
        config[ConfigWrapper.API_DOMAIN] = "apiDomain"
        config[ConfigWrapper.FORM_CAPTCHA_SOLUTION] = "formCaptchaSolution"
        config[ConfigWrapper.FAIL_ON_ERROR] = "true"
        config[ConfigWrapper.VERSION] = "true"

        val action = RegistrationFriendlyCaptcha()

        action.buildPage(formContext, loginFormsProvider)

        verify(loginFormsProvider, times(6)).setAttribute(anyString(), any())
        verify(loginFormsProvider, times(2)).addScript(anyString())
    }

    @Test
    fun `should be able to validate captcha response`() {
        config[ConfigWrapper.FORM_CAPTCHA_SOLUTION] = "frc-captcha-response"
        config[ConfigWrapper.API_DOMAIN] = "https://api.friendlycaptcha.com"
        config[ConfigWrapper.SITE_KEY] = "siteKey"
        config[ConfigWrapper.SECRET_KEY] = "topSecret"

        responseForm["frc-captcha-response"] = "captchaResponse"

        val config = ConfigWrapper(config)
        val context = ValidationContextWrapper(validationContext, config)
        val httpClient = HttpClientWrapper(
            httpClient(200, """{"success": true}"""),
            config
        )

        val action = RegistrationFriendlyCaptcha()

        action.validate("captchaResponse", httpClient, context)

        verify(validationContext, times(1)).success()
    }

    @Test
    fun `should fail on blank captcha response`() {
        config[ConfigWrapper.FORM_CAPTCHA_SOLUTION] = "frc-captcha-response"
        config[ConfigWrapper.API_DOMAIN] = "https://api.friendlycaptcha.com"
        config[ConfigWrapper.SITE_KEY] = "siteKey"
        config[ConfigWrapper.SECRET_KEY] = "topSecret"

        responseForm["frc-captcha-response"] = ""

        val config = ConfigWrapper(config)
        val context = ValidationContextWrapper(validationContext, config)
        val httpClient = HttpClientWrapper(
            httpClient(200, """{"success": true}"""),
            config
        )

        val action = RegistrationFriendlyCaptcha()

        action.validate("", httpClient, context)

        verify(validationContext, times(1)).validationError(any(), any())
        verify(validationContext, times(1)).excludeOtherErrors()
        verify(validationContext, times(0)).success()
    }

    @Test
    fun `should fail on non-success from captcha`() {
        config[ConfigWrapper.FORM_CAPTCHA_SOLUTION] = "frc-captcha-response"
        config[ConfigWrapper.API_DOMAIN] = "https://api.friendlycaptcha.com"
        config[ConfigWrapper.SITE_KEY] = "siteKey"
        config[ConfigWrapper.SECRET_KEY] = "topSecret"

        responseForm["frc-captcha-response"] = "captchaResponse"

        val config = ConfigWrapper(config)
        val context = ValidationContextWrapper(validationContext, config)
        val httpClient = HttpClientWrapper(
            httpClient(200, """{"success": false}"""),
            config
        )

        val action = RegistrationFriendlyCaptcha()

        action.validate("captchaResponse", httpClient, context)

        verify(validationContext, times(1)).validationError(any(), any())
        verify(validationContext, times(1)).excludeOtherErrors()
        verify(validationContext, times(0)).success()
    }

    private fun httpClient(
        statusCode: Int,
        responseString: String,
        asserter: (api: String, headers: Map<String, String>, formData: List<BasicNameValuePair>) -> Unit = { _, _, _ -> }
    ): CaptchaHttpClient {
        return object : CaptchaHttpClient {
            override fun executePost(url: String, headers: Map<String, String>, formData: List<BasicNameValuePair>): Pair<Int, String> {
                asserter(url, headers, formData)
                return statusCode to responseString
            }
        }
    }
}