package org.telegram.bot.providers.media;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.exception.youtube.YoutubeDownloadNoResponseException;
import org.telegram.bot.exception.youtube.YtDlpException;
import org.telegram.bot.services.BotStats;
import org.telegram.bot.timers.FileManagerTimer;
import org.telegram.bot.utils.TelegramUtils;

import java.io.File;
import java.io.IOException;

@RequiredArgsConstructor
@Component
@Slf4j
public class YoutubeVideoProviderImpl implements YoutubeVideoProvider {

    private static final String FILE_PREFIX = "youtube_";

    private final ObjectMapper objectMapper;
    private final FileManagerTimer fileManagerTimer;
    private final BotStats botStats;

    @Override
    public File getVideo(String url) throws YoutubeDownloadNoResponseException, YtDlpException {
        VideoInfo videoInfo = getSuitableFormatId(url);
        String fileName = getFileName(videoInfo.extension);

        download(url, videoInfo.formatId, fileName);

        java.io.File videoFile = new java.io.File(fileName);
        if (!videoFile.exists()) {
            fileManagerTimer.deleteFile(fileName);

            String errorMessage = "File " + fileName + " does not exists";
            log.error("File {} does not exists", fileName);
            botStats.incrementErrors(fileName, errorMessage);

            throw new YoutubeDownloadNoResponseException(errorMessage);
        }

        return videoFile;
    }

    private VideoInfo getSuitableFormatId(String url) throws YtDlpException, YoutubeDownloadNoResponseException {
        ProcessBuilder pb = new ProcessBuilder("yt-dlp", "-J", url);
        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            String errorMessage = "Failed to call yt-dlp: " + e.getMessage();
            log.error(errorMessage);
            botStats.incrementErrors(e, errorMessage);
            throw new YtDlpException(errorMessage);
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(process.getInputStream());
        } catch (IOException e) {
            String errorMessage = "Failed to read youtube response: " + e.getMessage();
            log.error(errorMessage);
            botStats.incrementErrors(url, e, errorMessage);
            throw new YoutubeDownloadNoResponseException(errorMessage);
        }

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            String errorMessage = "Failed to wait yt-dlp response: " + e.getMessage();
            log.error(errorMessage);
            botStats.incrementErrors(e, errorMessage);
            throw new YtDlpException(errorMessage);
        }

        JsonNode formats = root.get("formats");
        if (formats == null || formats.isEmpty()) {
            String errorMessage = "Youtube returns empty response";
            log.error(errorMessage);
            botStats.incrementErrors(url, errorMessage);
            throw new YoutubeDownloadNoResponseException(errorMessage);
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
            log.error(errorMessage);
            botStats.incrementErrors(url, errorMessage);
            throw new YoutubeDownloadNoResponseException(errorMessage);
        }

        return new VideoInfo(bestFormat.get("format_id").asText(), bestFormat.get("ext").asText());
    }

    private static long extractSize(JsonNode format, long duration) {
        if (format.has("filesize") && !format.get("filesize").isNull()) {
            return format.get("filesize").asLong();
        }

        if (format.has("filesize_approx") && !format.get("filesize_approx").isNull()) {
            return format.get("filesize_approx").asLong();
        }

        if (format.has("tbr") && !format.get("tbr").isNull() && duration > 0) {
            double tbr = format.get("tbr").asDouble();
            double bytesPerSecond = (tbr * 1000) / 8;
            return (long) (bytesPerSecond * duration);
        }

        return -1;
    }

    private String getFileName(String extension) {
        return fileManagerTimer.addFile(FILE_PREFIX, "." + extension);
    }

    private void download(String url, String formatId, String fileName) throws YtDlpException {
        ProcessBuilder downloadPb = new ProcessBuilder(
                "yt-dlp",
                "--concurrent-fragments", "1",
                "--socket-timeout", "5",
                "--retries", "150",
                "--fragment-retries", "15",
                "-f", formatId,
                "-o", fileName,
                url);

        downloadPb.inheritIO();

        try {
            Process downloadProcess = downloadPb.start();
            downloadProcess.waitFor();
        } catch (InterruptedException | IOException e) {
            String errorMessage = "Failed to download youtube-video: " + e.getMessage();
            log.error(errorMessage);
            botStats.incrementErrors(url, e, errorMessage);
            throw new YtDlpException(errorMessage);
        }
    }

    private record VideoInfo(String formatId, String extension) {}

}
