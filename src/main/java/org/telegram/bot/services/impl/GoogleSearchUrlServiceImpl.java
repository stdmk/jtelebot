package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.GoogleSearchResult;
import org.telegram.bot.repositories.GoogleSearchResultRepository;
import org.telegram.bot.services.GoogleSearchResultService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleSearchUrlServiceImpl implements GoogleSearchResultService {

    private final GoogleSearchResultRepository googleSearchResultRepository;

    @Override
    public GoogleSearchResult get(Long googleSearchResultId) {
        log.debug("Request to get GoogleSearchResult by Id: {}", googleSearchResultId);
        return googleSearchResultRepository.findById(googleSearchResultId).orElse(null);
    }

    @Override
    public GoogleSearchResult save(GoogleSearchResult googleSearchResult) {
        log.debug("Request to save GoogleSearchResult: {}", googleSearchResult);
        return googleSearchResultRepository.save(googleSearchResult);
    }

    @Override
    public List<GoogleSearchResult> save(List<GoogleSearchResult> googleSearchResultList) {
        log.debug("Request to save GoogleSearchResults: {}", googleSearchResultList);
        return googleSearchResultRepository.saveAll(googleSearchResultList);
    }
}
