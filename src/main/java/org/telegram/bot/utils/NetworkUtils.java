package org.telegram.bot.utils;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;

public class NetworkUtils {

    public static InputStream getFileFromUrl(String url) {
        RestTemplate restTemplate = new RestTemplate();
        return new ByteArrayInputStream(Objects.requireNonNull(restTemplate.getForObject(url, byte[].class)));
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
