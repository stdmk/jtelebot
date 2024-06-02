package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.FileResponse;
import org.telegram.bot.domain.model.response.FileType;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.timers.FileManagerTimer;
import org.telegram.bot.utils.TextUtils;

import java.io.*;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class Webcam implements Command {

    private final Bot bot;
    private final SpeechService speechService;
    private final FileManagerTimer fileManagerTimer;
    private final CommandWaitingService commandWaitingService;

    private static final String FILE_NAME_PREFIX = "file";
    private static final String FILE_NAME_POSTFIX = ".mp4";
    private static final int DEFAULT_VIDEO_DURATION_IN_SECONDS = 5;
    private static final int MAX_VIDEO_DURATION_IN_SECONDS = 20;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();

        String commandArgument = commandWaitingService.getText(message);

        if (commandArgument == null) {
            log.debug("Empty request. Turning on command waiting");
            commandWaitingService.add(message, this.getClass());
            return returnResponse(new TextResponse(message)
                    .setText("${command.webcam.commandwaitingstart}"));
        } else {
            bot.sendUploadVideo(message.getChatId());
            String duration;
            String url;
            int spaceIndex = commandArgument.indexOf(" ");
            if (spaceIndex < 0) {
                duration = String.valueOf(DEFAULT_VIDEO_DURATION_IN_SECONDS);
                url = commandArgument;
            } else {
                url = commandArgument.substring(0, spaceIndex);
                duration = commandArgument.substring(spaceIndex + 1);
            }

            if (!TextUtils.isThatUrl(url) || !TextUtils.isThatInteger(duration) || Integer.parseInt(duration) > MAX_VIDEO_DURATION_IN_SECONDS) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            String fileName = fileManagerTimer.addFile(FILE_NAME_PREFIX, FILE_NAME_POSTFIX);
            final String command = "ffmpeg -re -t " + duration + " -i " + url + " -c:v copy -c:a copy -bsf:a aac_adtstoasc -t " + duration + " " + fileName;

            try {
                ProcessBuilder processBuilder = new ProcessBuilder().inheritIO().command(command.split(" "));

                Process process = processBuilder.start();
                process.waitFor();
            } catch (IOException | InterruptedException e) {
                log.error("Failed to call command {}: {}", command, e.getMessage());
                Thread.currentThread().interrupt();
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
            }

            File videoFile = new File(fileName);
            if (!videoFile.exists()) {
                fileManagerTimer.deleteFile(fileName);
                log.error("File {} does not exists", fileName);
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
            }

            return returnResponse(new FileResponse(message)
                    .addFile(new org.telegram.bot.domain.model.response.File(FileType.VIDEO, videoFile)));
        }
    }
}
