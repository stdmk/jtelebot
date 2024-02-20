package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.Command;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class Cmd implements Command<SendMessage> {

    private final Bot bot;
    private final SpeechService speechService;

    @Override
    public List<SendMessage> parse(Update update) {
        Message message = getMessageFromUpdate(update);
        bot.sendTyping(message.getChatId());
        String textMessage = cutCommandInText(message.getText());
        if (textMessage == null || textMessage.isEmpty()) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
        String responseText;

        ProcessBuilder processBuilder = new ProcessBuilder(textMessage.split(" "));
        log.debug("Request to execute {}", textMessage);

        Process process;
        StringWriter writer = new StringWriter();
        try {
            process = processBuilder.start();
            IOUtils.copy(process.getInputStream(), writer, Charset.forName("cp866"));
            responseText = writer.toString();
            if (StringUtils.isEmpty(responseText)) {
                responseText = "executing...";
            }
        } catch (IOException e) {
            log.debug("Error while executing command {}", textMessage);
            responseText = e.getMessage();
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId());
        sendMessage.enableMarkdown(true);
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText("`" + responseText + "`");

        return returnOneResult(sendMessage);
    }
}
