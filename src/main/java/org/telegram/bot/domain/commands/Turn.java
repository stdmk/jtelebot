package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.Parser;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.TextAnalyzer;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandPropertiesService;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

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

        StringBuilder buf = new StringBuilder();
        if (textMessage == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        } else {
            for (char textChar : textMessage.toCharArray()) {
                try {
                    buf.append(ruLayout.charAt(enLayout.indexOf(textChar)));
                } catch (Exception ignored) {

                }
            }
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText(buf.toString());

        return sendMessage;
    }

    @Override
    public void analyze(Bot bot, CommandParent<?> command, Update update) {
        String messageText = getMessageFromUpdate(update).getText();

        if (isItMistakenText(messageText)) {
            String commandName = commandPropertiesService.getCommand(Turn.class);
            update.getMessage().setText(commandName + " " + messageText);

            Parser parser = new Parser(bot, command, update);
            parser.start();
        }
    }

    private Boolean isItMistakenText(String text) {
        if (text.startsWith("http")) {
            return false;
        }

        Pattern pattern = Pattern.compile("[а-яА-Я]+", Pattern.UNICODE_CHARACTER_CLASS);
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            pattern = Pattern.compile("[qwrtpsdfghjklzxcvbnm]{5}", Pattern.UNICODE_CHARACTER_CLASS);
            matcher = pattern.matcher(text);
            return matcher.find();
        }

        return false;
    }
}
