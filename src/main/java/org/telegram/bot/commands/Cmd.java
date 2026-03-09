package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class Cmd implements Command {

    private static final boolean IS_WIN_OS = System.getProperty("os.name").toLowerCase().contains("win");
    private static final int TIMEOUT_SECONDS = 60;

    private final Bot bot;
    private final SpeechService speechService;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        bot.sendTyping(message.getChatId());

        String commandArgument = message.getCommandArgument();
        if (StringUtils.isBlank(commandArgument)) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        String responseText;
        ProcessBuilder processBuilder = getProcessBuilder(commandArgument);
        processBuilder.redirectErrorStream(true);
        log.debug("Request to execute {}", commandArgument);

        try {
            Process process = processBuilder.start();
            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                responseText = "timeout after " + TIMEOUT_SECONDS + "s";
            } else {
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                responseText = output.toString();
                if (StringUtils.isBlank(responseText)) responseText = "command executed";
            }
        } catch (Exception e) {
            log.debug("Error while executing command {}", commandArgument, e);
            responseText = e.getMessage();
        }

        return returnResponse(new TextResponse(message).setText("<pre><code class=\"language-shell\">" + responseText + "</code></pre>").setResponseSettings(FormattingStyle.HTML));
    }

    private ProcessBuilder getProcessBuilder(String commandArgument) {
        if (IS_WIN_OS) {
            return new ProcessBuilder("cmd.exe", "/c", commandArgument);
        } else {
            return new ProcessBuilder("bash", "-c", commandArgument);
        }
    }

}