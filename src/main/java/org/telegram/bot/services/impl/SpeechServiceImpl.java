package org.telegram.bot.services.impl;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Speech;
import org.telegram.bot.repositories.SpeechRepository;
import org.telegram.bot.services.SpeechService;

import java.util.List;

import static org.telegram.bot.utils.MathUtils.getRandomInRange;

@Service
@AllArgsConstructor
public class SpeechServiceImpl implements SpeechService {

    private final Logger log = LoggerFactory.getLogger(SpeechServiceImpl.class);

    private final SpeechRepository speechRepository;

    @Override
    public String getRandomMessageByTag(String tag) {
        log.debug("Request to get random speech message by tag: {}", tag);
        List<Speech> speeches = speechRepository.findByTag(tag);
        if (speeches.isEmpty()) {
            return "что-то пошло не так";
        }

        return speeches.get(getRandomInRange(0, speeches.size())).getMessage();
    }
}
