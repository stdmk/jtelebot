package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class Cmd implements Command {

    private final Bot bot;
    private final SpeechService speechService;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        bot.sendTyping(message.getChatId());
        String commandArgument = request.getMessage().getCommandArgument();
        if (commandArgument == null || commandArgument.isEmpty()) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
        String responseText;

        ProcessBuilder processBuilder = new ProcessBuilder(commandArgument.split(" "));
        log.debug("Request to execute {}", commandArgument);

        Process process;
        StringWriter writer = new StringWriter();
        try {
            process = processBuilder.start();
            IOUtils.copy(process.getInputStream(), writer, Charset.forName("cp866"));
            responseText = writer.toString();
            if (StringUtils.isEmpty(responseText)) {
                responseText = "executing...";
            }
        } catch (IOException e) {
            log.debug("Error while executing command {}", commandArgument);
            responseText = e.getMessage();
        }

        return returnResponse(new TextResponse(message)
                .setText("```" + responseText + "```")
                .setResponseSettings(FormattingStyle.MARKDOWN));
    }
}
