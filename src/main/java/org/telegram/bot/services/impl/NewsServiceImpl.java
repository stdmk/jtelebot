package org.telegram.bot.services.impl;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.News;
import org.telegram.bot.services.NewsService;

@Service
@AllArgsConstructor
public class NewsServiceImpl implements NewsService {
    @Override
    public News get(Long newsId) {
        return null;
    }

    @Override
    public News save(News news) {
        return null;
    }
}
