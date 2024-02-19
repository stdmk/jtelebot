package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.NewsSource;
import org.telegram.bot.repositories.NewsSourceRepository;
import org.telegram.bot.services.NewsSourceService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsSourceServiceImpl implements NewsSourceService {

    private final NewsSourceRepository newsSourceRepository;

    @Override
    public NewsSource get(Long newsSourceId) {
        log.debug("Request to get NewsSource by id: {}", newsSourceId);
        return newsSourceRepository.findById(newsSourceId).orElse(null);
    }

    @Override
    public NewsSource get(String url) {
        log.debug("Request to get NewsSource by its url: {}", url);
        return newsSourceRepository.findByUrl(url);
    }

    @Override
    public Page<NewsSource> getAll(int page) {
        log.debug("Request to get all NewsSource. Page " + page);
        return newsSourceRepository.findAll(PageRequest.of(page, 10));
    }

    @Override
    public List<NewsSource> getAll() {
        log.debug("Request to get all NewsSource");
        return newsSourceRepository.findAll();
    }

    @Override
    public NewsSource save(NewsSource newsSource) {
        log.debug("Request to save NewsSource {}", newsSource);
        return newsSourceRepository.save(newsSource);
    }

    @Override
    public void remove(NewsSource newsSource) {
        log.debug("Request to remove NewsSource {}", newsSource);
        newsSourceRepository.delete(newsSource);
    }
}
