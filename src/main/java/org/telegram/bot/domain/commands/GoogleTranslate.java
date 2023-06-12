package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.config.PropertiesConfig;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class GoogleTranslate implements CommandParent<SendMessage> {

    private final CommandWaitingService commandWaitingService;
    private final SpeechService speechService;
    private final RestTemplate botRestTemplate;
    private final PropertiesConfig propertiesConfig;

    private static final String GOOGLE_TRANSLATE_URL = "https://script.google.com/macros/s/";
    private static final Set<Character> RU_ALPHABET = "йцукенгшщзхъфывапролджэячсмитьбюё".chars().mapToObj(s -> (char) s).collect(Collectors.toSet());
    private static final Set<String> LANG_CODE_LIST = Set.of("af", "sq", "am", "ar", "hy", "az", "eu", "be", "bn", "bs",
            "bg", "ca", "ceb", "zh", "co", "hr", "cs", "da", "nl", "en", "eo", "et", "fi", "fr", "fy", "gl", "ka",
            "de", "el", "gu", "ht", "ha", "haw", "he", "hi", "hmn", "hu", "is", "ig", "id", "ga", "it", "ja", "jv", "kn",
            "kk", "km", "rw", "ko", "ku", "ky", "lo", "lv", "lt", "lb", "mk", "mg", "ms", "ml", "mt", "mi", "mr", "mn",
            "my", "ne", "no", "ny", "or", "ps", "fa", "pl", "pt", "pa", "ro", "ru", "sm", "gd", "sr", "st", "sn", "sd",
            "si", "sk", "sl", "so", "es", "su", "sw", "sv", "tl", "tg", "ta", "tt", "te", "th", "tr", "tk", "uk", "ur",
            "ug", "uz", "vi", "cy", "xh", "yi", "yo", "zu");

    @Override
    public SendMessage parse(Update update) {
        Integer replyToMessage;
        String responseText;

        Message message = getMessageFromUpdate(update);
        String textMessage = commandWaitingService.getText(message);

        if (textMessage == null) {
            textMessage = cutCommandInText(message.getText() + " ");
        }

        String targetLang = getTargetLang(textMessage);
        if (targetLang != null) {
            textMessage = textMessage.substring(targetLang.length() + 1);
        } else {
            if (!textMessage.isEmpty() && RU_ALPHABET.contains(textMessage.charAt(0))) {
                targetLang = "en";
            } else {
                targetLang = "ru";
            }
        }

        if (!textMessage.isEmpty()) {
            log.debug("Request to translate text from message: {}", textMessage);
            responseText = translateText(textMessage, targetLang);
            replyToMessage = message.getMessageId();
        } else {
            if (message.getReplyToMessage() == null) {
                log.debug("Empty request. Turning on command waiting");
                commandWaitingService.add(message, this.getClass());
                replyToMessage = message.getMessageId();
                responseText = "теперь напиши мне что нужно перевести";
            } else {
                String requestText = message.getReplyToMessage().getText();
                if (requestText == null) {
                    requestText = message.getReplyToMessage().getCaption();
                    if (requestText == null) {
                        throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                    }
                }

                responseText = translateText(requestText + " ", targetLang);
                replyToMessage = message.getReplyToMessage().getMessageId();
            }
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(replyToMessage);
        sendMessage.enableHtml(true);
        sendMessage.setText(responseText);

        return sendMessage;
    }

    /**
     * Translate text with Google Translate.
     *
     * @param requestText translatable text.
     * @param targetLang code of language into which the translation will be.
     * @return translated text.
     */
    private String translateText(String requestText, String targetLang) {
        String token = propertiesConfig.getGoogleTranslateToken();
        if (StringUtils.isEmpty(token)) {
            log.error("Unable to find google translate token");
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.UNABLE_TO_FIND_TOKEN));
        }

        String url = GOOGLE_TRANSLATE_URL + token + "/exec?" + "q=" + requestText + "&target=" + targetLang;
        ResponseEntity<TranslateResult> response;
        try {
            response = botRestTemplate.getForEntity(url, TranslateResult.class);
        } catch (RestClientException e) {
            log.error("Error receiving result of searching: ", e);
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        if (response.getBody() == null) {
            log.error("Empty result of translate");
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        return response.getBody().getText();
    }

    /**
     * Searching target language code in text.
     *
     * @param text search text.
     * @return language code.
     */
    private String getTargetLang(String text) {
        int i = text.indexOf(" ");
        if (i > 0 && i <= 3) {
            return LANG_CODE_LIST.stream().filter(text::startsWith).findFirst().orElse(null);
        }

        return null;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TranslateResult {
        private String text;
    }
}
