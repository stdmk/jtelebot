package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.TalkerWord;
import org.telegram.bot.repositories.TalkerWordRepository;
import org.telegram.bot.services.TalkerWordService;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class TalkerWordServiceImpl implements TalkerWordService {

    private final TalkerWordRepository talkerWordRepository;

    @Override
    public void save(Set<TalkerWord> talkerWordSet) {
        List<String> words = talkerWordSet.stream().map(TalkerWord::getWord).toList();
        log.debug("Request to save TalkerWords {}", words);

        Set<TalkerWord> storedTalkerWordList = talkerWordRepository.findAllByWordInIgnoreCase(words);
        Set<String> storedWords = storedTalkerWordList.stream().map(TalkerWord::getWord).collect(Collectors.toSet());

        storedTalkerWordList.forEach(talkerWord -> talkerWord.setPhrases(
                Stream.concat(
                        talkerWord.getPhrases().stream(),
                        talkerWordSet.stream().map(TalkerWord::getPhrases).flatMap(Collection::stream)).collect(Collectors.toSet())));

        talkerWordRepository.saveAll(
                Stream.concat(
                                storedTalkerWordList.stream(),
                                talkerWordSet
                                        .stream()
                                        .filter(talkerWord -> !storedWords.contains(talkerWord.getWord())))
                        .toList());
    }

    @Override
    public Set<TalkerWord> get(List<String> words, Long chatId) {
        log.debug("Request to get TalkerWord for chat {} by words {}", chatId, String.join(", ", words));
        words = words.stream().map(String::toLowerCase).toList();
        return talkerWordRepository.findAllByWordInIgnoreCaseAndPhrasesChatIdEq(words, chatId);
    }
}
