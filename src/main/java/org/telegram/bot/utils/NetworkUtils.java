package org.telegram.bot.utils;

import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class NetworkUtils {

    public static InputStream getImageFromUrl(String url) {
        RestTemplate restTemplate = new RestTemplate();
        byte[] imageBytes;

        imageBytes = restTemplate.getForObject(url, byte[].class);

        return new ByteArrayInputStream(imageBytes);
    }
}
