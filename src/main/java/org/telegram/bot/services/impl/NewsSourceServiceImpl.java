package org.telegram.bot.services.impl;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.NewsSource;
import org.telegram.bot.repositories.NewsSourceRepository;
import org.telegram.bot.services.NewsSourceService;

import java.util.List;

@Service
@AllArgsConstructor
public class NewsSourceServiceImpl implements NewsSourceService {

    private final Logger log = LoggerFactory.getLogger(NewsSourceServiceImpl.class);

    private final NewsSourceRepository newsSourceRepository;

    @Override
    public NewsSource get(Long newsSourceId) {
        log.debug("Request to get NewsSource by its id: {} ", newsSourceId);
        return newsSourceRepository.findById(newsSourceId).orElse(null);
    }

    @Override
    public NewsSource get(String newsSourceName) {
        log.debug("Request to get NewsSource by its name: {} ", newsSourceName);
        return newsSourceRepository.findByName(newsSourceName);
    }

    @Override
    public NewsSource get(String newsSourceName, String newsSourceUrl) {
        log.debug("Request to get NewsSource by its name {} or url {}", newsSourceName, newsSourceUrl);
        return newsSourceRepository.findFirstByNameOrUrl(newsSourceName, newsSourceUrl);
    }

    @Override
    public List<NewsSource> getAll() {
        log.debug("Request to get all NewsSources");
        return newsSourceRepository.findAll();
    }

    @Override
    public NewsSource save(NewsSource newsSource) {
        log.debug("Request to save NewsSource {} ", newsSource);
        return newsSourceRepository.save(newsSource);
    }

    @Override
    public Boolean remove(Long newsSourceId) {
        log.debug("Request to delete NewsSource by id {}", newsSourceId);
        NewsSource newsSource = get(newsSourceId);
        if (newsSource == null) {
            return false;
        }

        newsSourceRepository.delete(newsSource);
        return true;
    }

    @Override
    public Boolean remove(String newsSourceName) {
        log.debug("Request to delete NewsSource by name {}", newsSourceName);
        NewsSource newsSource = get(newsSourceName);
        if (newsSource == null) {
            return false;
        }

        newsSourceRepository.delete(newsSource);
        return true;
    }
}
