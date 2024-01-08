package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.TodoTag;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.repositories.TodoTagRepository;
import org.telegram.bot.services.TodoTagService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TodoTagServiceImpl implements TodoTagService {

    private final TodoTagRepository todoTagRepository;

    @Override
    public List<TodoTag> get(Chat chat, User user, List<String> tags) {
        log.debug("Request to get TodoTags for Chat {} and User {} by strings {}", chat, user, tags);
        return todoTagRepository.findByChatAndUserAndTagIn(tags);
    }
}
