package org.telegram.bot.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

@RequiredArgsConstructor
@Configuration
public class HttpStatusesPropertiesLoader {

    private final ConfigurableEnvironment environment;

    @PostConstruct
    public void postConstruct() throws IOException {
        File file = new File("./http_statuses.properties");

        if (file.exists()) {
            Properties properties = new Properties();
            try (InputStreamReader reader = new InputStreamReader(
                    new FileInputStream(file), StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
            PropertiesPropertySource propertySource = new PropertiesPropertySource("httpStatuses", properties);
            environment.getPropertySources().addLast(propertySource);
        }
    }
}
