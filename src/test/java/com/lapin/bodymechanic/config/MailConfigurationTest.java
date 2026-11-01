package com.lapin.bodymechanic.config;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The project must not depend on any particular mail provider: the default is a local
 * catch-all SMTP server (Mailpit), everything else comes from the environment.
 */
class MailConfigurationTest {

    private static final Properties PROPS = new Properties();

    @BeforeAll
    static void load() throws Exception {
        try (InputStream in = new ClassPathResource("application.properties").getInputStream()) {
            PROPS.load(in);
        }
    }

    private static String read(String file) throws Exception {
        return new String(Files.readAllBytes(Paths.get(file)), StandardCharsets.UTF_8);
    }

    @Test
    void noMailProviderIsNamedInTheProject() throws Exception {
        Path props = Paths.get("src/main/resources/application.properties");
        for (Path p : new Path[]{props, Paths.get(".env.example"), Paths.get("README.md"),
                Paths.get("docker-compose.yml")}) {
            String text = new String(Files.readAllBytes(p), StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
            assertFalse(text.contains("yandex") || text.contains("ya.ru"), p + " must not mention Yandex");
        }
    }

    @Test
    void defaultSmtpHostIsLocal() {
        assertEquals("${SPRING_MAIL_HOST:localhost}", PROPS.getProperty("spring.mail.host"));
    }

    @Test
    void defaultSmtpPortIsMailpitPort() {
        assertEquals("${SPRING_MAIL_PORT:1025}", PROPS.getProperty("spring.mail.port"));
    }

    @Test
    void defaultProtocolIsPlainSmtp_notSmtps() {
        String protocol = PROPS.getProperty("spring.mail.protocol", "smtp");
        assertFalse(protocol.contains("smtps"), "plain smtp by default, tls providers are configured via env");
    }

    @Test
    void credentialsAreOptional() {
        assertTrue(PROPS.getProperty("spring.mail.username").matches("\\$\\{[^:}]+:}"),
                "username must default to empty so a local SMTP server works without auth");
        assertTrue(PROPS.getProperty("spring.mail.password").matches("\\$\\{[^:}]+:}"),
                "password must default to empty");
    }

    @Test
    void senderAddressIsConfigurableAndIndependentOfTheSmtpLogin() {
        String from = PROPS.getProperty("app.mail.from");
        assertNotNull(from, "app.mail.from must be defined");
        assertTrue(from.contains("@"));
    }

    @Test
    void dockerComposeProvidesAMailCatcher() throws Exception {
        String compose = read("docker-compose.yml");
        assertTrue(compose.contains("mailpit"), "docker-compose.yml must have a mailpit service");
        assertTrue(compose.contains("127.0.0.1:1025:1025"), "SMTP port must be bound to localhost only");
        assertTrue(compose.contains("127.0.0.1:8025:8025"), "web UI port must be bound to localhost only");
    }
}
