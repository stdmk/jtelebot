package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;

@Component
@AllArgsConstructor
public class Cmd implements CommandParent<SendMessage> {

    private final SpeechService speechService;

    @Override
    public SendMessage parse(Update update) throws Exception {
        Message message = getMessageFromUpdate(update);
        String textMessage = cutCommandInText(message.getText());
        if (textMessage == null || textMessage.equals("")) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        ProcessBuilder processBuilder = new ProcessBuilder(textMessage.split(" "));

        //TODO убрать
        if (textMessage.endsWith(".bat\"")) {
            processBuilder.directory(new File(textMessage.substring(1, textMessage.lastIndexOf("\\"))));
        }

        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            throw new BotException(e.getMessage());
        }

        StringWriter writer = new StringWriter();
        IOUtils.copy(process.getInputStream(), writer, Charset.forName("cp866"));
        String responseText = writer.toString();
        if (responseText.isEmpty()) {
            responseText = "executing...";
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.enableMarkdown(true);
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText("`" + responseText + "`");

        return sendMessage;
    }
}
