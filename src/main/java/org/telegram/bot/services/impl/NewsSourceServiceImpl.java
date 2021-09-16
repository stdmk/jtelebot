package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.NewsSource;
import org.telegram.bot.repositories.NewsSourceRepository;
import org.telegram.bot.services.NewsSourceService;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsSourceServiceImpl implements NewsSourceService {

    private final NewsSourceRepository newsSourceRepository;

    @Override
    public NewsSource get(String url) {
        log.debug("Request to get NewsSource by its url: {}", url);
        return newsSourceRepository.findByUrl(url);
    }

    @Override
    public NewsSource save(NewsSource newsSource) {
        log.debug("Request to save NewsSource {}", newsSource);
        return newsSourceRepository.save(newsSource);
    }
}
