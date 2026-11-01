package com.lapin.bodymechanic.config;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards against committing credentials again: every secret in application.properties
 * must be a ${PLACEHOLDER} resolved from the environment.
 */
class NoHardcodedSecretsTest {

    private static final Properties PROPS = new Properties();

    @BeforeAll
    static void load() throws Exception {
        try (InputStream in = new ClassPathResource("application.properties").getInputStream()) {
            PROPS.load(in);
        }
    }

    private static void assertPlaceholder(String key) {
        String value = PROPS.getProperty(key);
        assertNotNull(value, key + " must be defined");
        assertTrue(value.startsWith("${") && value.endsWith("}"),
                key + " must come from the environment, not a literal value");
    }

    @Test
    void smtpPasswordIsNotHardcoded() {
        assertPlaceholder("spring.mail.password");
    }

    @Test
    void smtpUsernameIsNotHardcoded() {
        assertPlaceholder("spring.mail.username");
    }

    @Test
    void recaptchaSecretIsNotHardcoded() {
        assertPlaceholder("recaptcha.secret");
    }

    @Test
    void databasePasswordHasNoLiteralDefault() {
        String value = PROPS.getProperty("spring.datasource.password");
        assertNotNull(value);
        assertFalse(value.matches("\\$\\{[^:}]+:.+}"),
                "spring.datasource.password must not have a default value");
    }

    @Test
    void mailDebugIsOffByDefault() {
        String value = PROPS.getProperty("mail.debug");
        assertTrue("false".equals(value) || (value != null && value.matches("\\$\\{[^:}]+:false}")));
    }

    @Test
    void baseUrlIsConfigurable() {
        assertNotNull(PROPS.getProperty("app.base-url"), "app.base-url must be defined");
    }
}
