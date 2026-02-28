package org.telegram.bot.utils;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;

@RequiredArgsConstructor
@Component
public class NetworkUtils {

    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " + "AppleWebKit/537.36 (KHTML, like Gecko) " + "Chrome/122.0.0.0 Safari/537.36";
    private static final int TELEGRAM_UPLOAD_MEDIA_LIMIT_BYTES = 52428800;
    private static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 10 * 1000;
    private static final int DEFAULT_READ_TIMEOUT_MILLIS = 10 * 1000;

    public byte[] getFileFromUrl(String url) throws IOException {
        try (InputStream in = openStreamFromUrl(url)) {
            return in.readAllBytes();
        }
    }

    public byte[] getFileFromUrlWithLimit(String url) throws IOException {
        return getFileFromUrlWithLimit(url, TELEGRAM_UPLOAD_MEDIA_LIMIT_BYTES);
    }

    public byte[] getFileFromUrlWithLimit(String url, int limitBytes) throws IOException {
        try (InputStream in = openStreamFromUrl(url);
             BoundedInputStream bounded = new BoundedInputStream(in, limitBytes)) {

            bounded.setPropagateClose(false);
            return bounded.readAllBytes();
        }
    }

    private InputStream openStreamFromUrl(String url) throws IOException {
        URLConnection connection = URI.create(url).toURL().openConnection();
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(DEFAULT_READ_TIMEOUT_MILLIS);
        connection.connect();
        return connection.getInputStream();
    }

    public String readStringFromURL(String url) throws IOException {
        return readStringFromURL(URI.create(url).toURL().toString(), StandardCharsets.UTF_8);
    }

    public String readStringFromURL(URL url) throws IOException {
        return readStringFromURL(url.toString(), StandardCharsets.UTF_8);
    }

    public String readStringFromURL(String url, Charset encoding) throws IOException {
        return readStringFromURL(URI.create(url).toURL(), encoding);
    }

    public String readStringFromURL(URL url, Charset encoding) throws IOException {
        return IOUtils.toString(url, encoding);
    }

    public SyndFeed getRssFeedFromUrl(String url) throws IOException, FeedException {
        SyndFeedInput syndFeedInput = new SyndFeedInput();
        syndFeedInput.setAllowDoctypes(true);
        return syndFeedInput.build(new XmlReader(URI.create(url).toURL()));
    }

    public PingResult pingHost(String host) throws UnknownHostException {
        InetAddress inetAddress = InetAddress.getByName(host);

        boolean reachable;
        long durationMillis;
        LocalDateTime start = LocalDateTime.now();
        try {
            reachable = inetAddress.isReachable(4000);
        } catch (IOException e) {
            reachable = false;
        }

        Duration delta = Duration.between(start, LocalDateTime.now());
        durationMillis = (delta.getSeconds() * 1000) + (delta.getNano() / 1000000);

        return new PingResult(inetAddress.getHostName(), inetAddress.getHostAddress(), reachable, durationMillis);
    }

    @Value
    public static class PingResult {
        String host;
        String ip;
        boolean reachable;
        long durationMillis;
    }

}
