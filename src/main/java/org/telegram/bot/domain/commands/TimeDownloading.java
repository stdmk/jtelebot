package org.telegram.bot.domain.commands;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.DateUtils;
import org.telegram.bot.utils.TextUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
@Slf4j
public class TimeDownloading implements CommandParent<SendMessage> {

    private final Bot bot;
    private final CommandWaitingService commandWaitingService;
    private final SpeechService speechService;

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
            responseText = "теперь напиши мне что нужно рассчитать";
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

            FileWeightUnit fileWeightUnit;
            try {
                fileWeightUnit = FileWeightUnit.getFileWeightUnitByUnitName(textMessage.substring(0, i));
            } catch (IllegalArgumentException e) {
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

            SpeedDownloadingUnit speedDownloadingUnit;
            try {
                speedDownloadingUnit = SpeedDownloadingUnit.getSpeedDownloadingUnitByUnitName(textMessage.substring(i + 1));
            } catch (IllegalArgumentException e) {
                log.debug("Unknown file weigh unit name: {}", textMessage);
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            double weighInBytes = fileWeight * fileWeightUnit.getMultiplier();
            double bytesInSecond = speedDownloading * speedDownloadingUnit.getMultiplier() / 8;

            long milliseconds = (long) (weighInBytes / bytesInSecond) * 1000;

            if (milliseconds < 1000) {
                responseText = "Файл *" + TextUtils.formatFileSize(weighInBytes) + "* скачается *мгновенно*";
            } else {
                responseText = "Файл *" + TextUtils.formatFileSize(weighInBytes) + "* скачается за *" + DateUtils.durationToString(milliseconds) + "*";
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

    @RequiredArgsConstructor
    @Getter
    private enum FileWeightUnit {
        BYTE("б", "b", 1L),
        KILOBYTE("кб", "kb", 1024L),
        MEGABYTE("мб", "mb", 1024L * 1024),
        GIGABYTE("гб", "gb", 1024L * 1024 * 1024),
        TERABYTE("тб", "tb", 1024L * 1024 * 1024 * 1024),
        PETABYTE("пб", "pb", 1024L * 1024 * 1024 * 1024 * 1024),
        ;

        private final String unitRu;
        private final String unitEn;
        private final long multiplier;

        public static FileWeightUnit getFileWeightUnitByUnitName(String unitName) {
            return Arrays.stream(FileWeightUnit.values())
                    .filter(unit -> (unit.getUnitRu().equalsIgnoreCase(unitName) || unit.getUnitEn().equalsIgnoreCase(unitName)))
                    .findFirst()
                    .orElseThrow(IllegalArgumentException::new);
        }
    }

    @RequiredArgsConstructor
    @Getter
    private enum SpeedDownloadingUnit {
        BIT("бит", "b", 1L),
        KILOBIT("кбит", "kb", 1024L),
        MEGABIT("мбит", "mb", 1024L * 1024),
        GIGABIT("гбит", "gb", 1024L * 1024 * 1024),
        TERABIT("тбит", "tb", 1024L * 1024 * 1024 * 1024),
        PETABIT("пбит", "pb", 1024L * 1024 * 1024 * 1024 * 1024),
        ;

        private final String unitRu;
        private final String unitEn;
        private final long multiplier;

        public static SpeedDownloadingUnit getSpeedDownloadingUnitByUnitName(String unitName) {
            return Arrays.stream(SpeedDownloadingUnit.values())
                    .filter(unit -> (unit.getUnitRu().equalsIgnoreCase(unitName) || unit.getUnitEn().equalsIgnoreCase(unitName)))
                    .findFirst()
                    .orElseThrow(IllegalArgumentException::new);
        }
    }
}
