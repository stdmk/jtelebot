package org.telegram.bot.utils;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class NetworkUtils {

    private final static String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11";

    public static InputStream getFileFromUrl(String url) throws IOException {
        URLConnection connection = new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.connect();

        return connection.getInputStream();
    }

    public static InputStream getFileFromUrl(String url, int limitBytes) throws Exception {
        byte[] file = IOUtils.toByteArray(getFileFromUrl(url));
        if (file.length > limitBytes) {
            throw new Exception("the file is not included in the limit");
        }

        return new ByteArrayInputStream(Objects.requireNonNull(file));
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
