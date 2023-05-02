package org.telegram.bot.domain.commands;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.domain.CommandParent;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import static org.telegram.bot.utils.MathUtils.getRandomInRange;

@Component
@RequiredArgsConstructor
public class Truth implements CommandParent<PartialBotApiMethod<?>> {

    private final RestTemplate botRestTemplate;

    @Override
    public PartialBotApiMethod<?> parse(Update update) {
        Message message = getMessageFromUpdate(update);
        String textMessage = getTextMessage(update);
        Integer messageIdToReply = message.getMessageId();

        Message repliedMessage = message.getReplyToMessage();
        if (repliedMessage != null & textMessage == null) {
            messageIdToReply = repliedMessage.getMessageId();
        }

        int prob = getRandomInRange(0, 100);
        String responseText = buildResponseMessage(prob);

        InputFile gif = getGif(prob);
        if (gif != null) {
            SendDocument sendDocument = new SendDocument();
            sendDocument.setChatId(message.getChatId().toString());
            sendDocument.setCaption(responseText);
            sendDocument.setParseMode("Markdown");
            sendDocument.setReplyToMessageId(message.getMessageId());
            sendDocument.setDocument(gif);

            return sendDocument;
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(messageIdToReply);
        sendMessage.enableMarkdown(true);
        sendMessage.setText(responseText);

        return sendMessage;
    }

    private String buildResponseMessage(int prob) {
        String comment;

        if (prob == 0) {
            comment = "пиздит";
        } else if (prob > 0 && prob < 21) {
            comment = "врёт";
        } else if (prob > 20 && prob < 41) {
            comment = "привирает";
        } else if (prob > 40 && prob < 61) {
            comment = "ну чот хз";
        } else if (prob > 60 && prob < 81) {
            comment = "похоже на правду";
        } else if (prob > 80 && prob < 100) {
            comment = "дело говорит";
        } else {
            comment = "не пиздит";
        }

        return "Вероятность того, что выражение верно - *" + prob + "%*\n(" + comment + ")";
    }

    private InputFile getGif(int prob) {
        InputFile inputFile = null;

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
            inputFile = new InputFile(yesNo.getImage());
        }

        return inputFile;
    }

    private YesNo getYesNo(Answer answer) {
        final String apiUrl = "https://yesno.wtf/api";

        ResponseEntity<YesNo> responseEntity;
        try {
            responseEntity = botRestTemplate.getForEntity(apiUrl + "?force=" + answer.name().toLowerCase(), YesNo.class);
        } catch (RestClientException e) {
            return null;
        }

        return responseEntity.getBody();
    }

    @Data
    private static class YesNo {
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
