package org.telegram.bot.commands;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class Speller implements Command {

    public static final String SPELLER_API_URL = "https://speller.yandex.net/services/spellservice.json/checkText?text=";

    private final Bot bot;
    private final CommandWaitingService commandWaitingService;
    private final RestTemplate botRestTemplate;
    private final SpeechService speechService;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        bot.sendTyping(message.getChatId());
        Integer replyToMessage;

        String commandArgument = commandWaitingService.getText(message);

        String responseText;
        if (commandArgument == null) {
            Message repliedMessage = message.getReplyToMessage();
            if (repliedMessage == null) {
                commandWaitingService.add(message, this.getClass());
                responseText = "${command.speller.commandwaitingstart}";
                replyToMessage = message.getMessageId();
            } else {
                commandArgument = repliedMessage.getText();
                if (commandArgument == null) {
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                } else {
                    log.debug("Request to speller text from replied message {}", commandArgument);
                    responseText = getRevisedText(commandArgument);
                    replyToMessage = message.getReplyToMessage().getMessageId();
                }
            }
        } else {
            replyToMessage = message.getMessageId();
            responseText = getRevisedText(commandArgument);
        }

        return returnResponse(new TextResponse()
                .setChatId(message.getChatId())
                .setReplyToMessageId(replyToMessage)
                .setText(responseText)
                .setResponseSettings(FormattingStyle.HTML));
    }

    /**
     * Sending text to Speller for review.
     *
     * @param text for review
     * @return revised text
     */
    private String getRevisedText(String text) {
        ResponseEntity<SpellResult[]> response;

        try {
            response = botRestTemplate.getForEntity(SPELLER_API_URL + text, SpellResult[].class);
        } catch (RestClientException e) {
            log.error("Error getting Speller review: ", e);
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE));
        }

        SpellResult[] body = response.getBody();
        if (body == null || body.length == 0) {
            return "${command.speller.noerrorsfound}";
        }

        StringBuilder buf = new StringBuilder("<u>${command.speller.errorsfound}</u>\n");
        Arrays.asList(body).forEach(spellResult -> buf
                .append("<s>").append(spellResult.getWord()).append("</s> — ").append(String.join(", ", spellResult.getS())).append("\n")
        );

        return buf.toString();
    }

    @Data
    @Accessors(chain = true)
    public static class SpellResult {
        private Integer code;
        private Integer pos;
        private Integer row;
        private Integer col;
        private Integer len;
        private String word;
        private List<String> s;
    }
}
