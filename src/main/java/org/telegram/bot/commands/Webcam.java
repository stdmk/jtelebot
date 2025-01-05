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
import org.telegram.bot.exception.ffmpeg.FfmpegException;
import org.telegram.bot.providers.ffmpeg.FfmpegProvider;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.TextUtils;

import java.io.File;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class Webcam implements Command {

    private final Bot bot;
    private final SpeechService speechService;
    private final FfmpegProvider ffmpegProvider;
    private final CommandWaitingService commandWaitingService;

    private static final int DEFAULT_VIDEO_DURATION_IN_SECONDS = 5;
    private static final int MAX_VIDEO_DURATION_IN_SECONDS = 20;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();

        String commandArgument = commandWaitingService.getText(message);

        if (commandArgument == null) {
            bot.sendTyping(message.getChatId());
            log.debug("Empty request. Turning on command waiting");
            commandWaitingService.add(message, this.getClass());
            return returnResponse(new TextResponse(message)
                    .setText("${command.webcam.commandwaitingstart}"));
        } else {
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

            if (!TextUtils.isThatUrl(url) || !TextUtils.isThatPositiveInteger(duration) || Integer.parseInt(duration) > MAX_VIDEO_DURATION_IN_SECONDS) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            bot.sendUploadVideo(message.getChatId());

            File videoFile;
            try {
                videoFile = ffmpegProvider.getVideo(url, duration);
            } catch (FfmpegException e) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
            }

            return returnResponse(new FileResponse(message)
                    .addFile(new org.telegram.bot.domain.model.response.File(FileType.VIDEO, videoFile)));
        }
    }
}
