package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.FileResponse;
import org.telegram.bot.domain.model.response.FileType;

import java.io.File;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class Logs implements Command {

    private final Bot bot;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        if (message.hasCommandArgument()) {
            return returnResponse();
        }
        bot.sendUploadDocument(message.getChatId());

        Long chatId = message.getChatId();
        if (chatId < 0) {
            chatId = message.getUser().getUserId();
        }

        File logs = new File("logs/log.log");
        log.debug("Request to send logs to {}", chatId);

        return returnResponse(new FileResponse()
                .setChatId(chatId)
                .addFile(new org.telegram.bot.domain.model.response.File(FileType.FILE, logs)));
    }
}
