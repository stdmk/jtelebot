package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.NewsMessage;
import org.telegram.bot.repositories.NewsMessageRepository;
import org.telegram.bot.services.NewsMessageService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsMessageServiceImpl implements NewsMessageService {

    private final NewsMessageRepository newsMessageRepository;

    @Override
    public NewsMessage get(Long newsId) {
        log.debug("Request to get News by id: {} ", newsId);
        return newsMessageRepository.findById(newsId).orElse(null);
    }

    @Override
    public NewsMessage save(NewsMessage newsMessage) {
        log.debug("Request to save News {} ", newsMessage);
        return newsMessageRepository.save(newsMessage);
    }

    @Override
    public List<NewsMessage> save(List<NewsMessage> newsMessageList) {
        log.debug("Request to save News {} ", newsMessageList);
        return newsMessageRepository.saveAll(newsMessageList);
    }

}
