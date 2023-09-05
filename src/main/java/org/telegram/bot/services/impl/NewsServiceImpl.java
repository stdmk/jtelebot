package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.News;
import org.telegram.bot.domain.entities.NewsSource;
import org.telegram.bot.repositories.NewsRepository;
import org.telegram.bot.services.NewsService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsServiceImpl implements NewsService {

    private final NewsRepository newsRepository;

    @Override
    public News get(Chat chat, Long newsId) {
        log.debug("Request to get News by its id: {} for Chat {}", newsId, chat.getChatId());
        return newsRepository.findByChatAndId(chat, newsId);
    }

    @Override
    public News get(Chat chat, String newsName) {
        log.debug("Request to get News by its name: {}  for Chat {}", newsName, chat.getChatId());
        return newsRepository.findByChatAndNameIgnoreCase(chat, newsName);
    }

    @Override
    public News get(Chat chat, String newsName, NewsSource newsSource) {
        log.debug("Request to get News by its name {} or url {} for Chat {}", newsName, newsSource.getUrl(), chat.getChatId());
        return getAll(chat)
                .stream()
                .filter(news -> news.getName().equals(newsName) || news.getNewsSource().equals(newsSource))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<News> getAll() {
        return newsRepository.findAll();
    }

    @Override
    public List<News> getAll(Chat chat) {
        log.debug("Request to get all News for chat {}", chat.getChatId());
        return newsRepository.findByChat(chat);
    }

    @Override
    public List<News> getAll(NewsSource newsSource) {
        log.debug("Request to get all News by News {}", newsSource);
        return newsRepository.findByNewsSource(newsSource);
    }

    @Override
    public void save(News news) {
        log.debug("Request to save News {} ", news);
        newsRepository.save(news);
    }

    @Override
    public Boolean remove(Chat chat, Long newsId) {
        log.debug("Request to delete News by id {}", newsId);

        News news = get(chat, newsId);
        if (news == null) {
            return false;
        }

        newsRepository.delete(news);
        return true;
    }

    @Override
    public Boolean remove(Chat chat, String newsName) {
        log.debug("Request to delete News by name {}", newsName);

        News news = get(chat, newsName);
        if (news == null) {
            return false;
        }

        newsRepository.delete(news);
        return true;
    }

    @Override
    public void remove(Long newsId) {
        log.debug("Request to delete News by id {}", newsId);
        newsRepository.deleteById(newsId);
    }
}
