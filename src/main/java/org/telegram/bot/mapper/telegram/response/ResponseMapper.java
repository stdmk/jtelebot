package org.telegram.bot.mapper.telegram.response;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.FileResponse;
import org.telegram.bot.domain.model.response.FileType;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class ResponseMapper {

    private final List<TelegramTextApiMethodMapper> textApiMethodMappers;
    private final List<TelegramFileApiMethodMapper> fileApiMethodMappers;
    private final MediaGroupMapper mediaGroupMapper;
    private final BotStats botStats;

    private final Map<Class<? extends BotResponse>, TelegramTextApiMethodMapper> telegramTextApiMethodMapperMap = new ConcurrentHashMap<>();
    private final Map<FileType, TelegramFileApiMethodMapper> telegramFileApiMethodMapperMap = new ConcurrentHashMap<>();

    @PostConstruct
    private void postConstruct() {
        textApiMethodMappers.forEach(telegramTextApiMethodMapper ->
                telegramTextApiMethodMapperMap.put(telegramTextApiMethodMapper.getMappingClass(), telegramTextApiMethodMapper));
        fileApiMethodMappers.forEach(telegramFileApiMethodMapper ->
                telegramFileApiMethodMapperMap.put(telegramFileApiMethodMapper.getMappingFileType(), telegramFileApiMethodMapper));
    }

    public List<PartialBotApiMethod<?>> toTelegramMethod(List<BotResponse> responseList) {
        return responseList.stream().map(this::toTelegramMethod).collect(Collectors.toList());
    }

    public PartialBotApiMethod<?> toTelegramMethod(BotResponse response) {
        if (response instanceof FileResponse fileResponse) {
            return toMediaMethod(fileResponse);
        }

        TelegramTextApiMethodMapper telegramTextApiMethodMapper = telegramTextApiMethodMapperMap.get(response.getClass());
        if (telegramTextApiMethodMapper == null) {
            botStats.incrementErrors(response, "Unknown type of BotResponse: " + response.getClass());
            throw new IllegalArgumentException("Unknown type of BotResponse: " + response.getClass());
        }

        return telegramTextApiMethodMapper.map(response);
    }

    private PartialBotApiMethod<?> toMediaMethod(FileResponse fileResponse) {
        List<org.telegram.bot.domain.model.response.File> files = fileResponse.getFiles();
        if (files.size() > 1) {
            return mediaGroupMapper.map(fileResponse);
        }

        FileType responseFileType = files.get(0).getFileType();
        TelegramFileApiMethodMapper telegramFileApiMethodMapper = telegramFileApiMethodMapperMap.get(responseFileType);

        if (telegramFileApiMethodMapper == null) {
            botStats.incrementErrors(fileResponse, "Unable to find fileMapper for FileType: " + responseFileType);
            telegramFileApiMethodMapper = telegramFileApiMethodMapperMap.get(FileType.FILE);
        }

        return telegramFileApiMethodMapper.map(fileResponse);
    }

}
