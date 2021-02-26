package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.Parser;
import org.telegram.bot.domain.BotStats;
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

import static org.telegram.bot.utils.TextUtils.deleteWordsInText;

@Component
@AllArgsConstructor
public class Turn implements CommandParent<SendMessage>, TextAnalyzer {

    private final SpeechService speechService;
    private final CommandPropertiesService commandPropertiesService;
    private final BotStats botStats;

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
    public void analyze(Bot bot, CommandParent<?> command,  Update update) {
        Message message = getMessageFromUpdate(update);
        String textMessage = message.getText();
        String mistakenText = getMistakenText(textMessage);

        if (mistakenText != null) {
            String commandName = commandPropertiesService.getCommand(this.getClass()).getCommandName();
            Update newUpdate = copyUpdate(update);
            if (newUpdate == null) {
                return;
            }

            newUpdate.getMessage().setText(commandName + " " + mistakenText);

            Parser parser = new Parser(bot, command, newUpdate, botStats);
            parser.start();
        }
    }

    private String getMistakenText(String text) {

        text = deleteWordsInText("@", text);

        text = deleteWordsInText("http", text);

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
