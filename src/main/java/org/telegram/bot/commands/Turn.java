package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandPropertiesService;
import org.telegram.bot.services.InternationalizationService;
import org.telegram.bot.services.LanguageResolver;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.ObjectCopier;
import org.telegram.bot.utils.TextUtils;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.TextUtils.deleteWordsInText;

@Component
@RequiredArgsConstructor
@Slf4j
public class Turn implements Command, MessageAnalyzer {

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
                        .filter(TextUtils::isEmpty)
                        .map(Pattern::compile)
                        .collect(Collectors.toSet()));
    }

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        bot.sendTyping(message.getChatId());
        String commandArgument = message.getCommandArgument();
        Integer messageIdToReply = message.getMessageId();

        if (commandArgument == null) {
            Message repliedMessage = message.getReplyToMessage();
            if (repliedMessage != null) {
                commandArgument = repliedMessage.getText();
                messageIdToReply = repliedMessage.getMessageId();
            } else {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }
        }

        log.debug("Request to turn text: {}", commandArgument);
        String responseText = convert(commandArgument, languageResolver.getChatLanguageCode(request));

        return returnResponse(new TextResponse()
                .setChatId(message.getChatId())
                .setReplyToMessageId(messageIdToReply)
                .setText(responseText));
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
    public List<BotResponse> analyze(BotRequest request) {
        String textMessage = request.getMessage().getText();

        if (textMessage == null || textMessage.startsWith(this.getClass().getSimpleName().toLowerCase())) {
            return returnResponse();
        }
        log.debug("Initialization of unturned text search in {}", textMessage);
        
        textMessage = deleteWordsInText("@", textMessage);
        textMessage = deleteWordsInText("http", textMessage);

        if (!notNeedTurnPatternMatch(textMessage, languageResolver.getChatLanguageCode(request))) {
            Matcher matcher = UNTURNED_WORD_SYMPTOM.matcher(textMessage);
            if (matcher.find()) {
                String commandName = commandPropertiesService.getCommand(this.getClass()).getCommandName();
                BotRequest newRequest = objectCopier.copyObject(request, BotRequest.class);

                if (newRequest == null) {
                    return returnResponse();
                }
                newRequest.getMessage().setText(commandName + " " + textMessage);
                return this.parse(newRequest);
            }
        }

        return returnResponse();
    }

    private boolean notNeedTurnPatternMatch(String text, String lang) {
        if (EN_LANG_CODE.equals(lang)) {
            return true;
        }

        return NOT_NEED_TURN_PATTERNS.stream().map(pattern -> pattern.matcher(text)).map(Matcher::find).findFirst().orElse(false);
    }

}
