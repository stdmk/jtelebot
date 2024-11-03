package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.InternationalizationService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.DateUtils;
import org.telegram.bot.utils.TextUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class TimeDownloading implements Command {

    private final Bot bot;
    private final CommandWaitingService commandWaitingService;
    private final SpeechService speechService;
    private final InternationalizationService internationalizationService;
    private final Map<Long, Set<String>> weightNamesMultiplierMap = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> speedNamesMultiplierMap = new ConcurrentHashMap<>();

    @PostConstruct
    private void postConstruct() {
        weightNamesMultiplierMap.put(1L, internationalizationService.getAllTranslations("command.timedownloading.weight.b"));
        weightNamesMultiplierMap.put(1024L, internationalizationService.getAllTranslations("command.timedownloading.weight.kb"));
        weightNamesMultiplierMap.put(1024L * 1024L, internationalizationService.getAllTranslations("command.timedownloading.weight.mb"));
        weightNamesMultiplierMap.put(1024L * 1024L * 1024L, internationalizationService.getAllTranslations("command.timedownloading.weight.gb"));
        weightNamesMultiplierMap.put(1024L * 1024L * 1024L * 1024L, internationalizationService.getAllTranslations("command.timedownloading.weight.tb"));
        weightNamesMultiplierMap.put(1024L * 1024L * 1024L * 1024L * 1024L, internationalizationService.getAllTranslations("command.timedownloading.weight.pb"));

        speedNamesMultiplierMap.put(1L, internationalizationService.getAllTranslations("command.timedownloading.speed.b"));
        speedNamesMultiplierMap.put(1024L, internationalizationService.getAllTranslations("command.timedownloading.speed.kb"));
        speedNamesMultiplierMap.put(1024L * 1024L, internationalizationService.getAllTranslations("command.timedownloading.speed.mb"));
        speedNamesMultiplierMap.put(1024L * 1024L * 1024L, internationalizationService.getAllTranslations("command.timedownloading.speed.gb"));
        speedNamesMultiplierMap.put(1024L * 1024L * 1024L * 1024L, internationalizationService.getAllTranslations("command.timedownloading.speed.tb"));
        speedNamesMultiplierMap.put(1024L * 1024L * 1024L * 1024L * 1024L, internationalizationService.getAllTranslations("command.timedownloading.speed.pb"));
    }

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        bot.sendTyping(message.getChatId());

        String commandArgument = commandWaitingService.getText(message);

        String responseText;
        if (commandArgument == null) {
            commandWaitingService.add(message, this.getClass());
            responseText = "${command.timedownloading.commandwaitingstart}";
        } else {
            commandArgument = commandArgument.replace(",", ".");
            int i = getNextSpaceIndex(commandArgument);

            double fileWeight;
            try {
                fileWeight = Double.parseDouble(commandArgument.substring(0, i));
            } catch (NumberFormatException e) {
                log.debug("Command parsing error: {} text: {}", e.getMessage(), commandArgument);
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            commandArgument = commandArgument.substring(i + 1);
            i = getNextSpaceIndex(commandArgument);

            Long weightMultiplier = getWeighMultiplierByName(commandArgument.substring(0, i));
            if (weightMultiplier == null) {
                log.debug("Unknown file weigh unit name: {}", commandArgument);
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            commandArgument = commandArgument.substring(i + 1);

            i = getNextSpaceIndex(commandArgument);

            double speedDownloading;
            try {
                speedDownloading = Double.parseDouble(commandArgument.substring(0, i));
            } catch (NumberFormatException e) {
                log.debug("Command parsing error: {} text: {}", e.getMessage(), commandArgument);
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            Long speedMultiplier = getSpeedMultiplierByName(commandArgument.substring(i + 1));
            if (speedMultiplier == null) {
                log.debug("Unknown file weigh unit name: {}", commandArgument);
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            double weighInBytes = fileWeight * weightMultiplier;
            double bytesInSecond = speedDownloading * speedMultiplier / 8;

            long milliseconds = (long) (weighInBytes / bytesInSecond) * 1000;

            if (milliseconds < 1000) {
                responseText = "${command.timedownloading.file} *" + TextUtils.formatFileSize(weighInBytes) + "* ${command.timedownloading.willdownload} *${command.timedownloading.instantly}*";
            } else {
                responseText = "${command.timedownloading.file} *" + TextUtils.formatFileSize(weighInBytes) + "* ${command.timedownloading.willdownload} ${command.timedownloading.in} *" + DateUtils.durationToString(milliseconds) + "*";
            }
        }

        return returnResponse(new TextResponse(message)
                .setText(responseText)
                .setResponseSettings(FormattingStyle.MARKDOWN));
    }

    private int getNextSpaceIndex(String text) {
        int i = text.indexOf(" ");
        if (i < 0) {
            log.debug("Unable to find space in text: {}", text);
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        return i;
    }

    private Long getWeighMultiplierByName(String name) {
        return getMultiplierFromMapByName(weightNamesMultiplierMap, name);
    }

    private Long getSpeedMultiplierByName(String name) {
        return getMultiplierFromMapByName(speedNamesMultiplierMap, name);
    }

    private Long getMultiplierFromMapByName(Map<Long, Set<String>> map, String name) {
        name = name.toLowerCase(Locale.ROOT);

        for (Map.Entry<Long, Set<String>> entry : map.entrySet()) {
            for (String data : entry.getValue()) {
                String[] monthNames = data.split(",");
                for (String monthName : monthNames) {
                    if (monthName.equals(name)) {
                        return entry.getKey();
                    }
                }
            }
        }

        return null;
    }

}
