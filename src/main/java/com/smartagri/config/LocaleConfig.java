package com.smartagri.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

/**
 * Configures Spring's i18n infrastructure for the advisory message system.
 *
 * <h3>Locale resolution</h3>
 * {@link AcceptHeaderLocaleResolver} reads the {@code Accept-Language} HTTP
 * header on web requests.  For background (non-HTTP) threads — e.g. the
 * scheduled advisory generator — callers must supply a {@link Locale}
 * directly when calling
 * {@link MessageSource#getMessage(String, Object[], Locale)}.
 *
 * <h3>Supported locales</h3>
 * <ul>
 *   <li>{@code en} — English (default)</li>
 *   <li>{@code hi} — Hindi</li>
 *   <li>{@code mr} — Marathi</li>
 * </ul>
 *
 * <h3>Bundle location</h3>
 * {@code classpath:i18n/messages} resolves to:
 * <ul>
 *   <li>{@code src/main/resources/i18n/messages.properties}</li>
 *   <li>{@code src/main/resources/i18n/messages_hi.properties}</li>
 *   <li>{@code src/main/resources/i18n/messages_mr.properties}</li>
 * </ul>
 */
@Configuration
public class LocaleConfig {

    /**
     * Resolves the locale from the {@code Accept-Language} HTTP header.
     *
     * <p>Falls back to {@link Locale#ENGLISH} when the header is absent or
     * specifies an unsupported locale.  Supported locales are declared
     * explicitly so Spring only accepts valid language tags.
     */
    @Bean
    public AcceptHeaderLocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.ENGLISH);
        resolver.setSupportedLocales(List.of(
                Locale.ENGLISH,
                Locale.forLanguageTag("hi"),
                Locale.forLanguageTag("mr")
        ));
        return resolver;
    }

    /**
     * {@link ReloadableResourceBundleMessageSource} that loads
     * {@code classpath:i18n/messages*.properties}.
     *
     * <p>Key configuration choices:
     * <ul>
     *   <li>{@code setDefaultEncoding("UTF-8")} — required for Hindi/Marathi
     *       characters stored as Unicode escapes in .properties files.</li>
     *   <li>{@code setCacheSeconds(3600)} — bundles are re-read from disk at
     *       most once per hour, enabling hot-reload in dev without restarting
     *       the JVM.</li>
     *   <li>{@code setFallbackToSystemLocale(false)} — prevents accidental
     *       fall-through to OS locale; always uses English default bundle.</li>
     * </ul>
     */
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource source =
                new ReloadableResourceBundleMessageSource();
        source.setBasename("classpath:i18n/messages");
        source.setDefaultEncoding("UTF-8");
        source.setCacheSeconds(3600);
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
