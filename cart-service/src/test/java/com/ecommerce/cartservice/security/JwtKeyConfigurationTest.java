package com.ecommerce.cartservice.security;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.io.Encoders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import static org.assertj.core.api.Assertions.assertThat;

class JwtKeyConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(CartJwtVerifier.class).withPropertyValues("jwt.expiration=1800000");

    @Test
    void missingKeyFailsBeanStartup() {
        runner.run(context -> assertThat(context).hasFailed());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not!base64", "YQ=="})
    void invalidKeyFailsBeanStartup(String secret) {
        runner.withPropertyValues("jwt.secret=" + secret).run(context -> assertThat(context).hasFailed());
    }

    @Test
    void validExternalKeyStartsSecurityBean() {
        String secret = Encoders.BASE64.encode(Keys.secretKeyFor(SignatureAlgorithm.HS256).getEncoded());
        runner.withPropertyValues("jwt.secret=" + secret).run(context ->
                assertThat(context).hasNotFailed().hasSingleBean(CartJwtVerifier.class));
    }

    @Test
    void applicationConfigurationHasNoSecretFallback() {
        var yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        assertThat(yaml.getObject().getProperty("jwt.secret")).isEqualTo("${JWT_SECRET}");
    }
}
