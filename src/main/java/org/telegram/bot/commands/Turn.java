package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.Command;
import org.telegram.bot.domain.MessageAnalyzer;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandPropertiesService;
import org.telegram.bot.services.InternationalizationService;
import org.telegram.bot.services.LanguageResolver;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.ObjectCopier;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.TextUtils.deleteWordsInText;

@Component
@RequiredArgsConstructor
@Slf4j
public class Turn implements Command<SendMessage>, MessageAnalyzer {

    private final Bot bot;
    private final ObjectCopier objectCopier;
    private final SpeechService speechService;
    private final CommandPropertiesService commandPropertiesService;
    private final LanguageResolver languageResolver;
    private final InternationalizationService internationalizationService;

    private static final HashSet<Pattern> NOT_NEED_TURN_PATTERNS = new HashSet<>();
    private static final String EN_LANG_CODE = "en";
    private static final String EN_LAYOUT = " 1234567890-=qwertyuiop[]asdfghjkl;'zxcvbnm,./\\!@#$%^&*()_+QWERTYUIOP{}ASDFGHJKL:\"ZXCVBNM<>?|";
    private static final Pattern UNTURNED_WORD_SYMPTOM =  Pattern.compile("[qwrtpsdfghjklzxcvbnm]{5}", Pattern.UNICODE_CHARACTER_CLASS);

    @PostConstruct
    private void postConstruct() {
        NOT_NEED_TURN_PATTERNS.addAll(
                internationalizationService.internationalize("${command.turn.pattern}")
                        .stream()
                        .filter(StringUtils::hasLength)
                        .map(Pattern::compile)
                        .collect(Collectors.toSet()));
    }

    @Override
    public SendMessage parse(Update update) {
        Message message = getMessageFromUpdate(update);
        bot.sendTyping(message.getChatId());
        String textMessage = cutCommandInText(message.getText());
        Integer messageIdToReply = message.getMessageId();

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
        String responseText = convert(textMessage, languageResolver.getChatLanguageCode(update));

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(messageIdToReply);
        sendMessage.setText(responseText);

        return sendMessage;
    }

    private String convert(String text, String lang) {
        String layout = " " + internationalizationService.internationalize("${command.turn.layout}", lang);

        if (isEnLayout(text)) {
            log.debug("Request to turn text: {} from EN to RU", text);
            return convert(EN_LAYOUT, layout, text);
        } else {
            log.debug("Request to turn text: {} from RU to EN", text);
            return convert(layout, EN_LAYOUT, text);
        }
    }

    private boolean isEnLayout(String text) {
        return EN_LAYOUT.contains(text.substring(0, 1));
    }

    private String convert(String fromLayout, String toLayout, String text) {
        StringBuilder buf = new StringBuilder();

        for (char textChar : text.toCharArray()) {
            try {
                buf.append(toLayout.charAt(fromLayout.indexOf(textChar)));
            } catch (Exception ignored) {
                // something wrong
            }
        }

        return buf.toString();
    }

    @Override
    public void analyze(Update update) {
        String textMessage;
        if (update.hasMessage()) {
            textMessage = update.getMessage().getText();
        } else if (update.hasEditedMessage()) {
            textMessage = update.getEditedMessage().getText();
        } else {
            return;
        }

        if (textMessage == null || textMessage.startsWith(this.getClass().getSimpleName().toLowerCase())) {
            return;
        }
        log.debug("Initialization of unturned text search in {}", textMessage);
        
        textMessage = deleteWordsInText("@", textMessage);
        textMessage = deleteWordsInText("http", textMessage);

        if (!notNeedTurnPatternMatch(textMessage, languageResolver.getChatLanguageCode(update))) {
            Matcher matcher = UNTURNED_WORD_SYMPTOM.matcher(textMessage);
            if (matcher.find()) {
                String commandName = commandPropertiesService.getCommand(this.getClass()).getCommandName();
                Update newUpdate = objectCopier.copyObject(update, Update.class);

                if (newUpdate == null) {
                    return;
                }
                newUpdate.getMessage().setText(commandName + " " + textMessage);
                bot.parseAsync(newUpdate, this);
            }
        }
    }

    private boolean notNeedTurnPatternMatch(String text, String lang) {
        if (EN_LANG_CODE.equals(lang)) {
            return true;
        }

        return NOT_NEED_TURN_PATTERNS.stream().map(pattern -> pattern.matcher(text)).map(Matcher::find).findFirst().orElse(false);
    }

}
