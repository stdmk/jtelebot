package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.entities.City;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserCity;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CityService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserCityService;
import org.telegram.bot.services.UserService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.telegram.bot.utils.DateUtils.formatDateTime;
import static org.telegram.bot.utils.DateUtils.formatTime;
import static org.telegram.bot.utils.TextUtils.getMarkdownLinkToUser;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserTime implements Command {

    private final Bot bot;
    private final UserService userService;
    private final UserCityService userCityService;
    private final CityService cityService;
    private final SpeechService speechService;
    private final Clock clock;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        bot.sendTyping(message.getChatId());
        String commandArgument = message.getCommandArgument();
        String responseText;

        User user;
        LocalDateTime repliedMessageDateTime = null;
        if (commandArgument == null) {
            Message repliedMessage = message.getReplyToMessage();
            if (repliedMessage != null) {
                repliedMessageDateTime = repliedMessage.getDateTime();
                user = userService.get(repliedMessage.getUser().getUserId());
            } else {
                user = userService.get(message.getUser().getUserId());
            }
        } else {
            user = userService.get(commandArgument);
        }

        UserCity userCity = userCityService.get(user, message.getChat());
        if (userCity == null) {
            City city = cityService.get(commandArgument);
            if (city == null) {
                if (user == null) {
                    log.debug("Unable to find user or city {}", commandArgument);
                    throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                }
                responseText = getMarkdownLinkToUser(user) + " ${command.usertime.citynotset}";
            } else {
                log.debug("Request to get time of city {}", city.getNameEn());
                String dateTimeNow = formatTime(ZonedDateTime.now(clock).withZoneSameInstant(ZoneId.of(city.getTimeZone())));
                responseText = "${command.usertime.incity} " + city.getNameRu() + " ${command.usertime.now}: *" + dateTimeNow + "*";
            }
        } else {
            log.debug("Request to get time of user {}", user);

            ZoneId userZoneId = ZoneId.of(userCity.getCity().getTimeZone());
            String dateTimeNow = formatTime(ZonedDateTime.now(clock).withZoneSameInstant(userZoneId));
            responseText = "${command.usertime.at} " + getMarkdownLinkToUser(user) + " ${command.usertime.now} *" + dateTimeNow + "*";
            if (repliedMessageDateTime != null) {
                String pastDateTime = formatDateTime(repliedMessageDateTime.atZone(ZoneId.systemDefault()).withZoneSameInstant(userZoneId));
                responseText = responseText + "\n" + "${command.usertime.was}: *" + pastDateTime + "*";
            }
        }

        return returnResponse(new TextResponse(message)
                .setText(responseText)
                .setResponseSettings(FormattingStyle.MARKDOWN));
    }
}