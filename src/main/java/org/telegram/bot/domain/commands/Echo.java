package org.telegram.bot.domain.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.Parser;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.TextAnalyzer;
import org.telegram.bot.domain.entities.TalkerPhrase;
import org.telegram.bot.domain.entities.TalkerWord;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.services.CommandPropertiesService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.TalkerDegreeService;
import org.telegram.bot.services.TalkerPhraseService;
import org.telegram.bot.services.TalkerWordService;
import org.telegram.bot.utils.MathUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class Echo implements CommandParent<SendMessage>, TextAnalyzer {

    private final SpeechService speechService;
    private final TalkerWordService talkerWordService;
    private final TalkerPhraseService talkerPhraseService;
    private final CommandPropertiesService commandPropertiesService;
    private final BotStats botStats;
    private final TalkerDegreeService talkerDegreeService;

    @Override
    public SendMessage parse(Update update) {
        Message message = getMessageFromUpdate(update);
        String textMessage = message.getText();

        String responseText = getReplyForText(cutCommandInText(textMessage));

        if (responseText == null) {
            responseText = speechService.getRandomMessageByTag(BotSpeechTag.ECHO);
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText(responseText);

        return sendMessage;
    }

    @Override
    public void analyze(Bot bot, CommandParent<?> command, Update update) {
        if (update.hasCallbackQuery()) {
            return;
        }

        Message message = getMessageFromUpdate(update);
        String textMessage = message.getText();
        parseTalkerData(message);

        boolean sendMessage = false;
        Message replyToMessage = message.getReplyToMessage();
        if (replyToMessage != null) {
            if (bot.getBotUsername().equals(replyToMessage.getFrom().getUserName())) {
                sendMessage = true;
            }
        }

        Integer degree = talkerDegreeService.get(message.getChatId()).getDegree();
        if (!sendMessage && MathUtils.getRandomInRange(1, 100) <= degree) {
            sendMessage = true;
        }

        if (sendMessage) {
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

    private String getReplyForText(String text) {
        if (text == null) {
            return null;
        }

        Map<TalkerPhrase, Integer> phrasesRating = new HashMap<>();
        talkerWordService.get(getWordsFromText(text))
                .stream()
                .map(TalkerWord::getPhrases)
                .flatMap(Collection::stream)
                .forEach(talkerPhrase -> {
                    if (phrasesRating.containsKey(talkerPhrase)) {
                        phrasesRating.put(talkerPhrase, phrasesRating.get(talkerPhrase) + 1);
                    } else {
                        phrasesRating.put(talkerPhrase, 1);
                    }
                });
        Optional<Map.Entry<TalkerPhrase, Integer>> optionalTalkerPhrase = phrasesRating.entrySet().stream().max(Map.Entry.comparingByValue());

        return optionalTalkerPhrase.map(talkerPhraseIntegerEntry -> talkerPhraseIntegerEntry.getKey().getPhrase()).orElse(null);
    }

    private void parseTalkerData(Message message) {
        log.debug("Start parsing message {} for Talker data", message.getMessageId());

        if (message.getText() == null) {
            log.debug("Empty message. Nothing to parse");
            return;
        }

        Message messagesWithWords = message.getReplyToMessage();
        if (messagesWithWords == null) {
            log.debug("Reply message is missing");
            return;
        }

        List<String> words = getWordsFromText(messagesWithWords.getText());
        if (words.isEmpty()) {
            log.info("Unable to find words in text {}", messagesWithWords.getText());
            return;
        }

        List<String> phrases = getPhrasesFromText(message.getText());
        if (phrases.isEmpty()) {
            log.info("Unable to find phrases in text {}", message.getText());
            return;
        }

        List<TalkerPhrase> storedTalkerPhraseList = talkerPhraseService.save(phrases
                .stream()
                .map(phrase -> new TalkerPhrase().setPhrase(phrase))
                .collect(Collectors.toSet()));

        talkerWordService.save(words
                .stream()
                .map(word -> new TalkerWord().setWord(word).setPhrases(new HashSet<>(storedTalkerPhraseList)))
                .collect(Collectors.toSet()));
    }

    private List<String> getWordsFromText(String text) {
        return parseText("[а-яА-Я]+", text);
    }

    private List<String> getPhrasesFromText(String text) {
        return parseText("([^.!?),]+[.!?]?)", text);
    }

    private List<String> parseText(String regex, String text) {
        List<String> result = new ArrayList<>();

        if (text != null) {
            Pattern pattern = Pattern.compile(regex, Pattern.UNICODE_CHARACTER_CLASS);
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                result.add(text.substring(matcher.start(), matcher.end()));
            }
        }

        return result;
    }
}
