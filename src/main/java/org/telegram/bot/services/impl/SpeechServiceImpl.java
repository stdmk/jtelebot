package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.services.SpeechService;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpeechServiceImpl implements SpeechService {

    @Override
    public String getRandomMessageByTag(BotSpeechTag tag) {
        log.debug("Request to get random speech message by tag: {}", tag);
        return "${speech." + tag.getValue() + "}";
    }
}
