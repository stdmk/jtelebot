package org.telegram.bot.domain.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
@Slf4j
public class Turn implements CommandParent<SendMessage>, TextAnalyzer {

    private final SpeechService speechService;
    private final CommandPropertiesService commandPropertiesService;
    private final BotStats botStats;

    @Override
    public SendMessage parse(Update update) {
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

        log.debug("Request to turn text: {}", textMessage);
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
        String textMessage = getTextMessage(update);
        if (textMessage == null) {
            return;
        }
        log.debug("Initialization of unturned text search in {}", textMessage);
        
        textMessage = deleteWordsInText("@", textMessage);
        textMessage = deleteWordsInText("http", textMessage);

        Pattern pattern = Pattern.compile("[а-яА-Я]+", Pattern.UNICODE_CHARACTER_CLASS);
        Matcher matcher = pattern.matcher(textMessage);
        if (!matcher.find()) {
            pattern = Pattern.compile("[qwrtpsdfghjklzxcvbnm]{5}", Pattern.UNICODE_CHARACTER_CLASS);
            matcher = pattern.matcher(textMessage);
            if (matcher.find()) {
                String commandName = commandPropertiesService.getCommand(this.getClass()).getCommandName();
                Update newUpdate = copyUpdate(update);

                if (newUpdate == null) {
                    return;
                }
                newUpdate.getMessage().setText(commandName + " " + textMessage);

                Parser parser = new Parser(bot, command, newUpdate, botStats);
                parser.start();
            }
        }
    }
}
