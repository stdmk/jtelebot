package org.telegram.bot.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.entities.NewsMessage;
import org.telegram.bot.repositories.NewsMessageRepository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Component
@Slf4j
public class DbUtils {

    private static final int BATCH_SIZE = 1000;

    private final NewsMessageRepository newsMessageRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @EventListener(ApplicationReadyEvent.class)
    public void checkDb() {
        checkNewsMessageDescHash();
    }

    private void checkNewsMessageDescHash() {
        List<NewsMessage> newsMessageWithoutDescHash = getNewsMessageWithoutDescHash();
        if (!newsMessageWithoutDescHash.isEmpty()) {
            log.info("Description of NewsMessage hashing started");
            LocalDateTime start = LocalDateTime.now();
            int count = 0;

            while (!newsMessageWithoutDescHash.isEmpty()) {
                count = count + newsMessageWithoutDescHash.size();
                newsMessageRepository.saveAll(
                        newsMessageWithoutDescHash
                                .stream()
                                .map(newsMessage -> newsMessage.setDescHash(DigestUtils.sha256Hex(newsMessage.getDescription())))
                                .toList());
                newsMessageWithoutDescHash = getNewsMessageWithoutDescHash();
            }

            log.info("NewsMessages updated: {} ({} sec)", count, Duration.between(start, LocalDateTime.now()).toSeconds());
        }
    }

    private List<NewsMessage> getNewsMessageWithoutDescHash() {
        return entityManager
                .createQuery("SELECT nm FROM NewsMessage nm WHERE nm.descHash IS NULL", NewsMessage.class)
                .setMaxResults(BATCH_SIZE)
                .getResultList();
    }

}
