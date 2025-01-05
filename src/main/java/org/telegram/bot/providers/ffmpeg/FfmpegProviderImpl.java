package org.telegram.bot.providers.ffmpeg;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.exception.ffmpeg.FfmpegException;
import org.telegram.bot.timers.FileManagerTimer;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

@RequiredArgsConstructor
@Component
@Slf4j
public class FfmpegProviderImpl implements FfmpegProvider {

    private static final String FILE_NAME_PREFIX = "file";
    private static final String FILE_NAME_POSTFIX = ".mp4";
    private static final String COMMAND_TEMPLATE = "ffmpeg -re -t {1} -i {0} -c:v copy -c:a copy -bsf:a aac_adtstoasc -t {1} {2}";

    private final FileManagerTimer fileManagerTimer;

    @Override
    public File getVideo(String url, String duration) throws FfmpegException {
        String fileName = fileManagerTimer.addFile(FILE_NAME_PREFIX, FILE_NAME_POSTFIX);

        String command = MessageFormat.format(COMMAND_TEMPLATE, url, duration, fileName);

        try {
            ProcessBuilder processBuilder = new ProcessBuilder().inheritIO().command(command.split(" "));

            Process process = processBuilder.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            log.error("Failed to call command {}: {}", command, e.getMessage());
            Thread.currentThread().interrupt();
            throw new FfmpegException(e.getMessage());
        }

        File videoFile = new File(fileName);
        if (!videoFile.exists()) {
            fileManagerTimer.deleteFile(fileName);
            log.error("File {} does not exists", fileName);
            throw new FfmpegException("File does not exists");
        }

        return videoFile;
    }

}
