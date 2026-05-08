package com.jiacheng.securevault.config;

import jakarta.servlet.Filter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractJacksonHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class EncodingConfig implements WebMvcConfigurer {

    public static final MediaType APPLICATION_JSON_UTF8 =
            new MediaType("application", "json", StandardCharsets.UTF_8);

    @Bean
    public FilterRegistrationBean<Filter> characterEncodingFilterRegistration() {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        filter.setEncoding(StandardCharsets.UTF_8.name());
        filter.setForceEncoding(true);

        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Override
    public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
        builder.configureMessageConverters(this::configureConverter);
    }

    private void configureConverter(HttpMessageConverter<?> converter) {
        if (converter instanceof StringHttpMessageConverter stringConverter) {
            stringConverter.setDefaultCharset(StandardCharsets.UTF_8);
        }
        if (converter instanceof AbstractJacksonHttpMessageConverter<?> jacksonConverter) {
            jacksonConverter.setDefaultCharset(StandardCharsets.UTF_8);
            List<MediaType> mediaTypes = new ArrayList<>(jacksonConverter.getSupportedMediaTypes());
            mediaTypes.remove(APPLICATION_JSON_UTF8);
            mediaTypes.add(0, APPLICATION_JSON_UTF8);
            jacksonConverter.setSupportedMediaTypes(mediaTypes);
        }
    }

    /*
     * Kept for test slices and third-party MVC customizers that still expose the
     * converter list directly. Spring MVC calls the builder method above.
     */
    @Override
    @SuppressWarnings("removal")
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        for (HttpMessageConverter<?> converter : converters) {
            configureConverter(converter);
        }
    }
}
