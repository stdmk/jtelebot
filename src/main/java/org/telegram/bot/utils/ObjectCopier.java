package org.telegram.bot.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.BotStats;

@RequiredArgsConstructor
@Component
@Slf4j
public class ObjectCopier {

    private final ObjectMapper objectMapper;
    private final BotStats botStats;

    public <T> T copyObject(T object, Class<T> tClass) {
        T newObject = null;

        try {
            String json = objectMapper.writeValueAsString(object);
            newObject = objectMapper.readValue(json, tClass);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize/deserialize object");
            botStats.incrementErrors(object, "Failed to serialize/deserialize object");
            e.printStackTrace();
        }

        return newObject;
    }

}
