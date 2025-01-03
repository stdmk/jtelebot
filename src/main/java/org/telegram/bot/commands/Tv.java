package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.entities.*;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.ResponseSettings;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;

import java.time.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.telegram.bot.utils.DateUtils.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class Tv implements Command {

    private static final String COMMAND_NAME = "tv";
    private static final int HOURS_NUMBER_SHORT = 3;
    private static final int HOURS_NUMBER_DEFAULT = 6;
    private static final int HOURS_NUMBER_LONG = 12;

    private final Bot bot;
    private final TvChannelService tvChannelService;
    private final TvProgramService tvProgramService;
    private final UserTvService userTvService;
    private final UserCityService userCityService;
    private final SpeechService speechService;
    private final Clock clock;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        bot.sendTyping(message.getChatId());
        String commandArgument = message.getCommandArgument();
        List<String> responseTextList;

        ZoneId zoneId = userCityService.getZoneIdOfUserOrDefault(message);

        if (commandArgument == null) {
            responseTextList = buildResponseTextWithShortProgramsToChannels(message, zoneId);
        } else if (commandArgument.startsWith("_ch")) {
            TvChannel tvChannel = getTvChannel(commandArgument);
            responseTextList = buildResponseTextWithProgramsToChannel(tvChannel, zoneId, HOURS_NUMBER_LONG);
        } else if (commandArgument.startsWith("_pr")) {
            TvProgram tvProgram = getTvProgram(commandArgument);
            responseTextList = List.of(buildResponseTextWithProgramDetails(tvProgram, zoneId));
        } else {
            responseTextList = searchForChannelsAndPrograms(commandArgument, zoneId);
        }

        return mapToTextResponseList(responseTextList, message, new ResponseSettings().setFormattingStyle(FormattingStyle.HTML));
    }

    private List<String> searchForChannelsAndPrograms(String searchText, ZoneId zoneId) {
        log.debug("Request to search in tv-channels and tv-programs by text {}", searchText);
        List<TvChannel> tvChannelList = tvChannelService.get(searchText);
        if (tvChannelList.size() == 1) {
            TvChannel tvChannel = tvChannelList.get(0);
            return buildResponseTextWithProgramsToChannel(tvChannel, zoneId, HOURS_NUMBER_LONG);
        } else {
            TvChannel tvChannel = tvChannelList
                    .stream()
                    .filter(channel -> channel.getName().equalsIgnoreCase(searchText))
                    .findFirst()
                    .orElse(null);
            if (tvChannel != null) {
                return buildResponseTextWithProgramsToChannel(tvChannel, zoneId, HOURS_NUMBER_LONG);
            } else {
                List<TvProgram> tvProgramList = tvProgramService.get(
                        searchText,
                        ZonedDateTime.of(LocalDateTime.now(clock), zoneId).toLocalDateTime(),
                        HOURS_NUMBER_DEFAULT);
                if (tvChannelList.isEmpty() && tvProgramList.isEmpty()) {
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING));
                }
                return buildResponseTextWithSearchResults(tvChannelList, tvProgramList, zoneId);
            }
        }
    }

    /**
     * Building response text for search results.
     *
     * @param tvChannelList list of TvChannel entities.
     * @param tvProgramList list of TvProgram entities.
     * @param zoneId        time-zone ID.
     * @return formatted string with search results.
     */
    private List<String> buildResponseTextWithSearchResults(List<TvChannel> tvChannelList, List<TvProgram> tvProgramList, ZoneId zoneId) {
        List<String> result = new ArrayList<>();
        result.add("<u>${command.tv.foundchannels}:</u>\n");

        result.addAll(tvChannelList
                .stream()
                .map(tvChannel -> tvChannel.getName() + " - /" + Tv.COMMAND_NAME + "_ch" + tvChannel.getId() + "\n")
                .toList());

        if (!tvProgramList.isEmpty()) {
            result.add("\n<u>${command.tv.foundprograms}:</u>\n");

            result.addAll(tvProgramList
                    .stream()
                    .map(tvProgram -> tvProgram.getTitle() + " "
                            + getProgramProgress(tvProgram.getStart(), tvProgram.getStop(), zoneId.getRules().getOffset(LocalDateTime.now(clock)))
                            + "\n(<b>" + tvProgram.getChannel().getName() + "</b>)\n"
                            + formatTvDateTime(tvProgram.getStart(), zoneId) + "\n/" + Tv.COMMAND_NAME
                            + "_pr" + tvProgram.getId() + "\n\n")
                    .toList());
        }

        return result;
    }

    /**
     * Building response text with tv-program details.
     *
     * @param tvProgram TvProgram entity.
     * @param zoneId time-zone ID.
     * @return formatted string with tv-program details.
     */
    private String buildResponseTextWithProgramDetails(TvProgram tvProgram, ZoneId zoneId) {
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

        ZoneOffset zoneOffSet = zoneId.getRules().getOffset(LocalDateTime.now(clock));
        Duration programDuration = getDuration(tvProgram.getStart(), tvProgram.getStop(), zoneId);

        return "<u>" + tvProgram.getChannel().getName() + "</u> /" + COMMAND_NAME + "_ch" + tvProgram.getChannel().getId() + "\n" +
                "<b>" + tvProgram.getTitle() + "</b> " +
                getProgramProgress(tvProgram.getStart(), tvProgram.getStop(), programDuration.toMillis(), zoneOffSet) + "\n" + category +
                "${command.tv.start}: " + formatTvTime(tvProgram.getStart(), zoneId) + "\n" +
                "${command.tv.stop}: " + formatTvTime(tvProgram.getStop(), zoneId) + "\n" +
                "(" + durationToString(programDuration) + ")\n" +
                desc;
    }

    /**
     * Building response text with tv-programs for channel.
     *
     * @param tvChannel TvChannel entity.
     * @param zoneId time-zone ID.
     * @param hours count of hours tv-program.
     * @return formatted string with list of tv-program.
     */
    private List<String> buildResponseTextWithProgramsToChannel(TvChannel tvChannel, ZoneId zoneId, int hours) {
        List<TvProgram> tvProgramList = tvProgramService.get(tvChannel, ZonedDateTime.of(LocalDateTime.now(clock), zoneId).toLocalDateTime(), hours);
        if (tvProgramList.isEmpty()) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING));
        }
        
        List<String> result = new ArrayList<>();
        result.add("<u>" + tvChannel.getName() + "</u>" + " /" + COMMAND_NAME + "_ch" + tvChannel.getId() + "\n");

        ZoneOffset zoneOffSet = zoneId.getRules().getOffset(LocalDateTime.now(clock));
        TvProgram currentTvProgram = tvProgramList.get(0);

        result.add("<b>[" + formatTvTime(currentTvProgram.getStart(), zoneId) + "]</b> "
                + currentTvProgram.getTitle()
                + " " + getProgramProgress(currentTvProgram.getStart(), currentTvProgram.getStop(), zoneOffSet) + "\n"
                + "/" + COMMAND_NAME + "_pr" + currentTvProgram.getId() + "\n");

        result.addAll(tvProgramList
                .stream()
                .skip(1)
                .map(tvProgram -> "<b>[" + formatTvTime(tvProgram.getStart(), zoneId) + "]</b> " + tvProgram.getTitle() + "\n/"
                        + COMMAND_NAME + "_pr" + tvProgram.getId() + "\n")
                .toList());

        result.add("\n");

        return result;
    }

    private List<String> buildResponseTextWithShortProgramsToChannels(Message message, ZoneId zoneId) {
        Chat chat = message.getChat();
        User user = message.getUser();
        log.debug("Request to get tv-program for user {} and chat {}", user, chat);

        List<UserTv> userTvList = userTvService.get(chat, user);
        if (userTvList.isEmpty()) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.SETTING_REQUIRED));
        }
        
        return userTvList
                .stream()
                .map(UserTv::getTvChannel)
                .map(tvChannel ->
                        buildResponseTextWithProgramsToChannel(tvChannel, zoneId, HOURS_NUMBER_SHORT))
                .flatMap(Collection::stream)
                .toList();
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
        Duration programDuration = getDuration(dateTimeStart, dateTimeEnd, zoneOffset);
        return getProgramProgress(dateTimeStart, dateTimeEnd, programDuration.toMillis(), zoneOffset);
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
        LocalDateTime dateTimeNow = LocalDateTime.now(clock);
        if (dateTimeNow.isAfter(dateTimeEnd) || dateTimeNow.isBefore(dateTimeStart)) {
            return "";
        }

        long timePassed = dateTimeNow.toInstant(zoneOffset).toEpochMilli() - dateTimeStart.toInstant(zoneOffset).toEpochMilli();
        float buf = (float) timePassed / (float) programDuration;

        return "(" + String.format("%.0f%%", buf * 100) + ")";
    }

    private TvChannel getTvChannel(String text) {
        int tvChannelId = parseEntityId(text);

        log.debug("Request to get tv-program by channel with id {}", tvChannelId);
        TvChannel tvChannel = tvChannelService.get(tvChannelId);
        if (tvChannel == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        return tvChannel;
    }

    private TvProgram getTvProgram(String text) {
        int tvProgramId = parseEntityId(text);

        log.debug("Request to get details for tv-program with id {}", tvProgramId);
        TvProgram tvProgram = tvProgramService.get(tvProgramId);
        if (tvProgram == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        return tvProgram;
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

}
