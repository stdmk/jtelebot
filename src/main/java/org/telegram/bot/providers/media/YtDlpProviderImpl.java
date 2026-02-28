package org.telegram.bot.providers.media;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.enums.yt_dlp.MediaPlatform;
import org.telegram.bot.exception.youtube.YtDlpBigFileException;
import org.telegram.bot.exception.youtube.YtDlpCallException;
import org.telegram.bot.exception.youtube.YtDlpException;
import org.telegram.bot.exception.youtube.YtDlpNoResponseException;
import org.telegram.bot.services.BotStats;
import org.telegram.bot.timers.FileManagerTimer;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.bot.utils.TelegramUtils;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;

@RequiredArgsConstructor
@Component
@Slf4j
public class YtDlpProviderImpl implements YtDlpProvider {

    private static final String FILE_PREFIX = "yt_dlp_";

    private final ObjectMapper objectMapper;
    private final FileManagerTimer fileManagerTimer;
    private final BotStats botStats;

    @Override
    public File getVideo(MediaPlatform mediaPlatform, String url) throws YtDlpException {
        VideoInfo videoInfo = getSuitableFormatId(mediaPlatform, url);
        String fileName = getFileName(videoInfo.extension);

        download(mediaPlatform, url, videoInfo.formatId, fileName);

        java.io.File videoFile = new java.io.File(fileName);
        if (!videoFile.exists()) {
            fileManagerTimer.deleteFile(fileName);

            String errorMessage = "File " + fileName + " does not exists";
            log.error("File {} does not exists", fileName);
            botStats.incrementErrors(fileName, errorMessage);

            throw new YtDlpNoResponseException(errorMessage);
        }

        return videoFile;
    }

    private VideoInfo getSuitableFormatId(MediaPlatform mediaPlatform, String url) throws YtDlpCallException, YtDlpNoResponseException, YtDlpBigFileException {
        ProcessBuilder pb = new ProcessBuilder(getFormatIdArguments(mediaPlatform, url));
        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            String errorMessage = "Failed to call yt-dlp: " + e.getMessage();
            log.error(errorMessage);
            botStats.incrementErrors(e, errorMessage);
            throw new YtDlpCallException(errorMessage);
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(process.getInputStream());
        } catch (IOException e) {
            String errorMessage = "Failed to read youtube response: " + e.getMessage();
            log.error(errorMessage);
            botStats.incrementErrors(url, e, errorMessage);
            throw new YtDlpNoResponseException(errorMessage);
        }

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            String errorMessage = "Failed to wait yt-dlp response: " + e.getMessage();
            log.error(errorMessage);
            botStats.incrementErrors(e, errorMessage);
            throw new YtDlpCallException(errorMessage);
        }

        JsonNode formats = root.get("formats");
        if (formats == null || formats.isEmpty()) {
            String errorMessage = "Youtube returns empty response";
            log.error(errorMessage);
            botStats.incrementErrors(url, errorMessage);
            throw new YtDlpNoResponseException(errorMessage);
        }

        JsonNode bestFormat = null;
        long duration = root.path("duration").asLong(0);
        long bestHeight = 0;
        for (JsonNode format : formats) {
            boolean hasAudio = !format.path("acodec").asText("").equals("none");
            boolean hasVideo = !format.path("vcodec").asText("").equals("none");
            if (!hasAudio || !hasVideo) {
                continue;
            }

            long size = extractSize(format, duration);
            if (size > 0 && size <= TelegramUtils.MAX_FILE_LIMIT_BYTES) {
                int height = format.path("height").asInt(0);
                if (bestFormat == null || height > bestHeight) {
                    bestFormat = format;
                    bestHeight = height;
                }
            }
        }

        if (bestFormat == null) {
            String errorMessage = "Unable to find best format";
            log.info(errorMessage);
            throw new YtDlpBigFileException(errorMessage);
        }

        return new VideoInfo(bestFormat.get("format_id").asText(), bestFormat.get("ext").asText());
    }

    private List<String> getFormatIdArguments(MediaPlatform mediaPlatform, String url) {
        if (mediaPlatform.isNeedsUserAgent()) {
            return List.of(
                    "yt-dlp",
                    "--user-agent", NetworkUtils.USER_AGENT,
                    "-J", url
            );
        } else {
            return List.of(
                    "yt-dlp",
                    "-J", url
            );
        }
    }

    private static long extractSize(JsonNode format, long duration) {
        if (format.has("filesize") && !format.get("filesize").isNull()) {
            return format.get("filesize").asLong();
        }

        if (format.has("filesize_approx") && !format.get("filesize_approx").isNull()) {
            return format.get("filesize_approx").asLong();
        }

        if (format.has("tbr") && !format.get("tbr").isNull() && duration > 0) {
            double tbr = format.get("tbr").asDouble(); // кбит/с
            double bytesPerSecond = (tbr * 1000) / 8;
            return (long) (bytesPerSecond * duration);
        }

        if (format.has("url") && !format.get("url").isNull()) {
            return resolveSizeViaHead(format);
        }

        return -1;
    }

    private static long resolveSizeViaHead(JsonNode format) {
        try {
            String videoUrl = format.get("url").asText();

            HttpURLConnection connection = (HttpURLConnection) URI.create(videoUrl).toURL().openConnection();

            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            if (format.has("http_headers")) {
                JsonNode headers = format.get("http_headers");
                headers.fieldNames().forEachRemaining(name -> connection.setRequestProperty(name, headers.get(name).asText()));
            } else {
                connection.setRequestProperty("User-Agent", NetworkUtils.USER_AGENT);
            }

            connection.connect();

            long contentLength = connection.getContentLengthLong();
            connection.disconnect();

            return contentLength > 0 ? contentLength : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    private String getFileName(String extension) {
        return fileManagerTimer.addFile(FILE_PREFIX, "." + extension);
    }

    private void download(MediaPlatform mediaPlatform, String url, String formatId, String fileName) throws YtDlpCallException {
        ProcessBuilder downloadPb = new ProcessBuilder(getDownloadAguments(mediaPlatform, url, formatId, fileName));

        downloadPb.inheritIO();

        try {
            Process downloadProcess = downloadPb.start();
            downloadProcess.waitFor();
        } catch (InterruptedException | IOException e) {
            String errorMessage = "Failed to download youtube-video: " + e.getMessage();
            log.error(errorMessage);
            botStats.incrementErrors(url, e, errorMessage);
            throw new YtDlpCallException(errorMessage);
        }
    }

    private List<String> getDownloadAguments(MediaPlatform mediaPlatform, String url, String formatId, String fileName) {
        if (mediaPlatform.isNeedsUserAgent()) {
            return List.of(
                    "yt-dlp",
                    "--user-agent", NetworkUtils.USER_AGENT,
                    "--concurrent-fragments", "1",
                    "--socket-timeout", "5",
                    "--retries", "150",
                    "--fragment-retries", "15",
                    "-f", formatId,
                    "-o", fileName,
                    url
            );
        } else {
            return List.of(
                    "yt-dlp",
                    "--concurrent-fragments", "1",
                    "--socket-timeout", "5",
                    "--retries", "150",
                    "--fragment-retries", "15",
                    "-f", formatId,
                    "-o", fileName,
                    url
            );
        }
    }

    private record VideoInfo(String formatId, String extension) {}

}
