package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Speech;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.repositories.SpeechRepository;
import org.telegram.bot.services.SpeechService;

import java.util.List;

import static org.telegram.bot.utils.MathUtils.getRandomInRange;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpeechServiceImpl implements SpeechService {

    private final SpeechRepository speechRepository;

    @Override
    public String getRandomMessageByTag(BotSpeechTag tag) {
        log.debug("Request to get random speech message by tag: {}", tag);

        List<Speech> speeches = speechRepository.findByTag(tag.getValue());
        if (speeches.isEmpty()) {
            return "Internal error";
        }

        return speeches.get(getRandomInRange(0, speeches.size())).getMessage();
    }
}
