package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.telegram.bot.domain.entities.NewsMessage;
import org.telegram.bot.repositories.NewsMessageRepository;
import org.telegram.bot.services.NewsMessageService;
import org.telegram.bot.utils.DateUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsMessageServiceImpl implements NewsMessageService {

    private final NewsMessageRepository newsMessageRepository;

    @Override
    public NewsMessage getLastNewsMessage() {
        log.debug("Request to get last NewsMessage");
        return newsMessageRepository.findFirstByOrderByIdDesc();
    }

    @Override
    public NewsMessage get(Long newsId) {
        log.debug("Request to get News by id: {} ", newsId);
        return newsMessageRepository.findById(newsId).orElse(null);
    }

    @Override
    public List<NewsMessage> getAll(List<Long> newsIds) {
        log.debug("Request to get News by ids: {} ", newsIds);
        return newsMessageRepository.findAllById(newsIds);
    }

    @Override
    public List<NewsMessage> save(List<NewsMessage> newsMessageList) {
        log.debug("Request to save News {} ", newsMessageList);
        return newsMessageList.stream().map(this::save).toList();
    }

    @Override
    public NewsMessage save(NewsMessage newsMessage) {
        log.debug("Request to save News {} ", newsMessage);

        String descHash;
        String description = newsMessage.getDescription();
        if (StringUtils.hasText(description)) {
            descHash = DigestUtils.sha256Hex(description);
        } else {
            descHash = DigestUtils.sha256Hex(newsMessage.getTitle());
        }

        NewsMessage alreadyStoredNewsMessage = newsMessageRepository.findByDescHash(descHash);
        if (alreadyStoredNewsMessage != null && DateUtils.isDatesTheSame(alreadyStoredNewsMessage.getPubDate(), newsMessage.getPubDate())) {
            log.debug("NewsMessage with this description already saved");
            return alreadyStoredNewsMessage;
        }

        return newsMessageRepository.save(newsMessage);
    }

}
