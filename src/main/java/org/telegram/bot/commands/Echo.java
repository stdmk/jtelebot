package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.TalkerPhrase;
import org.telegram.bot.domain.entities.TalkerWord;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.services.*;
import org.telegram.bot.utils.MathUtils;
import org.telegram.bot.utils.ObjectCopier;
import org.telegram.bot.utils.TextUtils;

import java.util.*;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class Echo implements Command, MessageAnalyzer {

    private final Bot bot;
    private final ObjectCopier objectCopier;
    private final SpeechService speechService;
    private final TalkerWordService talkerWordService;
    private final TalkerPhraseService talkerPhraseService;
    private final CommandPropertiesService commandPropertiesService;
    private final TalkerDegreeService talkerDegreeService;

    private static final Pattern WORDS_PATTERN = Pattern.compile("[а-яА-Я]{3,}", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern PHRASES_PATTERN = Pattern.compile("([^.!?),]+[.!?]?)", Pattern.UNICODE_CHARACTER_CLASS);

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        bot.sendTyping(message.getChatId());

        String responseText = getReplyForText(message.getCommandArgument(), message.getChatId());

        if (responseText == null) {
            responseText = speechService.getRandomMessageByTag(BotSpeechTag.ECHO);
        }

        return returnResponse(new TextResponse(message)
                .setText(responseText));
    }

    @Override
    public List<BotResponse> analyze(BotRequest request) {
        Message message = request.getMessage();
        if (message.isCallback()) {
            return returnResponse();
        }

        String textMessage = message.getText();
        if (textMessage == null) {
            return returnResponse();
        }
        parseTalkerData(message);

        if (isCommand(textMessage)) {
            return returnResponse();
        }

        boolean sendMessage = false;
        Message replyToMessage = message.getReplyToMessage();
        String botUsername = bot.getBotUsername();

        if (textMessage.startsWith("@" + botUsername)) {
            sendMessage = true;
        }
        else if (replyToMessage != null) {
            if (botUsername.equals(replyToMessage.getUser().getUsername())) {
                sendMessage = true;
            }
        } else {
            Integer degree = talkerDegreeService.get(message.getChatId()).getDegree();
            if (MathUtils.getRandomInRange(1, 100) <= degree) {
                sendMessage = true;
            }
        }

        if (sendMessage) {
            String commandName = commandPropertiesService.getCommand(this.getClass()).getCommandName();
            BotRequest newRequest = objectCopier.copyObject(request, BotRequest.class);
            newRequest.getMessage().setText(commandName + " " + textMessage);

            return this.parse(newRequest);
        }

        return returnResponse();
    }

    private boolean isCommand(String argument) {
        String potentialCommandInText = TextUtils.getPotentialCommandInText(argument);
        return potentialCommandInText != null && commandPropertiesService.getCommand(potentialCommandInText) != null;
    }

    public String getQuestionForText(String text, Long chatId) {
        if (text == null) {
            return null;
        }

        return getReply(talkerWordService.get(getWordsFromText(text), chatId)
                .stream()
                .map(TalkerWord::getPhrases)
                .flatMap(Collection::stream)
                .filter(talkerPhrase -> chatId.equals(talkerPhrase.getChat().getChatId()))
                .filter(talkerPhrase -> talkerPhrase.getPhrase().contains("?"))
                .collect(Collectors.toSet()));
    }

    private String getReplyForText(String text, Long chatId) {
        if (text == null) {
            return null;
        }

        return getReply(talkerWordService.get(getWordsFromText(text), chatId)
                .stream()
                .map(TalkerWord::getPhrases)
                .flatMap(Collection::stream)
                .filter(talkerPhrase -> chatId.equals(talkerPhrase.getChat().getChatId()))
                .collect(Collectors.toSet()));
    }

    private String getReply(Set<TalkerPhrase> talkerPhraseSet) {
        Map<String, Integer> phrasesRating = new HashMap<>();
        talkerPhraseSet.forEach(talkerPhrase -> {
            String phrase = talkerPhrase.getPhrase();
            if (phrasesRating.containsKey(phrase)) {
                phrasesRating.put(phrase, phrasesRating.get(phrase) + 1);
            } else {
                phrasesRating.put(phrase, 1);
            }
        });

        String selectedPhrase = null;
        Integer maxValue = phrasesRating.values().stream().max(Integer::compareTo).orElse(null);
        if (maxValue != null) {
            int premaxValue = maxValue - 2;
            List<String> talkerPhraseWithHighRatingList = phrasesRating.entrySet()
                    .stream()
                    .filter(entry -> entry.getValue() >= premaxValue)
                    .map(Map.Entry::getKey).toList();

            int talkerPhrasesWithHighRatingCount = talkerPhraseWithHighRatingList.size();
            if (talkerPhrasesWithHighRatingCount > 1) {
                selectedPhrase = talkerPhraseWithHighRatingList.get(MathUtils.getRandomInRange(0, talkerPhrasesWithHighRatingCount - 1));
            } else {
                selectedPhrase = talkerPhraseWithHighRatingList.get(0);
            }
        }

        return selectedPhrase;
    }

    private void parseTalkerData(Message message) {
        log.debug("Start parsing message {} for Talker data", message.getMessageId());

        Message messagesWithWords = message.getReplyToMessage();
        if (messagesWithWords == null) {
            log.debug("Reply message is missing");
            return;
        }

        List<String> words = getWordsFromText(messagesWithWords.getText());
        if (words.isEmpty()) {
            log.debug("Unable to find words in text {}", messagesWithWords.getText());
            return;
        }

        List<String> phrases = getPhrasesFromText(message.getText());
        if (phrases.isEmpty()) {
            log.debug("Unable to find phrases in text {}", message.getText());
            return;
        }

        Chat chat = new Chat().setChatId(message.getChatId());
        List<TalkerPhrase> storedTalkerPhraseList = talkerPhraseService.save(
                phrases
                        .stream()
                        .map(phrase -> new TalkerPhrase()
                                .setPhrase(phrase)
                                .setChat(chat))
                        .collect(Collectors.toSet()),
                chat);

        talkerWordService.save(words
                .stream()
                .map(word -> new TalkerWord().setWord(word).setPhrases(new HashSet<>(storedTalkerPhraseList)))
                .collect(Collectors.toSet()));
    }

    private List<String> getWordsFromText(String text) {
        return parseText(WORDS_PATTERN, text);
    }

    private List<String> getPhrasesFromText(String text) {
        return parseText(PHRASES_PATTERN, text);
    }

    private List<String> parseText(Pattern pattern, String text) {
        List<String> result = new ArrayList<>();

        if (text != null) {
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                result.add(text.substring(matcher.start(), matcher.end()));
            }
        }

        return result;
    }

}
