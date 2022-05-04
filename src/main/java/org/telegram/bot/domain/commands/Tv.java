package org.telegram.bot.domain.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.TvChannel;
import org.telegram.bot.domain.entities.TvProgram;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserCity;
import org.telegram.bot.domain.entities.UserTv;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandPropertiesService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.TvChannelService;
import org.telegram.bot.services.TvProgramService;
import org.telegram.bot.services.UserCityService;
import org.telegram.bot.services.UserTvService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.DateUtils.formatTvDateTime;
import static org.telegram.bot.utils.DateUtils.formatTvTime;
import static org.telegram.bot.utils.DateUtils.deltaDatesToString;
import static org.telegram.bot.utils.DateUtils.getDuration;
import static org.telegram.bot.utils.TextUtils.isTextLengthIncludedInLimit;

@Component
@RequiredArgsConstructor
@Slf4j
public class Tv implements CommandParent<SendMessage> {

    private final TvChannelService tvChannelService;
    private final TvProgramService tvProgramService;
    private final UserTvService userTvService;
    private final CommandPropertiesService commandPropertiesService;
    private final UserCityService userCityService;
    private final SpeechService speechService;

    private final int HOURS_NUMBER_SHORT = 3;
    private final int HOURS_NUMBER_DEFAULT = 6;
    private final int HOURS_NUMBER_LONG = 12;

    @Override
    public SendMessage parse(Update update) {
        Message message = getMessageFromUpdate(update);
        String textMessage = cutCommandInText(message.getText());
        String responseText;

        ZoneId zoneId = getUserZoneId(message);
        String commandName = commandPropertiesService.getCommand(this.getClass()).getCommandName();

        if (textMessage == null) {
            Chat chat = new Chat().setChatId(message.getChatId());
            User user = new User().setUserId(message.getFrom().getId());
            log.debug("Request to get tv-program for user {} and chat {}", user, chat);

            List<UserTv> userTvList = userTvService.get(chat, user);
            if (userTvList.isEmpty()) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.SETTING_REQUIRED));
            }

            responseText = buildResponseTextWithShortProgramsToChannels(
                    userTvList.stream().map(UserTv::getTvChannel).collect(Collectors.toList()),
                    zoneId,
                    commandName);
        } else if (textMessage.startsWith("_ch")) {
            int tvChannelId = parseEntityId(textMessage);

            log.debug("Request to get tv-program by channel with id {}", tvChannelId);
            TvChannel tvChannel = tvChannelService.get(tvChannelId);
            if (tvChannel == null) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            responseText = buildResponseTextWithProgramsToChannel(
                    tvChannel,
                    getUserZoneId(message),
                    commandPropertiesService.getCommand(this.getClass()).getCommandName(),
                    HOURS_NUMBER_LONG);
        } else if (textMessage.startsWith("_pr")) {
            int tvProgramId = parseEntityId(textMessage);

            log.debug("Request to get details for tv-program with id {}", tvProgramId);
            TvProgram tvProgram = tvProgramService.get(tvProgramId);
            if (tvProgram == null) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            responseText = buildResponseTextWithProgramDetails(
                    tvProgram,
                    getUserZoneId(message),
                    commandPropertiesService.getCommand(this.getClass()).getCommandName());
        } else {
            log.debug("Request to search in tv-channels and tv-programs by text {}", textMessage);
            List<TvChannel> tvChannelList = tvChannelService.get(textMessage);
            if (tvChannelList.size() == 1) {
                TvChannel tvChannel = tvChannelList.get(0);
                responseText = buildResponseTextWithProgramsToChannel(tvChannel, zoneId, commandName, HOURS_NUMBER_LONG);
            } else {
                TvChannel tvChannel = tvChannelList
                        .stream()
                        .filter(channel -> channel.getName().equalsIgnoreCase(textMessage))
                        .findFirst()
                        .orElse(null);
                if (tvChannel != null) {
                    responseText = buildResponseTextWithProgramsToChannel(tvChannel, zoneId, commandName, HOURS_NUMBER_LONG);
                } else {
                    List<TvProgram> tvProgramList = tvProgramService.get(
                            textMessage,
                            ZonedDateTime.of(LocalDateTime.now(), zoneId).toLocalDateTime(),
                            HOURS_NUMBER_DEFAULT);
                    if (tvChannelList.isEmpty() && tvProgramList.isEmpty()) {
                        throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING));
                    }
                    responseText = buildResponseTextWithSearchResults(tvChannelList, tvProgramList, commandName, zoneId);
                }
            }
        }

        if (!isTextLengthIncludedInLimit(responseText)) {
            responseText = "Результат поиска оказался слишком большим. Попробуй сузить";
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.enableHtml(true);
        sendMessage.setText(responseText);

        return sendMessage;
    }

    /**
     * Building response text for search results.
     *
     * @param tvChannelList list of TvChannel entities.
     * @param tvProgramList list of TvProgram entities.
     * @param commandName name of tv command.
     * @param zoneId time-zone ID.
     * @return formatted string with search results.
     */
    private String buildResponseTextWithSearchResults(List<TvChannel> tvChannelList, List<TvProgram> tvProgramList, String commandName, ZoneId zoneId) {
        StringBuilder buf = new StringBuilder();

        buf.append("<u>Найденные каналы:</u>\n");
        tvChannelList
                .forEach(tvChannel -> buf.append(tvChannel.getName()).append(" - /").append(commandName).append("_ch").append(tvChannel.getId()).append("\n"));

        if (!tvProgramList.isEmpty()) {
            buf.append("\n<u>Найденные программы</u>\n");

            tvProgramList.forEach(tvProgram -> buf
                    .append(tvProgram.getTitle())
                    .append(" ").append(getProgramProgress(tvProgram.getStart(), tvProgram.getStop(), zoneId.getRules().getOffset(LocalDateTime.now())))
                    .append("\n")
                    .append("(<b>").append(tvProgram.getChannel().getName()).append("</b>)\n")
                    .append(formatTvDateTime(tvProgram.getStart(), zoneId)).append("\n/").append(commandName)
                    .append("_pr").append(tvProgram.getId()).append("\n\n"));
        }

        return buf.toString();
    }

    /**
     * Building response text with tv-program details.
     *
     * @param tvProgram TvProgram entity.
     * @param zoneId time-zone ID.
     * @param commandName name of tv command.
     * @return formatted string with tv-program details.
     */
    private String buildResponseTextWithProgramDetails(TvProgram tvProgram, ZoneId zoneId, String commandName) {
        String category = tvProgram.getCategory();
        String desc = tvProgram.getDesc();

        if (category == null) {
            category = "";
        } else {
            category = "<i>" + category + "</i>\n";
        }

        if (desc == null) {
            desc = "";
        } else {
            desc = "\n<i>" + desc + "</i>";
        }

        ZoneOffset zoneOffSet = zoneId.getRules().getOffset(LocalDateTime.now());
        long programDuration = getDuration(tvProgram.getStart(), tvProgram.getStop(), zoneId);

        return "<u>" + tvProgram.getChannel().getName() + "</u> /" + commandName + "_ch" + tvProgram.getChannel().getId() + "\n" +
                "<b>" + tvProgram.getTitle() + "</b> " +
                getProgramProgress(tvProgram.getStart(), tvProgram.getStop(), programDuration, zoneOffSet) + "\n" + category +
                "Начало: " + formatTvTime(tvProgram.getStart(), zoneId) + "\n" +
                "Конец: " + formatTvTime(tvProgram.getStop(), zoneId) + "\n" +
                "(" + deltaDatesToString(programDuration) + ")\n" +
                desc;
    }

    /**
     * Building response text with tv-programs for channel.
     *
     * @param tvChannel TvChannel entity.
     * @param zoneId time-zone ID.
     * @param commandName name of tv command.
     * @param hours count of hours tv-program.
     * @return formatted string with list of tv-program.
     */
    private String buildResponseTextWithProgramsToChannel(TvChannel tvChannel, ZoneId zoneId, String commandName, int hours) {
        StringBuilder buf = new StringBuilder();

        buf.append("<u>").append(tvChannel.getName()).append("</u>").append(" /").append(commandName).append("_ch").append(tvChannel.getId()).append("\n");

        List<TvProgram> tvProgramList = tvProgramService.get(tvChannel, ZonedDateTime.of(LocalDateTime.now(), zoneId).toLocalDateTime(), hours);

        ZoneOffset zoneOffSet = zoneId.getRules().getOffset(LocalDateTime.now());
        TvProgram currentTvProgram = tvProgramList.get(0);

        buf.append("<b>[").append(formatTvTime(currentTvProgram.getStart(), zoneId)).append("]</b> ")
                .append(currentTvProgram.getTitle())
                .append(" ").append(getProgramProgress(currentTvProgram.getStart(), currentTvProgram.getStop(), zoneOffSet)).append("\n")
                .append("/").append(commandName).append("_pr").append(currentTvProgram.getId()).append("\n");

        tvProgramList.stream().skip(1).forEach(tvProgram -> buf
                .append("<b>[").append(formatTvTime(tvProgram.getStart(), zoneId)).append("]</b> ")
                .append(tvProgram.getTitle()).append("\n/").append(commandName).append("_pr").append(tvProgram.getId()).append("\n")
        );

        return buf.toString();
    }

    /**
     * Building short response text with tv-programs for channels.
     *
     * @param tvChannelList list of TvChannel entity.
     * @param zoneId time-zone ID.
     * @param commandName name of tv command.
     * @return formatted strings with short list of tv-program.
     */
    private String buildResponseTextWithShortProgramsToChannels(List<TvChannel> tvChannelList, ZoneId zoneId, String commandName) {
        StringBuilder buf = new StringBuilder();
        tvChannelList.forEach(tvChannel -> buf.append(buildResponseTextWithProgramsToChannel(tvChannel, zoneId, commandName, HOURS_NUMBER_SHORT)).append("\n"));
        return buf.toString();
    }

    /**
     * Getting progress of tv-program.
     *
     * @param dateTimeStart datetime of start of tv-program.
     * @param dateTimeEnd datetime of end of tv-program.
     * @param zoneOffset time-zone offset from Greenwich/UTC.
     * @return tv-program progress.
     */
    private String getProgramProgress(LocalDateTime dateTimeStart, LocalDateTime dateTimeEnd, ZoneOffset zoneOffset) {
        long programDuration = getDuration(dateTimeStart, dateTimeEnd, zoneOffset);
        return getProgramProgress(dateTimeStart, dateTimeEnd, programDuration, zoneOffset);
    }

    /**
     * Getting progress of tv-program.
     *
     * @param dateTimeStart datetime of start of tv-program.
     * @param dateTimeEnd datetime of end of tv-program.
     * @param programDuration duration of program.
     * @param zoneOffset time-zone offset from Greenwich/UTC.
     * @return tv-program progress.
     */
    private String getProgramProgress(LocalDateTime dateTimeStart, LocalDateTime dateTimeEnd, Long programDuration, ZoneOffset zoneOffset) {
        LocalDateTime dateTimeNow = LocalDateTime.now();
        if (dateTimeNow.isAfter(dateTimeEnd) || dateTimeNow.isBefore(dateTimeStart)) {
            return "";
        }

        long timePassed = dateTimeNow.toInstant(zoneOffset).toEpochMilli() - dateTimeStart.toInstant(zoneOffset).toEpochMilli();
        float buf = (float) timePassed / (float) programDuration;

        return "(" + String.format("%.0f%%", buf * 100) + ")";
    }


    /**
     * Trying to get entity id from text.
     *
     * @param text text to parsing.
     * @return value of id.
     */
    private Integer parseEntityId(String text) {
        try {
            return Integer.parseInt(text.substring(3));
        } catch (NumberFormatException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }


    /**
     * Getting Zone id for User.
     *
     * @param message telegram message.
     * @return Zone id of User.
     */
    private ZoneId getUserZoneId(Message message) {
        ZoneId zoneId;
        UserCity userCity = userCityService.get(new User().setUserId(message.getFrom().getId()), new Chat().setChatId(message.getChatId()));
        if (userCity == null) {
            zoneId = ZoneId.systemDefault();
        } else {
            zoneId = ZoneId.of(userCity.getCity().getTimeZone());
        }

        return zoneId;
    }
}
