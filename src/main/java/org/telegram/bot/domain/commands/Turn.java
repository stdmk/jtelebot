package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.TextAnalyzer;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandPropertiesService;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@AllArgsConstructor
public class Turn implements CommandParent<SendMessage>, TextAnalyzer {

    private final SpeechService speechService;
    private final CommandPropertiesService commandPropertiesService;

    @Override
    public SendMessage parse(Update update) throws Exception {
        final String ruLayout = " 1234567890-=йцукенгшщзхъфывапролджэячсмитьбю.\\!\"№;%:?*()_+ЙЦУКЕНГШЩЗХЪФЫВАПРОЛДЖЭЯЧСМИТЬБЮ,/";
        final String enLayout = " 1234567890-=qwertyuiop[]asdfghjkl;'zxcvbnm,./\\!@#$%^&*()_+QWERTYUIOP{}ASDFGHJKL:\"ZXCVBNM<>?|";
        Message message = getMessageFromUpdate(update);
        String textMessage = cutCommandInText(message.getText());
        Integer messageIdToReply = message.getMessageId();

        StringBuilder buf = new StringBuilder();
        if (textMessage == null) {
            Message repliedMessage = message.getReplyToMessage();
            if (repliedMessage != null) {
                textMessage = repliedMessage.getText();
                messageIdToReply = repliedMessage.getMessageId();
            } else {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }
        }
        for (char textChar : textMessage.toCharArray()) {
            try {
                buf.append(ruLayout.charAt(enLayout.indexOf(textChar)));
            } catch (Exception ignored) {

            }
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(messageIdToReply);
        sendMessage.setText(buf.toString());

        return sendMessage;
    }

    @Override
    public void analyze(Bot bot, Update update) {
        Message message = getMessageFromUpdate(update);
        String textMessage = message.getText();
        String mistakenText = getMistakenText(textMessage);

        if (mistakenText != null) {
            String commandName = commandPropertiesService.getCommand(this.getClass()).getCommandName();
            message.setText(commandName + " " + mistakenText);

            try {
                bot.execute(parse(update));
            } catch (Exception e) {
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(message.getChatId().toString());
                sendMessage.setReplyToMessageId(message.getMessageId());
                sendMessage.setText(e.getMessage());

                try {
                    bot.execute(sendMessage);
                } catch (TelegramApiException telegramApiException) {
                    telegramApiException.printStackTrace();
                }
            }

            message.setText(textMessage);
        }
    }

    private String getMistakenText(String text) {
        while (text.startsWith("@")) {
            int i = text.indexOf(" ");
            if (i < 0) {
                return null;
            }
            text = text.substring(i + 1);
        }

        if (text.startsWith("http")) {
            return null;
        }

        Pattern pattern = Pattern.compile("[а-яА-Я]+", Pattern.UNICODE_CHARACTER_CLASS);
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            pattern = Pattern.compile("[qwrtpsdfghjklzxcvbnm]{5}", Pattern.UNICODE_CHARACTER_CLASS);
            matcher = pattern.matcher(text);
            if (matcher.find()) {
                return text;
            }
        }

        return null;
    }
}
