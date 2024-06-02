package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserCity;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.services.UserCityService;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.telegram.bot.utils.DateUtils.durationToString;

@Component
@RequiredArgsConstructor
public class NewYear implements Command {

    private static final String DEFAULT_TIME_ZONE = "GMT+03:00";

    private final Bot bot;
    private final UserCityService userCityService;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        if (message.hasCommandArgument()) {
            return returnResponse();
        }
        bot.sendTyping(message.getChatId());

        Chat chat = message.getChat();
        User user = message.getUser();
        ZoneId userTimeZone;

        UserCity userCity = userCityService.get(user, chat);
        if (userCity == null) {
            userTimeZone = ZoneId.of(DEFAULT_TIME_ZONE);
        } else {
            userTimeZone = ZoneId.of(userCity.getCity().getTimeZone());
        }

        ZonedDateTime dateTimeNow = ZonedDateTime.now(userTimeZone);
        String duration = durationToString(dateTimeNow.toLocalDateTime(), dateTimeNow.plusYears(1).withDayOfYear(1).toLocalDate().atStartOfDay());

        return returnResponse(new TextResponse(message)
                .setText("${command.newyear.caption}: *" + duration + "* (" + userTimeZone + ")")
                .setResponseSettings(FormattingStyle.MARKDOWN));
    }
}
