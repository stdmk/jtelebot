package org.telegram.bot.utils;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
@Component
public class NetworkUtils {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11";
    private static final Integer TELEGRAM_UPLOAD_MEDIA_LIMIT_BYTES = 52428800;

    public InputStream getFileFromUrlWithLimit(String url) throws IOException {
        return getFileFromUrlWithLimit(url, TELEGRAM_UPLOAD_MEDIA_LIMIT_BYTES);
    }

    public InputStream getFileFromUrlWithLimit(String url, int limitBytes) throws IOException {
        return new BoundedInputStream(getFileFromUrl(url), limitBytes);
    }

    public InputStream getFileFromUrl(String url) throws IOException {
        URLConnection connection = new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.connect();

        return connection.getInputStream();
    }

    public String readStringFromURL(String url) throws IOException {
        return readStringFromURL(new URL(url).toString(), StandardCharsets.UTF_8);
    }

    public String readStringFromURL(String url, Charset encoding) throws IOException {
        return IOUtils.toString(new URL(url), encoding);
    }

    public SyndFeed getRssFeedFromUrl(String url) throws IOException, FeedException {
        SyndFeedInput syndFeedInput = new SyndFeedInput();
        syndFeedInput.setAllowDoctypes(true);
        return syndFeedInput.build(new XmlReader(new URL(url)));
    }
}
