package org.telegram.bot.utils;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.apache.commons.io.IOUtils;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Objects;

public class NetworkUtils {

    public static InputStream getFileFromUrl(String url) {
        return new ByteArrayInputStream(Objects.requireNonNull(downloadFile(url)));
    }

    public static InputStream getFileFromUrl(String url, int limitBytes) throws Exception {
        byte[] file = downloadFile(url);
        if (file.length > limitBytes) {
            throw new Exception("the file is not included in the limit");
        }

        return new ByteArrayInputStream(Objects.requireNonNull(file));
    }

    private static byte[] downloadFile(String url) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("User-Agent", "Mozilla/4.0");

        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

        ResponseEntity<byte[]> respEntity = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);

        return respEntity.getBody();
    }

    public static String readStringFromURL(String url) throws IOException {
        return IOUtils.toString(new URL(url), StandardCharsets.UTF_8);
    }

    public static SyndFeed getRssFeedFromUrl(String url) {
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = null;
        try {
            feed = input.build(new XmlReader(new URL(url)));
        } catch (FeedException | IOException ignored) {

        }

        return feed;
    }
}
