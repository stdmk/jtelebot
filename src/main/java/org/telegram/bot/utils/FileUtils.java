package org.telegram.bot.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class FileUtils {
    public static void checkPropertiesFileExists() {
        File f = new File("properties.properties");
        if (!f.exists()) {
            Properties properties = new Properties();
            properties.setProperty("telegramBotApiToken", "");
            properties.setProperty("adminId", "0");
            properties.setProperty("openweathermapId", "");
            try {
                properties.store(new FileOutputStream(f), null);
            } catch (IOException fileNotFoundException) {
                fileNotFoundException.printStackTrace();
            }
        }
    }
}
