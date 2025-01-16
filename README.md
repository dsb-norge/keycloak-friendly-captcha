# Keycloak Friendly Captcha
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=dsb-norge_keycloak-friendly-captcha&metric=alert_status&token=f790a4addd21ddf94a8d01c19520950ec2bd9b30)](https://sonarcloud.io/summary/new_code?id=dsb-norge_keycloak-friendly-captcha)

A Keycloak authentication plugin that integrates [Friendly Captcha](https://friendlycaptcha.com/) into the registration flow. Friendly Captcha is a privacy-friendly, GDPR-compliant alternative to reCAPTCHA that respects user privacy while effectively preventing spam and abuse.

## Features

- Easy integration with Keycloak registration flow
- Privacy-focused CAPTCHA solution
- Configurable API endpoint
- Customizable form field names
- Optional failure handling for API errors

## Prerequisites

- Keycloak 26.1.0 or later (will most likely work with earlier versions)
- Java 17 or later
- A Friendly Captcha account with site key and secret key

## Installation

1. Build the plugin:
   ```bash
   mvn clean package
   ```

2. Copy the generated JAR file from `target/keycloak-friendly-captcha-1.0-SNAPSHOT.jar` to Keycloak's `providers` directory.

3. Restart Keycloak to load the plugin.

## Configuration

### 1. Add Authentication Execution

1. In the Keycloak admin console, go to Authentication
2. Select the "Registration flow" (You probably need to duplicate it, if its the built in one)
3. Click "+" button next to the "Registration form" step
4. On the popup click "Add step"
5. Select "Friendly Captcha" from the list
6. Configure all the values for the plugin  (See below)
7. Set the requirement to "Required"
8. Make sure that the flow you have modified is bound to the "Registration flow"
9. The Friendly Captcha should now show up in the registration form (Provided you have added the necessary HTML to the registration form)

### 2. Configure the Execution

Click the gear icon next to the Friendly Captcha execution and configure:

- **Site Key**: Your Friendly Captcha site key
- **Secret Key**: Your Friendly Captcha secret key (API Key)
- **API Domain**: The Friendly Captcha API domain (default: https://api.friendlycaptcha.com)
- **Form field name**: The name of the form field for the captcha solution (default: frc-captcha-solution)
- **Fail on HTTP status error**: Whether to fail on API errors (default: false)

### 3. Add to template file `register.ftl`

Add the following code to the `register.ftl` file in your theme to render the Friendly Captcha widget:

```html
<#if friendlyCaptchaRequired??>
  <div
    class="frc-captcha"
    data-sitekey="${friendlyCaptchaSiteKey}"
    data-start="focus"
    data-lang="${friendlyCaptchaLang}"
    data-solution-field-name="${friendlyCaptchaFormAttribute}"
    data-puzzle-endpoint="${friendlyCaptchaApiDomain}/api/v1/puzzle"
  ></div>
</#if>
```

#### Note about *Fail on HTTP status error*: 

See Friendly Captcha [Best Practices](https://developer.friendlycaptcha.com/docs/v1/getting-started/verify#verification-best-practices)
So if this value is false, the user will be allowed to continue even if the API returns an error. If it is true, the user will be blocked from continuing.

## Development

For local development, you can use the provided Docker Compose file:

```bash
mvn clean package
docker-compose up
```

This will start Keycloak with the plugin pre-installed at http://localhost:8080.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.