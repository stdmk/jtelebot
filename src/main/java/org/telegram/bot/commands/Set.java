package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.commands.setters.Setter;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandWaiting;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.AccessLevel;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserService;
import org.telegram.bot.utils.TextUtils;

import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class Set implements Command {

    private static final ResponseSettings DEFAULT_RESPONSE_SETTINGS = new ResponseSettings()
            .setFormattingStyle(FormattingStyle.HTML)
            .setWebPagePreview(false);
    private static final String EMPTY_COMMAND = "set ";

    private final Bot bot;
    private final CommandWaitingService commandWaitingService;
    private final UserService userService;
    private final SpeechService speechService;

    private final List<Setter<?>> setters;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        bot.sendTyping(message.getChatId());
        Chat chat = message.getChat();
        User user = message.getUser();
        String textMessage = message.getText();

        String commandArgument;
        CommandWaiting commandWaiting = commandWaitingService.get(chat, user);
        if (!message.isCallback() && commandWaiting != null) {
            commandArgument = TextUtils.cutCommandInText(commandWaiting.getTextMessage() + textMessage);
        } else {
            commandArgument = message.getCommandArgument();
        }

        if (commandArgument == null || commandArgument.toLowerCase(Locale.ROOT).startsWith("back")) {
            if (message.isCallback()) {
                return returnResponse(new EditResponse(message)
                        .setText("<b>${setter.set.caption}</b>")
                        .setKeyboard(buildMainKeyboard())
                        .setResponseSettings(DEFAULT_RESPONSE_SETTINGS));
            } else {
                return returnResponse(new TextResponse(message)
                        .setText("<b>${setter.set.caption}</b>")
                        .setKeyboard(buildMainKeyboard())
                        .setResponseSettings(DEFAULT_RESPONSE_SETTINGS));
            }
        } else {
            String lowerCasedTextMessage = commandArgument.toLowerCase(Locale.ROOT);
            Setter<?> setter = setters
                    .stream()
                    .filter(setter1 -> setter1.canProcessed(lowerCasedTextMessage))
                    .findFirst()
                    .orElseThrow(() -> {
                        commandWaitingService.remove(commandWaiting);
                        return new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                    });

            AccessLevel userAccessLevel = userService.getCurrentAccessLevel(user.getUserId(), chat.getChatId());
            if (userService.isUserHaveAccessForCommand(userAccessLevel, setter.getAccessLevel())) {
                return returnResponse(setter.set(request, commandArgument));
            } else {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_ACCESS));
            }
        }
    }

    private Keyboard buildMainKeyboard() {
        return new Keyboard()
                .addRow(List.of(
                        new KeyboardButton()
                                .setName("${setter.set.news}")
                                .setCallback(EMPTY_COMMAND + "${setter.set.news}"),
                        new KeyboardButton()
                                .setName("${setter.set.city}")
                                .setCallback(EMPTY_COMMAND + "${setter.set.city}"),
                        new KeyboardButton()
                                .setName("${setter.set.alias}")
                                .setCallback(EMPTY_COMMAND + "${setter.set.alias}")
                ))
                .addRow(List.of(
                        new KeyboardButton()
                                .setName("${setter.set.tv}")
                                .setCallback(EMPTY_COMMAND + "${setter.set.tv}"),
                        new KeyboardButton()
                                .setName("${setter.set.holiday}")
                                .setCallback(EMPTY_COMMAND + "${setter.set.holiday}"),
                        new KeyboardButton()
                                .setName("${setter.set.command}")
                                .setCallback(EMPTY_COMMAND + "${setter.set.command}")
                ))
                .addRow(List.of(
                        new KeyboardButton()
                                .setName("${setter.set.talker}")
                                .setCallback(EMPTY_COMMAND + "${setter.set.talker}"),
                        new KeyboardButton()
                                .setName("${setter.set.zodiac}")
                                .setCallback(EMPTY_COMMAND + "${setter.set.zodiac}"),
                        new KeyboardButton()
                                .setName("${setter.set.trainings}")
                                .setCallback(EMPTY_COMMAND + "${setter.set.trainings}")
                ))
                .addRow(List.of(
                        new KeyboardButton()
                                .setName("${setter.set.chatgpt}")
                                .setCallback(EMPTY_COMMAND + "${setter.set.chatgpt}"),
                        new KeyboardButton()
                                .setName("${setter.set.gigachat}")
                                .setCallback(EMPTY_COMMAND + "${setter.set.gigachat}"),
                        new KeyboardButton()
                                .setName("${setter.set.lang}")
                                .setCallback(EMPTY_COMMAND + "${setter.set.lang}")
                ))
                .addRow(List.of(
                        new KeyboardButton()
                                .setName("${setter.set.results}")
                                .setCallback(EMPTY_COMMAND + "results"),
                        new KeyboardButton()
                                .setName("${setter.set.calories}")
                                .setCallback(EMPTY_COMMAND + "calories"),
                        new KeyboardButton()
                                .setName("${setter.set.email}")
                                .setCallback(EMPTY_COMMAND + "email")
                ));
    }
}
