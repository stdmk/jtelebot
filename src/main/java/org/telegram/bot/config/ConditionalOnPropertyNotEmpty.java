package org.telegram.bot.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.Properties;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(ConditionalOnPropertyNotEmpty.OnPropertyNotEmptyCondition.class)
@PropertySource(value = "file:properties.properties", ignoreResourceNotFound = true)
public @interface ConditionalOnPropertyNotEmpty {
    String[] value();

    class OnPropertyNotEmptyCondition implements Condition {
        @Override
        public boolean matches(@NotNull ConditionContext context, @NotNull AnnotatedTypeMetadata metadata) {
            Properties properties = new Properties();
            try (FileInputStream fileInputStream = new FileInputStream("properties.properties")) {
                properties.load(fileInputStream);
            } catch (IOException e) {
                return false;
            }

            Map<String, Object> attrs = metadata.getAnnotationAttributes(ConditionalOnPropertyNotEmpty.class.getName());
            if (attrs == null) {
                return false;
            }

            String[] propertyNames = (String[]) attrs.get("value");

            boolean condition = true;
            for (String propertyName : propertyNames) {
                String value = properties.getProperty(propertyName);
                condition = value != null && !value.trim().isEmpty();

                if (!condition) {
                    return condition;
                }
            }

            return condition;
        }
    }
}
