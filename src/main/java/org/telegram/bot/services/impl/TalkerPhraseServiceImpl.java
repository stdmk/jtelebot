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
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class TalkerPhraseServiceImpl implements TalkerPhraseService {

    private final TalkerPhraseRepository talkerPhraseRepository;

    @Override
    public List<TalkerPhrase> save(Set<TalkerPhrase> talkerPhraseSet) {
        Set<String> words = talkerPhraseSet.stream().map(TalkerPhrase::getPhrase).collect(Collectors.toSet());
        log.debug("Request to save TalkerWords {}", words);

        Set<TalkerPhrase> storedTalkerPhraseList = talkerPhraseRepository.findAllByPhraseInIgnoreCase(words);
        List<String> storedPhrases = storedTalkerPhraseList.stream().map(TalkerPhrase::getPhrase).collect(Collectors.toList());

        return Stream.concat(
                storedTalkerPhraseList.stream(),
                talkerPhraseRepository.saveAll(talkerPhraseSet
                        .stream()
                        .filter(talkerWord -> !storedPhrases.contains(talkerWord.getPhrase()))
                        .collect(Collectors.toSet())).stream()
                )
                .collect(Collectors.toList());
    }
}
