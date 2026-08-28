package org.telegram.bot.providers.media;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.model.response.File;
import org.telegram.bot.domain.model.response.FileSettings;
import org.telegram.bot.domain.model.response.FileType;
import org.telegram.bot.enums.yt_dlp.MediaPlatform;
import org.telegram.bot.exception.youtube.YtDlpBigFileException;
import org.telegram.bot.exception.youtube.YtDlpCallException;
import org.telegram.bot.exception.youtube.YtDlpException;
import org.telegram.bot.exception.youtube.YtDlpNoResponseException;
import org.telegram.bot.services.BotStats;
import org.telegram.bot.services.TemporaryFileManager;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.bot.utils.TelegramUtils;
import org.telegram.bot.utils.TextUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
@Component
@Slf4j
public class YtDlpProviderImpl implements YtDlpProvider {

    private static final long MAX_AUDIO_BITS = TelegramUtils.MAX_FILE_LIMIT_BYTES * 8;

    private final ObjectMapper objectMapper;
    private final TemporaryFileManager temporaryFileManager;
    private final BotStats botStats;

    @Override
    public File getVideo(MediaPlatform mediaPlatform, String url) throws YtDlpException {
        VideoInfo videoInfo = getSuitableFormatId(mediaPlatform, url);
        String fileName = getFileName(videoInfo.title, videoInfo.ext);

        download(mediaPlatform, url, videoInfo.formatId, fileName);

        java.io.File videoFile = temporaryFileManager.get(fileName);
        if (videoFile == null) {
            String errorMessage = "File " + fileName + " does not exists";
            log.error("File {} does not exists", fileName);
            botStats.incrementErrors(fileName, errorMessage);
            throw new YtDlpNoResponseException(errorMessage);
        }

        return new File(
                FileType.VIDEO,
                videoFile,
                new FileSettings()
                        .setDuration(videoInfo.duration)
                        .setWidth(videoInfo.width)
                        .setHeight(videoInfo.height));
    }

    @Override
    public File getAudio(MediaPlatform mediaPlatform, String url) throws YtDlpException {
        long duration = getDuration(mediaPlatform, url);
        if (duration <= 0) {
            throw new YtDlpNoResponseException("Unable to determine audio duration");
        }

        AudioInfo audioInfo = getAudioInfo(mediaPlatform, url);
        String fileName = getFileName(audioInfo.title, audioInfo.ext);

        int bitrate = calculateAudioBitrate(duration);
        temporaryFileManager.addFile(fileName, audioInfo.ext);
        ProcessBuilder pb = new ProcessBuilder(getAudioArguments(mediaPlatform, url, fileName, bitrate));
        pb.inheritIO();

        try {
            Process process = pb.start();
            process.waitFor();
        } catch (InterruptedException | IOException e) {
            String errorMessage = "Failed to download audio: " + e.getMessage();
            log.error(errorMessage);
            botStats.incrementErrors(url, e, errorMessage);
            throw new YtDlpCallException(errorMessage);
        }

        java.io.File audioFile = temporaryFileManager.get(fileName);
        if (audioFile == null) {
            String errorMessage = "File " + fileName + " does not exist";
            log.error(errorMessage);
            botStats.incrementErrors(fileName, errorMessage);
            throw new YtDlpNoResponseException(errorMessage);
        }

        return new File(FileType.AUDIO, audioFile, new FileSettings().setDuration(duration));
    }

    @Override
    public java.io.File getVideoFragment(MediaPlatform mediaPlatform, String url, int durationSeconds) throws YtDlpException {
        VideoInfo videoInfo = getSuitableFormatId(mediaPlatform, url);

        String fileName = getFileName(videoInfo.title, videoInfo.ext);

        downloadFragment(mediaPlatform, url, videoInfo.formatId, fileName, durationSeconds);

        java.io.File videoFile = temporaryFileManager.get(fileName);
        if (videoFile == null) {
            throw new YtDlpNoResponseException("Unable to download video fragment");
        }

        return videoFile;
    }

    private void downloadFragment(MediaPlatform mediaPlatform, String url, String formatId, String fileName, int durationSeconds) throws YtDlpCallException {
        ProcessBuilder pb = new ProcessBuilder(getFragmentArguments(mediaPlatform, url, formatId, fileName, durationSeconds));

        pb.inheritIO();

        try {
            Process process = pb.start();
            process.waitFor();
        } catch (InterruptedException | IOException e) {
            throw new YtDlpCallException(e.getMessage());
        }
    }

    private List<String> getFragmentArguments(MediaPlatform mediaPlatform, String url, String formatId, String fileName, int durationSeconds) {
        List<String> args = new ArrayList<>();

        args.add("yt-dlp");

        if (mediaPlatform.isNeedsUserAgent()) {
            args.add("--user-agent");
            args.add(NetworkUtils.USER_AGENT);
        }

        args.add("--concurrent-fragments");
        args.add("1");

        args.add("-f");
        args.add(formatId);

        args.add("--downloader");
        args.add("ffmpeg");

        args.add("--downloader-args");
        args.add("ffmpeg_i:-t " + durationSeconds);

        args.add("-o");
        args.add(fileName);

        args.add(url);

        return args;
    }

    private long getDuration(MediaPlatform mediaPlatform, String url) throws YtDlpCallException {
        ProcessBuilder pb = new ProcessBuilder(getFormatIdArguments(mediaPlatform, url));
        try {
            Process process = pb.start();
            JsonNode root = objectMapper.readTree(process.getInputStream());
            process.waitFor();
            return root.path("duration").asLong(0);
        } catch (IOException | InterruptedException e) {
            String errorMessage = "Failed to get media duration: " + e.getMessage();
            log.error(errorMessage);
            botStats.incrementErrors(url, e, errorMessage);
            throw new YtDlpCallException(errorMessage);
        }
    }

    private AudioInfo getAudioInfo(MediaPlatform mediaPlatform, String url) throws YtDlpCallException {
        ProcessBuilder pb = new ProcessBuilder(getFormatIdArguments(mediaPlatform, url));
        try {
            Process process = pb.start();
            JsonNode root = objectMapper.readTree(process.getInputStream());
            process.waitFor();

            long duration = root.path("duration").asLong(0);
            String title = root.path("title").asText("audio");
            String ext = "mp3";

            return new AudioInfo(title, ext, duration);
        } catch (IOException | InterruptedException e) {
            String errorMessage = "Failed to get audio info: " + e.getMessage();
            log.error(errorMessage);
            botStats.incrementErrors(url, e, errorMessage);
            throw new YtDlpCallException(errorMessage);
        }
    }

    private int calculateAudioBitrate(long durationSeconds) {
        long bitrate = MAX_AUDIO_BITS / durationSeconds;
        int kbps = (int) (bitrate / 1000);

        if (kbps > 320) {
            kbps = 320;
        }
        if (kbps < 64) {
            kbps = 64;
        }

        return kbps;
    }

    private List<String> getAudioArguments(MediaPlatform mediaPlatform, String url, String fileName, int bitrate) {
        String quality = bitrate + "K";
        if (mediaPlatform.isNeedsUserAgent()) {
            return List.of(
                    "yt-dlp",
                    "--user-agent", NetworkUtils.USER_AGENT,
                    "-x",
                    "--audio-format", "mp3",
                    "--audio-quality", quality,
                    "--no-playlist",
                    "-o", fileName,
                    url
            );
        } else {
            return List.of(
                    "yt-dlp",
                    "-x",
                    "--audio-format", "mp3",
                    "--audio-quality", quality,
                    "--no-playlist",
                    "-o", fileName,
                    url
            );
        }
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
            String errorMessage = "Returns empty response";
            log.error(errorMessage);
            throw new YtDlpNoResponseException(errorMessage);
        }

        long duration = root.path("duration").asLong(0);
        List<JsonNode> videoFormats = new ArrayList<>();
        List<JsonNode> audioFormats = new ArrayList<>();

        for (JsonNode format : formats) {
            boolean hasAudio = !format.path("acodec").asText("").equals("none");
            boolean hasVideo = !format.path("vcodec").asText("").equals("none");

            if (hasVideo && !hasAudio) {
                videoFormats.add(format);
            } else if (!hasVideo && hasAudio) {
                audioFormats.add(format);
            }
        }

        if (videoFormats.isEmpty() || audioFormats.isEmpty()) {
            String errorMessage = "Unable to find video and audio formats";
            log.info(errorMessage);
            throw new YtDlpBigFileException(errorMessage);
        }

        videoFormats.sort(
                Comparator
                        .comparingInt((JsonNode format) -> format.path("height").asInt(0))
                        .thenComparingInt(format -> format.path("fps").asInt(0))
                        .thenComparingInt(format -> isPreferredVideoFormat(format) ? 1 : 0)
                        .reversed()
        );

        audioFormats.sort(
                Comparator
                        .comparingInt((JsonNode format) -> isPreferredAudioFormat(format) ? 1 : 0)
                        .thenComparingDouble(format -> format.path("abr").asDouble(0))
                        .reversed()
        );

        JsonNode bestVideo = null;
        JsonNode bestAudio = null;
        long bestHeight = 0;

        for (JsonNode video : videoFormats) {
            long videoSize = extractSize(video);
            if (videoSize <= 0) {
                continue;
            }

            for (JsonNode audio : audioFormats) {
                long audioSize = extractSize(audio);
                if (audioSize <= 0) {
                    continue;
                }

                long totalSize = videoSize + audioSize;

                if (totalSize <= TelegramUtils.MAX_FILE_LIMIT_BYTES) {
                    bestVideo = video;
                    bestAudio = audio;
                    bestHeight = video.path("height").asInt(0);
                    break;
                }
            }

            if (bestVideo != null) {
                break;
            }
        }

        if (bestVideo == null || bestAudio == null) {
            String errorMessage = "Unable to find best format";
            log.info(errorMessage);
            throw new YtDlpBigFileException(errorMessage);
        }

        String formatId = bestVideo.get("format_id").asText()
                + "+"
                + bestAudio.get("format_id").asText();

        String fileName = TextUtils.sanitize(root.path("title").asText("video"));

        String ext = getMergedExtension(bestVideo, bestAudio);

        log.info(
                "Selected formats: video={}, audio={}, videoSize={}, audioSize={}, totalSize={}, height={}, ext={}",
                bestVideo.path("format_id").asText(),
                bestAudio.path("format_id").asText(),
                extractSize(bestVideo),
                extractSize(bestAudio),
                extractSize(bestVideo) + extractSize(bestAudio),
                bestHeight,
                ext
        );

        return new VideoInfo(
                formatId,
                fileName,
                ext,
                duration,
                bestVideo.path("width").asInt(0),
                bestVideo.path("height").asInt(0)
        );
    }

    private String getMergedExtension(JsonNode video, JsonNode audio) {
        if (isPreferredVideoFormat(video) && isPreferredAudioFormat(audio)) {
            return "mp4";
        }

        String videoExt = video.path("ext").asText("");
        if ("webm".equalsIgnoreCase(videoExt)) {
            return "webm";
        }

        String audioExt = audio.path("ext").asText("");
        if ("webm".equalsIgnoreCase(audioExt)) {
            return "webm";
        }

        return videoExt.isEmpty() ? "mp4" : videoExt;
    }

    private boolean isPreferredVideoFormat(JsonNode format) {
        String ext = format.path("ext").asText("");
        String vcodec = format.path("vcodec").asText("");

        return "mp4".equalsIgnoreCase(ext) && vcodec.startsWith("avc1");
    }

    private boolean isPreferredAudioFormat(JsonNode format) {
        String ext = format.path("ext").asText("");
        String acodec = format.path("acodec").asText("");

        return "m4a".equalsIgnoreCase(ext) || acodec.startsWith("mp4a");
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

    private static long extractSize(JsonNode format) {
        if (format.has("filesize") && !format.get("filesize").isNull()) {
            long filesize = format.get("filesize").asLong();
            if (filesize > 0) {
                return filesize;
            }
        }

        if (format.has("filesize_approx") && !format.get("filesize_approx").isNull()) {
            long filesizeApprox = format.get("filesize_approx").asLong();
            if (filesizeApprox > 0) {
                return filesizeApprox;
            }
        }

        return -1;
    }

    private String getFileName(String fileName, String ext) {
        return temporaryFileManager.addFile(fileName, "." + ext);
    }

    private void download(MediaPlatform mediaPlatform, String url, String formatId, String fileName) throws YtDlpCallException {
        ProcessBuilder downloadPb = new ProcessBuilder(getDownloadAguments(mediaPlatform, url, formatId, fileName));

        downloadPb.inheritIO();

        try {
            Process downloadProcess = downloadPb.start();

            int exitCode = downloadProcess.waitFor();

            log.info("yt-dlp finished with exit code: {}", exitCode);
            log.info("Expected output file: {}", new java.io.File(fileName).getAbsolutePath());
            log.info("Expected output file exists: {}", new java.io.File(fileName).exists());

            if (exitCode != 0) {
                String errorMessage = "yt-dlp exited with code " + exitCode;
                log.error(errorMessage);
                botStats.incrementErrors(url, errorMessage);
                throw new YtDlpCallException(errorMessage);
            }
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

    private record VideoInfo(String formatId, String title, String ext, long duration, int width, int height) {
    }

    private record AudioInfo(String title, String ext, long duration) {
    }

}
