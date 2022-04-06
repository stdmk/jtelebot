package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.TalkerPhrase;
import org.telegram.bot.repositories.TalkerPhraseRepository;
import org.telegram.bot.services.TalkerPhraseService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TalkerPhraseServiceImpl implements TalkerPhraseService {

    private final TalkerPhraseRepository talkerPhraseRepository;

    @Override
    public List<TalkerPhrase> save(Set<TalkerPhrase> talkerPhraseSet) {
        Set<String> words = talkerPhraseSet.stream().map(TalkerPhrase::getPhrase).collect(Collectors.toSet());
        log.debug("Request to save TalkerWords {}", words);

        Set<String> alreadyStoredWords = talkerPhraseRepository.findAllByPhraseInIgnoreCase(words).stream().map(TalkerPhrase::getPhrase).collect(Collectors.toSet());

        return talkerPhraseRepository.saveAll(talkerPhraseSet
                .stream()
                .filter(talkerWord -> !alreadyStoredWords.contains(talkerWord.getPhrase()))
                .collect(Collectors.toSet()));
    }
}
