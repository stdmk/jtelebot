package org.telegram.bot.providers.ffmpeg;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.exception.ffmpeg.FfmpegException;
import org.telegram.bot.services.TemporaryFileManager;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Component
@Slf4j
public class FfmpegProviderImpl implements FfmpegProvider {

    private static final String FILE_NAME_PREFIX = "file";
    private static final String FILE_NAME_POSTFIX = ".mp4";
    private static final long PROCESS_TIMEOUT_SECONDS = 60;

    private final TemporaryFileManager temporaryFileManager;

    @Override
    public File getVideo(String url, String duration) throws FfmpegException {
        String fileName = temporaryFileManager.addFile(FILE_NAME_PREFIX, FILE_NAME_POSTFIX);

        List<String> command = List.of("ffmpeg", "-re", "-t", duration, "-i", url,
                "-c:v", "copy", "-c:a", "copy", "-bsf:a", "aac_adtstoasc", "-t", duration, fileName);

        try {
            Process process = new ProcessBuilder(command).inheritIO().start();
            if (!process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new FfmpegException("ffmpeg timed out after " + PROCESS_TIMEOUT_SECONDS + " seconds");
            }
            if (process.exitValue() != 0) {
                throw new FfmpegException("ffmpeg exited with code " + process.exitValue());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting for ffmpeg", e);
            throw new FfmpegException(e.getMessage());
        } catch (IOException e) {
            log.error("Failed to call ffmpeg {}: {}", command, e.getMessage());
            throw new FfmpegException(e.getMessage());
        }

        File videoFile = temporaryFileManager.get(fileName);
        if (videoFile == null) {
            log.error("File {} does not exists", fileName);
            throw new FfmpegException("File does not exists");
        }

        return videoFile;
    }

}
