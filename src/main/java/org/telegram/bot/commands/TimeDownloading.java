package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.Command;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.InternalizationService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.DateUtils;
import org.telegram.bot.utils.TextUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class TimeDownloading implements Command<SendMessage> {

    private final Bot bot;
    private final CommandWaitingService commandWaitingService;
    private final SpeechService speechService;
    private final InternalizationService internalizationService;
    private final Map<Long, Set<String>> weightNamesMultiplierMap = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> speedNamesMultiplierMap = new ConcurrentHashMap<>();

    @PostConstruct
    private void postConstruct() {
        weightNamesMultiplierMap.put(1L, internalizationService.getAllTranslations("command.timedownloading.weight.b"));
        weightNamesMultiplierMap.put(1024L, internalizationService.getAllTranslations("command.timedownloading.weight.kb"));
        weightNamesMultiplierMap.put(1024L * 1024L, internalizationService.getAllTranslations("command.timedownloading.weight.mb"));
        weightNamesMultiplierMap.put(1024L * 1024L * 1024L, internalizationService.getAllTranslations("command.timedownloading.weight.gb"));
        weightNamesMultiplierMap.put(1024L * 1024L * 1024L * 1024L, internalizationService.getAllTranslations("command.timedownloading.weight.tb"));
        weightNamesMultiplierMap.put(1024L * 1024L * 1024L * 1024L * 1024L, internalizationService.getAllTranslations("command.timedownloading.weight.pb"));

        speedNamesMultiplierMap.put(1L, internalizationService.getAllTranslations("command.timedownloading.speed.b"));
        speedNamesMultiplierMap.put(1024L, internalizationService.getAllTranslations("command.timedownloading.speed.kb"));
        speedNamesMultiplierMap.put(1024L * 1024L, internalizationService.getAllTranslations("command.timedownloading.speed.mb"));
        speedNamesMultiplierMap.put(1024L * 1024L * 1024L, internalizationService.getAllTranslations("command.timedownloading.speed.gb"));
        speedNamesMultiplierMap.put(1024L * 1024L * 1024L * 1024L, internalizationService.getAllTranslations("command.timedownloading.speed.tb"));
        speedNamesMultiplierMap.put(1024L * 1024L * 1024L * 1024L * 1024L, internalizationService.getAllTranslations("command.timedownloading.speed.pb"));
    }

    @Override
    public SendMessage parse(Update update) {
        Message message = getMessageFromUpdate(update);
        bot.sendTyping(message.getChatId());
        String textMessage = commandWaitingService.getText(message);

        if (textMessage == null) {
            textMessage = cutCommandInText(message.getText());
        }

        String responseText;
        if (textMessage == null) {
            commandWaitingService.add(message, this.getClass());
            responseText = "${command.timedownloading.commandwaitingstart}";
        } else {
            textMessage = textMessage.replace(",", ".");
            int i = getNextSpaceIndex(textMessage);

            double fileWeight;
            try {
                fileWeight = Double.parseDouble(textMessage.substring(0, i));
            } catch (NumberFormatException e) {
                log.debug("Command parsing error: {} text: {}", e.getMessage(), textMessage);
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            textMessage = textMessage.substring(i + 1);
            i = getNextSpaceIndex(textMessage);

            Long weightMultiplier = getWeighMultiplierByName(textMessage.substring(0, i));
            if (weightMultiplier == null) {
                log.debug("Unknown file weigh unit name: {}", textMessage);
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            textMessage = textMessage.substring(i + 1);

            i = getNextSpaceIndex(textMessage);

            double speedDownloading;
            try {
                speedDownloading = Double.parseDouble(textMessage.substring(0, i));
            } catch (NumberFormatException e) {
                log.debug("Command parsing error: {} text: {}", e.getMessage(), textMessage);
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            Long speedMultiplier = getSpeedMultiplierByName(textMessage.substring(i + 1));
            if (speedMultiplier == null) {
                log.debug("Unknown file weigh unit name: {}", textMessage);
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

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.enableMarkdown(true);
        sendMessage.setText(responseText);

        return sendMessage;
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
        name = name.toLowerCase();

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
