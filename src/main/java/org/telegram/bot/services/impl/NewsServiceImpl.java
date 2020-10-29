package org.telegram.bot.services.impl;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.News;
import org.telegram.bot.repositories.NewsRepository;
import org.telegram.bot.services.NewsService;

import java.util.List;

@Service
@AllArgsConstructor
public class NewsServiceImpl implements NewsService {

    private final Logger log = LoggerFactory.getLogger(NewsServiceImpl.class);

    private final NewsRepository newsRepository;

    @Override
    public News get(Long newsId) {
        log.debug("Request to get News by id: {} ", newsId);
        return newsRepository.findById(newsId).orElse(null);
    }

    @Override
    public News save(News news) {
        log.debug("Request to save News {} ", news);
        return newsRepository.save(news);
    }

    @Override
    public List<News> save(List<News> newsList) {
        log.debug("Request to save News {} ", newsList);
        return newsRepository.saveAll(newsList);
    }
}
