package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.TalkerDegree;
import org.telegram.bot.repositories.TalkerDegreeRepository;
import org.telegram.bot.services.TalkerDegreeService;

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
                    .setChat(new Chat().setChatId(chatId)));
        }

        return talkerDegree;
    }

    @Override
    public TalkerDegree save(TalkerDegree talkerDegree) {
        log.debug("Request to save TalkerDegree: {}", talkerDegree);
        return talkerDegreeRepository.save(talkerDegree);
    }
}
