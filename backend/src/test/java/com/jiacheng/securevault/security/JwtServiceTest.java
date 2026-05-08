package com.jiacheng.securevault.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String EXPECTED_MESSAGE =
            "JWT_SECRET is required and must be at least 64 bytes. Run scripts/setup.ps1 or configure it in production secrets.";

    @Test
    void shouldFailWithClearMessageWhenJwtSecretIsBlank() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(" ");

        assertThatThrownBy(() -> new JwtService(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(EXPECTED_MESSAGE);
    }

    @Test
    void shouldFailWithClearMessageWhenJwtSecretIsTooShort() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("too-short");

        assertThatThrownBy(() -> new JwtService(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(EXPECTED_MESSAGE);
    }
}
