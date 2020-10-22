package org.telegram.bot.services.impl;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.bot.services.PropertiesService;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

@AllArgsConstructor
@Service
public class PropertiesServiceImpl implements PropertiesService {

    private final Logger log = LoggerFactory.getLogger(PropertiesServiceImpl.class);

    private static final String PROPERTIES_FILE_NAME = "properties.properties";

    @Override
    public Boolean save(String name, String value) {
        log.debug("Request to save property: {}:{}", name, value);
        Properties properties = getProperties();

        properties.setProperty(name, value);

        try {
            properties.store(new FileOutputStream(PROPERTIES_FILE_NAME), null);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @Override
    public String get(String name) {
        log.debug("Request to get value of property: {}", name);
        Properties properties = getProperties();

        return properties.getProperty(name);
    }

    private Properties getProperties() {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(PROPERTIES_FILE_NAME));
        } catch (FileNotFoundException e) {
            properties.setProperty("telegramBotApiToken", "null");
            properties.setProperty("adminId", "0");
            try {
                properties.store(new FileOutputStream(PROPERTIES_FILE_NAME), null);
            } catch (IOException fileNotFoundException) {
                fileNotFoundException.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return properties;
    }
}
