package org.telegram.bot.services.impl;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Chat;
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
    public NewsSource get(Chat chat, Long newsSourceId) {
        log.debug("Request to get NewsSource by its id: {} for Chat {}", newsSourceId, chat.getChatId());
        return newsSourceRepository.findByChatAndId(chat, newsSourceId);
    }

    @Override
    public NewsSource get(Chat chat, String newsSourceName) {
        log.debug("Request to get NewsSource by its name: {}  for Chat {}", newsSourceName, chat.getChatId());
        return newsSourceRepository.findByChatAndName(chat, newsSourceName);
    }

    @Override
    public NewsSource get(Chat chat, String newsSourceName, String newsSourceUrl) {
        log.debug("Request to get NewsSource by its name {} or url {} for Chat {}", newsSourceName, newsSourceUrl, chat.getChatId());
        return getAll(chat)
                .stream()
                .filter(newsSource -> newsSource.getName().equals(newsSourceName) || newsSource.getUrl().equals(newsSourceUrl))
                .findFirst().orElse(null);
    }

    @Override
    public List<NewsSource> getAll(Chat chat) {
        log.debug("Request to get all NewsSources for chat {}", chat.getChatId());
        return newsSourceRepository.findByChat(chat);
    }

    @Override
    public NewsSource save(NewsSource newsSource) {
        log.debug("Request to save NewsSource {} ", newsSource);
        return newsSourceRepository.save(newsSource);
    }

    @Override
    public Boolean remove(Chat chat, Long newsSourceId) {
        log.debug("Request to delete NewsSource by id {}", newsSourceId);
        NewsSource newsSource = get(chat, newsSourceId);
        if (newsSource == null) {
            return false;
        }

        newsSourceRepository.delete(newsSource);
        return true;
    }

    @Override
    public Boolean remove(Chat chat, String newsSourceName) {
        log.debug("Request to delete NewsSource by name {}", newsSourceName);
        NewsSource newsSource = get(chat, newsSourceName);
        if (newsSource == null) {
            return false;
        }

        newsSourceRepository.delete(newsSource);
        return true;
    }
}
