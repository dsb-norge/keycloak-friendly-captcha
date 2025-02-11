package no.dsb.keycloak.keycloak_friendly_captcha.wrappers

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.keycloak.authentication.FormContext
import org.keycloak.common.Version
import org.keycloak.forms.login.LoginFormsProvider
import org.keycloak.models.KeycloakContext
import org.keycloak.models.KeycloakSession
import org.keycloak.models.KeycloakUriInfo
import org.keycloak.models.UserModel
import org.mockito.Mock
import org.mockito.Mockito.*
import java.net.URI
import java.util.*

class FormContextWrapperTest {

    @Mock
    private val formContext = mock(FormContext::class.java)
    @Mock
    private val uriInfo = mock(KeycloakUriInfo::class.java)
    @Mock
    private val keycloakSession = mock(KeycloakSession::class.java)
    @Mock
    private val keycloakContext = mock(KeycloakContext::class.java)
    @Mock
    private val user = mock(UserModel::class.java)
    @Mock
    private val loginFormsProvider = mock(LoginFormsProvider::class.java)

    @BeforeEach
    fun beforeEach() {
        reset(
            formContext,
            uriInfo,
            keycloakSession,
            keycloakContext,
            user,
            loginFormsProvider
        )
        `when`(formContext.getUriInfo()).thenReturn(uriInfo)
        `when`(formContext.getSession()).thenReturn(keycloakSession)
        `when`(formContext.getUser()).thenReturn(user)
        `when`(keycloakSession.getContext()).thenReturn(keycloakContext)
        `when`(keycloakContext.resolveLocale(any(UserModel::class.java))).thenReturn(Locale.ENGLISH)
        `when`(uriInfo.delegate).thenReturn(uriInfo)
        `when`(uriInfo.baseUri).thenReturn(URI.create("https://example.com"))
    }

    @Test
    fun `should initialize a valid v1 form`() {
        val config = mock(ConfigWrapper::class.java).apply {
            `when`(useVersion2()).thenReturn(false)
            `when`(getSiteKey()).thenReturn("test-site-key")
            `when`(getFormCaptchaSolution()).thenReturn("custom-captcha-response")
            `when`(getApiDomain()).thenReturn("https://custom.friendlycaptcha.com")
        }
        val form = spy(LoginFormsProviderWrapper(loginFormsProvider))
        val wrapper = FormContextWrapper(formContext)


        wrapper.initializeFriendlyCaptcha(config, form)


        verify(form, times(1)).setAttribute("friendlyCaptchaRequired", true)
        verify(form, times(1)).setAttribute("friendlyCaptchaSiteKey", "test-site-key")
        verify(form, times(1)).setAttribute("friendlyCaptchaLang", "en")
        verify(form, times(1)).setAttribute("friendlyCaptchaFormAttribute", "custom-captcha-response")
        verify(form, times(1)).setAttribute("friendlyCaptchaApiDomain", "https://custom.friendlycaptcha.com")
        verify(form, times(1)).setAttribute("friendlyCaptchaV2", false)
        verify(form, times(1)).addScript("/resources/${Version.VERSION}/login/base/js/widget.min.js")
        verify(form, times(1)).addScript("/resources/${Version.VERSION}/login/base/js/widget.module.min.js")
    }

    @Test
    fun initializeFriendlyCaptcha_WithValidConfigV2() {
        val config = mock(ConfigWrapper::class.java).apply {
            `when`(useVersion2()).thenReturn(true)
            `when`(getSiteKey()).thenReturn("test-site-key")
            `when`(getFormCaptchaSolution()).thenReturn("custom-captcha-response")
            `when`(getApiDomain()).thenReturn("https://custom.friendlycaptcha.com")
        }
        val form = spy(LoginFormsProviderWrapper(loginFormsProvider))
        val wrapper = FormContextWrapper(formContext)


        wrapper.initializeFriendlyCaptcha(config, form)


        verify(form, times(1)).setAttribute("friendlyCaptchaRequired", true)
        verify(form, times(1)).setAttribute("friendlyCaptchaSiteKey", "test-site-key")
        verify(form, times(1)).setAttribute("friendlyCaptchaLang", "en")
        verify(form, times(1)).setAttribute("friendlyCaptchaFormAttribute", "custom-captcha-response")
        verify(form, times(1)).setAttribute("friendlyCaptchaApiDomain", "https://custom.friendlycaptcha.com")
        verify(form, times(1)).setAttribute("friendlyCaptchaV2", true)
        verify(form, times(1)).addScript("/resources/${Version.VERSION}/login/base/js/widget.v2.min.js")
        verify(form, times(1)).addScript("/resources/${Version.VERSION}/login/base/js/widget.v2.module.min.js")
    }
}