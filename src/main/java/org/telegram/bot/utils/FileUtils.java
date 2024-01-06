package org.telegram.bot.utils;

import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

@UtilityClass
public class FileUtils {
    public static void checkPropertiesFileExists() {
        File f = new File("properties.properties");
        if (!f.exists()) {
            Properties properties = new Properties();
            properties.setProperty("telegramBotApiToken", "");
            properties.setProperty("adminId", "0");
            properties.setProperty("openweathermapId", "");
            properties.setProperty("googleToken", "");
            properties.setProperty("screenshotMachineToken", "");
            properties.setProperty("wolframAlphaToken", "");
            try (FileOutputStream fos = new FileOutputStream(f)) {
                properties.store(fos, null);
            } catch (IOException fileNotFoundException) {
                fileNotFoundException.printStackTrace();
            }
        }
    }
}
