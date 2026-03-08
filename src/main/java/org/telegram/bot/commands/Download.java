package org.telegram.bot.commands;

import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.yt_dlp.MediaPlatform;
import org.telegram.bot.enums.yt_dlp.MediaType;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.exception.youtube.YtDlpException;
import org.telegram.bot.exception.youtube.YtDlpNoResponseException;
import org.telegram.bot.providers.media.YtDlpProvider;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.InternationalizationService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.NetworkUtils;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.telegram.bot.utils.TextUtils.isThatUrl;

@Component
@RequiredArgsConstructor
@Slf4j
public class Download implements Command {

    private final InternationalizationService internationalizationService;
    private final Bot bot;
    private final NetworkUtils networkUtils;
    private final YtDlpProvider ytDlpProvider;
    private final SpeechService speechService;
    private final CommandWaitingService commandWaitingService;

    private Set<String> videoMediaTypes;
    private Set<String> audioMediaTypes;

    private static final String DEFAULT_FILE_NAME = "file";

    @PostConstruct
    public void postConstruct() {
        videoMediaTypes = internationalizationService.getAllTranslations("command.download.videotype");
        audioMediaTypes = internationalizationService.getAllTranslations("command.download.audiotype");
    }

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
            MediaPlatform mediaPlatform = MediaPlatform.getByUrl(commandArgument);
            if (mediaPlatform != null) {
                bot.sendUploadDocument(chatId);
                file = getFileFromMediaPlatform(mediaPlatform, fileParams);
            } else {
                bot.sendUploadDocument(chatId);
                file = getFile(fileParams.url);
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
        String mediaType = null;

        String[] parts = text.trim().split("\\s+", 2);
        if (parts.length == 1) {
            url = parts[0];
        } else {
            mediaType = parts[0];
            url = parts[1];
        }

        if (!isThatUrl(url)) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        return new FileParams(url, resolveMediaType(mediaType));
    }

    private MediaType resolveMediaType(String rawType) {
        if (rawType == null) {
            return null;
        }

        rawType = rawType.toLowerCase(Locale.ROOT);
        if (videoMediaTypes.contains(rawType)) {
            return MediaType.VIDEO;
        } else if (audioMediaTypes.contains(rawType)) {
            return MediaType.AUDIO;
        } else {
            return null;
        }
    }

    private File getFileFromMediaPlatform(MediaPlatform mediaPlatform, FileParams fileParams) {
        MediaType mediaType = fileParams.mediaType;
        if (mediaType == null) {
            Set<MediaType> supportedMediaTypes = mediaPlatform.getSupportedMediaTypes();
            if (supportedMediaTypes.size() > 1) {
                mediaType = MediaType.VIDEO;
            } else {
                mediaType = supportedMediaTypes.iterator().next();
            }
        }

        java.io.File diskFile;
        try {
            if (MediaType.VIDEO.equals(mediaType)) {
                diskFile = ytDlpProvider.getVideo(mediaPlatform, fileParams.url);
                return new File(FileType.VIDEO, diskFile);
            } else if (MediaType.AUDIO.equals(mediaType)) {
                diskFile = ytDlpProvider.getAudio(mediaPlatform, fileParams.url);
                return new File(FileType.AUDIO, diskFile);
            } else {
                String errorMessage = "Unable to find method to download media type " + mediaType;
                log.error(errorMessage);
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
            }
        } catch (YtDlpNoResponseException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        } catch (YtDlpException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }


    }

    @Nullable
    private File getFile(String url) {
        byte[] fileFromUrl;
        try {
            fileFromUrl = networkUtils.getFileFromUrlWithLimit(url);
        } catch (Exception e) {
            return null;
        }

        return new File(FileType.FILE, fileFromUrl, DEFAULT_FILE_NAME);
    }

    private record FileParams(String url, MediaType mediaType) {
    }
}
