package org.telegram.bot.domain.commands;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.TextAnalyzer;
import org.telegram.bot.domain.entities.TalkerPhrase;
import org.telegram.bot.domain.entities.TalkerWord;
import org.telegram.bot.services.TalkerPhraseService;
import org.telegram.bot.services.TalkerWordService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class Talker implements CommandParent<SendMessage>, TextAnalyzer {

    private final TalkerWordService talkerWordService;
    private final TalkerPhraseService talkerPhraseService;

    @Override
    public SendMessage parse(Update update) {
        importDataFromFile();
        return null;
    }

    @Override
    public void analyze(Bot bot, CommandParent<?> command, Update update) {
        //importDataFromFile();
    }

    private void importDataFromFile() {
        JsonData jsonData;
        try {
            InputStream is = new FileInputStream("result.json");
            ObjectMapper objectMapper = new ObjectMapper();

            jsonData = objectMapper.readValue(IOUtils.toString(is, StandardCharsets.UTF_8), JsonData.class);
        } catch (IOException e) {
            return;
        }
        
        List<TelegramMessage> messages = jsonData.getMessages();
        messages.forEach(message -> {
            Integer messageId = message.getId();
            System.out.println(messageId);

            if (!(message.getText() instanceof String)) {
                return;
            }

            TelegramMessage messageWithWords = getMessageById(messages, message.getReplyToMessageId());
            if (messageWithWords == null || !(messageWithWords.getText() instanceof String)) {
                return;
            }

            List<String> words = getWordsFromText((String) messageWithWords.getText());
            if (words.isEmpty()) {
                return;
            }

            List<String> phrases = getPhrasesFromText((String) message.getText());
            if (phrases.isEmpty()) {
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
        });
    }

    private TelegramMessage getMessageById(List<TelegramMessage> messages, Integer id) {
        if (id == null) {
            return null;
        }
        Optional<TelegramMessage> optionalMessage = messages.stream().filter(message -> id.equals(message.getId())).findFirst();
        return optionalMessage.orElse(null);
    }

    private List<String> getWordsFromText(String text) {
        return parseText("[а-яА-Я]+", text);
    }

    private List<String> getPhrasesFromText(String text) {
        return parseText("([^.!?),]+[.!?]?)", text);
    }
    
    private List<String> parseText(String regex, String text) {
        List<String> result = new ArrayList<>();

        Pattern pattern = Pattern.compile(regex, Pattern.UNICODE_CHARACTER_CLASS);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            result.add(text.substring(matcher.start(), matcher.end()));
        }

        return result;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class JsonData {
        private List<TelegramMessage> messages;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TelegramMessage {
        private Integer id;

        @JsonProperty("reply_to_message_id")
        private Integer replyToMessageId;

        private Object text;
    }
}
