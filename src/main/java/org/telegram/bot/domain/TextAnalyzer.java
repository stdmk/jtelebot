package org.telegram.bot.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface TextAnalyzer {
    void analyze(Command<?> command, Update update);

    default Update copyUpdate(Update update) {
        Update newUpdate = null;

        ObjectMapper mapper = new ObjectMapper();
        try {
            String json = mapper.writeValueAsString(update);
            newUpdate = mapper.readValue(json, Update.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return newUpdate;
    }
}
