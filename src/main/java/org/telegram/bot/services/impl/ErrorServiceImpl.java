package org.telegram.bot.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.bot.domain.entities.Error;
import org.telegram.bot.repositories.ErrorRepository;
import org.telegram.bot.services.ErrorService;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class ErrorServiceImpl implements ErrorService {

    private final ErrorRepository errorRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void save(PartialBotApiMethod<?> response, Throwable throwable, String comment) {
        save(null, response, throwable, comment);
    }

    @Override
    public void save(Object request, String comment) {
        save(request, null, null, comment);
    }

    @Override
    public void save(Object request, Throwable throwable, String comment) {
        save(request, null, throwable, comment);
    }

    @Override
    public Error get(long id) {
        return errorRepository.findById(id).orElse(null);
    }

    @Override
    public List<Error> getAll() {
        return errorRepository.findAll();
    }

    @Transactional
    @Override
    public void clear() {
        errorRepository.deleteAll();
    }

    @Override
    public void save(Object request, PartialBotApiMethod<?> response, Throwable throwable, String comment) {
        String requestJson = getStringOrEmpty(request, () -> getObjectJson(request));
        String responseJson = getStringOrEmpty(response, () -> getObjectJson(response));
        String stacktrace = getStringOrEmpty(throwable, () -> ExceptionUtils.getStackTrace(throwable));

        errorRepository.save(
                new Error()
                        .setDateTime(LocalDateTime.now())
                        .setRequest(requestJson)
                        .setResponse(responseJson)
                        .setComment(comment)
                        .setStacktrace(stacktrace));
    }

    private String getStringOrEmpty(Object value, Supplier<String> supplier) {
        if (value == null) {
            return "";
        }

        return supplier.get();
    }

    private String getObjectJson(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.error("Не удалось сериализовать", e);
            e.printStackTrace();
            return "";
        }
    }
}
