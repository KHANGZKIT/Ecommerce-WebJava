package com.ecommerce.orderservice.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class JacksonConfig {

    @Bean
    public SimpleModule stringTrimmerModule() {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(String.class, new StringTrimmerDeserializer());
        return module;
    }

    public static class StrictIntegerDeserializer extends JsonDeserializer<Integer> {
        @Override
        public Integer deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonToken token = p.currentToken();
            if (token == JsonToken.VALUE_NUMBER_FLOAT) {
                throw MismatchedInputException.from(p, Integer.class,
                        "Fractional numbers are not allowed for integer fields: " + p.getText());
            }
            if (token == JsonToken.VALUE_NUMBER_INT) {
                try {
                    return p.getIntValue();
                } catch (Exception ex) {
                    throw MismatchedInputException.from(p, Integer.class,
                            "Integer value out of range: " + p.getText());
                }
            }
            if (token == JsonToken.VALUE_NULL) {
                return null;
            }
            throw MismatchedInputException.from(p, Integer.class,
                    "Expected an integer value but got: " + token.name().toLowerCase().replace('_', ' '));
        }
    }

    public static class StringTrimmerDeserializer extends StdScalarDeserializer<String> implements ContextualDeserializer {
        private final boolean shouldTrim;

        public StringTrimmerDeserializer() {
            this(true);
        }

        public StringTrimmerDeserializer(boolean shouldTrim) {
            super(String.class);
            this.shouldTrim = shouldTrim;
        }

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            // Reject non-string JSON tokens (numbers, booleans, arrays, objects)
            // Only accept VALUE_STRING and VALUE_NULL tokens
            JsonToken token = p.currentToken();
            if (token != JsonToken.VALUE_STRING && token != JsonToken.VALUE_NULL) {
                throw MismatchedInputException.from(p, String.class,
                    "Expected a JSON string value but got: " + token.name().toLowerCase().replace('_', ' ') +
                    ". Please wrap the value in quotes.");
            }

            String value = p.getText();
            if (value == null) return null;
            return shouldTrim ? value.trim() : value;
        }

        @Override
        public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
            if (property != null) {
                // Skip trimming if @NoTrim annotation is present (Authoritative)
                if (property.getAnnotation(NoTrim.class) != null) {
                    return new StringTrimmerDeserializer(false);
                }

                // Convenience layer: Skip trimming for common sensitive field names
                String name = property.getName().toLowerCase();
                boolean isSensitive = name.contains("password") ||
                                    name.contains("passphrase") ||
                                    name.contains("secret") ||
                                    name.contains("apikey") ||
                                    name.contains("token") ||
                                    name.contains("credential");

                if (isSensitive) {
                    return new StringTrimmerDeserializer(false);
                }
            }
            return this;
        }
    }
}
