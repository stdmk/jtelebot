package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.TalkerWord;
import org.telegram.bot.repositories.TalkerWordRepository;
import org.telegram.bot.services.TalkerWordService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TalkerWordServiceImpl implements TalkerWordService {

    private final TalkerWordRepository talkerWordRepository;

    @Override
    public List<TalkerWord> save(Set<TalkerWord> talkerWordSet) {
        List<String> words = talkerWordSet.stream().map(TalkerWord::getWord).collect(Collectors.toList());
        log.debug("Request to save TalkerWords {}", words);

        Set<String> alreadyStoredWords = talkerWordRepository.findAllByWordInIgnoreCase(words).stream().map(TalkerWord::getWord).collect(Collectors.toSet());

        return talkerWordRepository.saveAll(talkerWordSet
                .stream()
                .filter(talkerWord -> !alreadyStoredWords.contains(talkerWord.getWord()))
                .collect(Collectors.toSet()));
    }

    @Override
    public Set<TalkerWord> get(List<String> words, Long chatId) {
        log.debug("Request to get TalkerWord for chat {} by words {}", chatId, String.join(", ", words));
        return talkerWordRepository.findAllByWordInIgnoreCaseAndPhrasesChatIdEq(words, chatId);
    }
}
