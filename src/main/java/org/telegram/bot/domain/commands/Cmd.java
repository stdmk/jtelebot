package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.enums.ParseModes;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;

@Component
@AllArgsConstructor
public class Cmd implements CommandParent<SendMessage> {

    private final SpeechService speechService;

    @Override
    public SendMessage parse(Update update, String commandText) throws Exception {
        Message message = getMessageFromUpdate(update);
        String textMessage = cutCommandInText(message.getText());
        if (textMessage == null || textMessage.equals("")) {
            throw new BotException(speechService.getRandomMessageByTag("wrongInput"));
        }

        Process process;
        try {
            process = Runtime.getRuntime().exec(textMessage);
        } catch (IOException e) {
            throw new BotException(e.getMessage());
        }

        StringWriter writer = new StringWriter();
        IOUtils.copy(process.getInputStream(), writer, Charset.forName("cp866"));
        String responseText = writer.toString();

        return new SendMessage()
                .setChatId(message.getChatId())
                .setReplyToMessageId(message.getMessageId())
                .setParseMode(ParseModes.MARKDOWN.getValue())
                .setText("`" + responseText + "`");
    }
}
