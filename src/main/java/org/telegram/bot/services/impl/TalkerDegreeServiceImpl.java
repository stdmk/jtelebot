package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.TalkerDegree;
import org.telegram.bot.repositories.TalkerDegreeRepository;
import org.telegram.bot.services.TalkerDegreeService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TalkerDegreeServiceImpl implements TalkerDegreeService {

    private final TalkerDegreeRepository talkerDegreeRepository;

    @Override
    public TalkerDegree get(Long chatId) {
        log.debug("Request to get TalkerDegree by chatId: {}", chatId);

        TalkerDegree talkerDegree = talkerDegreeRepository.findByChat(new Chat().setChatId(chatId));
        if (talkerDegree == null) {
            talkerDegree = talkerDegreeRepository.save(new TalkerDegree()
                    .setDegree(0)
                    .setChatIdleMinutes(0)
                    .setChat(new Chat().setChatId(chatId)));
        }

        return talkerDegree;
    }

    @Override
    public List<TalkerDegree> getAllWithChatIdleParam() {
        log.debug("Request to get TalkerDegree entities with chat idle param");
        return talkerDegreeRepository.findAllByChatIdleMinutesGreaterThan(0);
    }

    @Override
    public void save(TalkerDegree talkerDegree) {
        log.debug("Request to save TalkerDegree: {}", talkerDegree);
        talkerDegreeRepository.save(talkerDegree);
    }
}
