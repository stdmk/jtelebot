package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.*;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;
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
@AllArgsConstructor
public class Tv implements CommandParent<SendMessage> {

    private final TvChannelService tvChannelService;
    private final TvProgramService tvProgramService;
    private final UserTvService userTvService;
    private final CommandPropertiesService commandPropertiesService;
    private final ChatService chatService;
    private final UserService userService;
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
            List<UserTv> userTvList = userTvService.get(chatService.get(message.getChatId()), userService.get(message.getFrom().getId()));
            if (userTvList.isEmpty()) {
                throw new BotException("Телеканалы по умолчанию не заданы.\nНажми /set");
            }

            responseText = buildResponseTextWithShortProgramsToChannels(userTvList.stream().map(UserTv::getTvChannel).collect(Collectors.toList()), zoneId, commandName);
        } else if (textMessage.startsWith("_ch")) {
            TvChannel tvChannel = tvChannelService.get(parseEntityId(textMessage));
            if (tvChannel == null) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            responseText = buildResponseTextWithProgramsToChannel(tvChannel, getUserZoneId(message), commandPropertiesService.getCommand(this.getClass()).getCommandName(), HOURS_NUMBER_LONG);
        } else if (textMessage.startsWith("_pr")) {
            TvProgram tvProgram = tvProgramService.get(parseEntityId(textMessage));
            if (tvProgram == null) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            responseText = buildResponseTextWithProgramDetails(tvProgram, getUserZoneId(message), commandPropertiesService.getCommand(this.getClass()).getCommandName());
        } else {
            List<TvChannel> tvChannelList = tvChannelService.get(textMessage);
            if (tvChannelList.size() == 1) {
                responseText = buildResponseTextWithProgramsToChannel(tvChannelList.get(0), zoneId, commandName, HOURS_NUMBER_LONG);
            } else {
                TvChannel tvChannel = tvChannelList.stream().filter(channel -> channel.getName().equalsIgnoreCase(textMessage)).findFirst().orElse(null);
                if (tvChannel != null) {
                    responseText = buildResponseTextWithProgramsToChannel(tvChannel, zoneId, commandName, HOURS_NUMBER_LONG);
                } else {
                    List<TvProgram> tvProgramList = tvProgramService.get(textMessage, ZonedDateTime.of(LocalDateTime.now(), zoneId).toLocalDateTime(), HOURS_NUMBER_DEFAULT);
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

    private String buildResponseTextWithSearchResults(List<TvChannel> tvChannelList, List<TvProgram> tvProgramList, String commandName, ZoneId zoneId) {
        StringBuilder buf = new StringBuilder();

        buf.append("<u>Найденные каналы:</u>\n");
        tvChannelList.forEach(tvChannel -> buf.append(tvChannel.getName()).append(" - /").append(commandName).append("_ch").append(tvChannel.getId()).append("\n"));

        if (!tvProgramList.isEmpty()) {
            buf.append("\n<u>Найденные программы</u>\n");

            tvProgramList.forEach(tvProgram -> buf
                    .append(tvProgram.getTitle()).append(" ").append(getProgramProgress(tvProgram.getStart(), tvProgram.getStop(), zoneId.getRules().getOffset(LocalDateTime.now()))).append("\n")
                    .append("(<b>").append(tvProgram.getChannel().getName()).append("</b>)\n")
                    .append(formatTvDateTime(tvProgram.getStart(), zoneId)).append("\n/").append(commandName)
                    .append("_pr").append(tvProgram.getId()).append("\n\n"));
        }

        return buf.toString();
    }

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

    private String buildResponseTextWithShortProgramsToChannels(List<TvChannel> tvChannelList, ZoneId zoneId, String commandName) {
        StringBuilder buf = new StringBuilder();

        tvChannelList.forEach(tvChannel -> buf.append(buildResponseTextWithProgramsToChannel(tvChannel, zoneId, commandName, HOURS_NUMBER_SHORT)).append("\n"));

        return buf.toString();
    }

    private String getProgramProgress(LocalDateTime dateTimeStart, LocalDateTime dateTimeEnd, ZoneOffset zoneOffset) {
        long programDuration = getDuration(dateTimeStart, dateTimeEnd, zoneOffset);
        return getProgramProgress(dateTimeStart, dateTimeEnd, programDuration, zoneOffset);
    }

    private String getProgramProgress(LocalDateTime dateTimeStart, LocalDateTime dateTimeEnd, Long programDuration, ZoneOffset zoneOffset) {
        LocalDateTime dateTimeNow = LocalDateTime.now();
        if (dateTimeNow.isAfter(dateTimeEnd) || dateTimeNow.isBefore(dateTimeStart)) {
            return "";
        }

        long timePassed = dateTimeNow.toInstant(zoneOffset).toEpochMilli() - dateTimeStart.toInstant(zoneOffset).toEpochMilli();
        float buf = (float) timePassed / (float) programDuration;

        return "(" + String.format("%.0f%%", buf * 100) + ")";
    }

    private Integer parseEntityId(String text) throws BotException {
        int id;
        try {
            id = Integer.parseInt(text.substring(3));
        } catch (NumberFormatException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        return id;
    }

    private ZoneId getUserZoneId(Message message) {
        ZoneId zoneId;
        UserCity userCity = userCityService.get(userService.get(message.getFrom().getId()), chatService.get(message.getChatId()));
        if (userCity == null) {
            zoneId = ZoneId.systemDefault();
        } else {
            zoneId = ZoneId.of(userCity.getCity().getTimeZone());
        }

        return zoneId;
    }
}
