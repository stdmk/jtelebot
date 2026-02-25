package org.telegram.bot.commands;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.exception.youtube.YoutubeDownloadException;
import org.telegram.bot.exception.youtube.YoutubeDownloadNoResponseException;
import org.telegram.bot.providers.media.YoutubeVideoProvider;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.NetworkUtils;
import org.telegram.bot.utils.TextUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static org.telegram.bot.utils.TextUtils.isThatUrl;

@Component
@RequiredArgsConstructor
@Slf4j
public class Download implements Command {

    private final Bot bot;
    private final NetworkUtils networkUtils;
    private final YoutubeVideoProvider youtubeVideoProvider;
    private final SpeechService speechService;
    private final CommandWaitingService commandWaitingService;

    private static final String DEFAULT_FILE_NAME = "file";

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();

        String commandArgument = commandWaitingService.getText(message);

        Long chatId = message.getChatId();
        if (commandArgument == null) {
            bot.sendTyping(chatId);
            log.debug("Empty request. Turning on command waiting");
            commandWaitingService.add(message, this.getClass());
            return returnResponse(new TextResponse(message)
                    .setText("${command.download.commandwaitingstart}"));
        } else {
            FileParams fileParams = getFileParams(commandArgument);

            File file;
            if (isYoutubeVideo(fileParams.url)) {
                bot.sendUploadVideo(chatId);
                file = getVideoFromYoutube(fileParams.url);
            } else {
                bot.sendUploadDocument(chatId);
                file = getFile(fileParams.url, fileParams.name);
            }

            if (file == null) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.TOO_BIG_FILE));
            }

            return returnResponse(new FileResponse(message)
                    .addFile(file));
        }
    }

    private FileParams getFileParams(String text) {
        String url;
        String fileName;

        int spaceIndex = text.indexOf(" ");
        if (spaceIndex > 0) {
            String firstArg = text.substring(0, spaceIndex);
            String secondArg = text.substring(spaceIndex + 1);

            if (isThatUrl(firstArg)) {
                url = firstArg;
                fileName = secondArg;
            } else {
                if (isThatUrl(secondArg)) {
                    url = secondArg;
                    fileName = firstArg;
                } else {
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                }
            }
        } else {
            if (isThatUrl(text)) {
                url = text;
                fileName = TextUtils.getFileNameFromUrl(url);
                if (fileName == null) {
                    fileName = DEFAULT_FILE_NAME;
                }
            } else {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }
        }

        return new FileParams(url, fileName);
    }

    private boolean isYoutubeVideo(String text) {
        URL url;
        try {
            url = TextUtils.findFirstUrlInText(text);
        } catch (MalformedURLException e) {
            return false;
        }

        return url.getHost().contains("youtube") || url.getHost().contains("youtu.be");
    }

    private File getVideoFromYoutube(String url) {
        java.io.File diskFile;
        try {
            diskFile = youtubeVideoProvider.getVideo(url);
        } catch (YoutubeDownloadNoResponseException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        } catch (YoutubeDownloadException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        return new File(FileType.VIDEO, diskFile);
    }

    @Nullable
    private File getFile(String url, String fileName) {
        byte[] fileFromUrl;
        try {
            fileFromUrl = networkUtils.getFileFromUrlWithLimit(url);
        } catch (Exception e) {
            return null;
        }

        return new File(FileType.FILE, fileFromUrl, fileName);
    }

    private record FileParams(String url, String name) {}
}
