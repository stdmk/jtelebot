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
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.FormattingStyle;

import java.util.List;
import java.util.Locale;

import static org.telegram.bot.utils.MathUtils.getRandomInRange;

@Component
@RequiredArgsConstructor
@Slf4j
public class Truth implements Command {

    private static final String API_URL = "https://yesno.wtf/api";

    private final Bot bot;
    private final RestTemplate botRestTemplate;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        bot.sendTyping(message.getChatId());
        String commandArgument = message.getCommandArgument();
        Integer messageIdToReply = message.getMessageId();

        Message repliedMessage = message.getReplyToMessage();
        if (repliedMessage != null && commandArgument == null) {
            messageIdToReply = repliedMessage.getMessageId();
        }

        int prob = getRandomInRange(0, 100);
        String responseText = buildResponseMessage(prob);

        File gif = getGif(prob);
        if (gif != null) {
            return returnResponse(new FileResponse()
                    .setChatId(message.getChatId())
                    .setReplyToMessageId(messageIdToReply)
                    .addFile(gif)
                    .setText(responseText)
                    .setResponseSettings(FormattingStyle.MARKDOWN));
        }

        return returnResponse(new TextResponse()
                .setChatId(message.getChatId())
                .setReplyToMessageId(messageIdToReply)
                .setText(responseText)
                .setResponseSettings(FormattingStyle.MARKDOWN));
    }

    private String buildResponseMessage(int prob) {
        String comment;

        if (prob == 0) {
            comment = "${command.truth.cunt}";
        } else if (prob > 0 && prob < 21) {
            comment = "${command.truth.lies}";
        } else if (prob > 20 && prob < 41) {
            comment = "${command.truth.lieslittle}";
        } else if (prob > 40 && prob < 61) {
            comment = "${command.truth.doubtfully}";
        } else if (prob > 60 && prob < 81) {
            comment = "${command.truth.truly}";
        } else if (prob > 80 && prob < 100) {
            comment = "${command.truth.exactly}";
        } else {
            comment = "${command.truth.fuckingtruth}";
        }

        return "${command.truth.caption} - *" + prob + "%*\n(" + comment + ")";
    }

    private File getGif(int prob) {
        File file = null;

        Answer answer;
        if (prob > 60) {
            answer = Answer.YES;
        } else if (prob < 41) {
            answer = Answer.NO;
        } else {
            answer = Answer.MAYBE;
        }

        YesNo yesNo = getYesNo(answer);
        if (yesNo != null) {
            file = new File(FileType.FILE, yesNo.getImage());
        }

        return file;
    }

    private YesNo getYesNo(Answer answer) {
        ResponseEntity<YesNo> responseEntity;
        try {
            responseEntity = botRestTemplate.getForEntity(API_URL + "?force=" + answer.name().toLowerCase(Locale.ROOT), YesNo.class);
        } catch (RestClientException e) {
            log.error("Failed to get gif from api: {}", e.getMessage());
            return null;
        }

        return responseEntity.getBody();
    }

    @Data
    @Accessors(chain = true)
    public static class YesNo {
        private String answer;
        private Boolean forced;
        private String image;
    }

    private enum Answer {
        YES,
        NO,
        MAYBE
    }

}
