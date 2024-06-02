package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserService;

import java.util.List;

import static org.telegram.bot.utils.TextUtils.getLinkToUser;

@Component
@RequiredArgsConstructor
@Slf4j
public class Getid implements Command {

    private final Bot bot;
    private final UserService userService;
    private final SpeechService speechService;

    @Override
    public List<BotResponse> parse(BotRequest request) throws BotException {
        Message message = request.getMessage();
        bot.sendTyping(message.getChatId());
        String commandArgument = message.getCommandArgument();
        StringBuilder responseText = new StringBuilder();
        Long chatId = message.getChatId();

        if (commandArgument != null) {
            log.debug("Request to getting telegram id of {}", commandArgument);
            User user = userService.get(commandArgument);
            if (user == null) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }
            responseText.append("${command.getid.id} ").append(getLinkToUser(user, false)).append(": `").append(user.getUserId()).append("`\n");
        }

        Message repliedMessage = message.getReplyToMessage();
        if (repliedMessage != null) {
            User repliedUser = repliedMessage.getUser();
            log.debug("Request to getting telegram id of {}", repliedUser);
            responseText.append("${command.getid.id} ").append(getLinkToUser(repliedUser, false)).append(": `").append(repliedUser.getUserId()).append("`\n");
        }

        if (chatId < 0) {
            log.debug("Request to getting telegram id of public chat {}", message.getChat());
            responseText.append("${command.getid.groupid}: `").append(chatId).append("`\n");
        }

        responseText.append("${command.getid.yourid}: `").append(message.getUser().getUserId()).append("`");

        return returnResponse(new TextResponse(message)
                .setText(responseText.toString())
                .setResponseSettings(FormattingStyle.MARKDOWN));
    }
}
