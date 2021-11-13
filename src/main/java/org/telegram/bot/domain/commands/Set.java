package org.telegram.bot.domain.commands;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.commands.setters.AliasSetter;
import org.telegram.bot.domain.commands.setters.CitySetter;
import org.telegram.bot.domain.commands.setters.DisableCommandSetter;
import org.telegram.bot.domain.commands.setters.HolidaySetter;
import org.telegram.bot.domain.commands.setters.NewsSetter;
import org.telegram.bot.domain.commands.setters.TvSetter;
import org.telegram.bot.domain.entities.CommandWaiting;
import org.telegram.bot.domain.enums.AccessLevel;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.ChatService;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserService;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class Set implements CommandParent<PartialBotApiMethod<?>> {

    private final CommandWaitingService commandWaitingService;
    private final ChatService chatService;
    private final UserService userService;
    private final SpeechService speechService;

    private final NewsSetter newsSetter;
    private final CitySetter citySetter;
    private final AliasSetter aliasSetter;
    private final TvSetter tvSetter;
    private final HolidaySetter holidaySetter;
    private final DisableCommandSetter disableCommandSetter;

    private final String NEWS = "новости";
    private final String CITY = "город";
    private final String ALIAS = "алиас";
    private final String TV = "тв";
    private final String HOLIDAY = "праздник";
    private final String COMMAND = "команда";

    @Override
    public PartialBotApiMethod<?> parse(Update update) {
        Message message = getMessageFromUpdate(update);
        Long userId = message.getFrom().getId();
        String textMessage = message.getText();
        boolean callback = false;

        CommandWaiting commandWaiting = commandWaitingService.get(chatService.get(message.getChatId()), userService.get(message.getFrom().getId()));

        if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            textMessage = cutCommandInText(callbackQuery.getData());
            userId = callbackQuery.getFrom().getId();
            callback = true;
        } else if (commandWaiting != null) {
            textMessage = cutCommandInText(commandWaiting.getTextMessage() + textMessage);
        } else {
            textMessage = cutCommandInText(textMessage);
        }

        if (textMessage == null || textMessage.toLowerCase().startsWith("back")) {
            if (update.hasCallbackQuery()) {
                return buildMainPageWithCallback(message);
            } else {
                return buildMainPage(message);
            }
        } else {
            AccessLevel userAccessLevel = userService.getCurrentAccessLevel(userId, message.getChatId());
            if (textMessage.toLowerCase().startsWith(NEWS)) {
                if (userService.isUserHaveAccessForCommand(userAccessLevel.getValue(), AccessLevel.MODERATOR.getValue())) {
                    return newsSetter.set(update, textMessage);
                }
            } else if (textMessage.toLowerCase().startsWith(CITY)) {
                if (userService.isUserHaveAccessForCommand(userAccessLevel.getValue(), AccessLevel.TRUSTED.getValue())) {
                    return citySetter.set(update, textMessage);
                }
            } else if (textMessage.toLowerCase().startsWith(ALIAS)) {
                if (userService.isUserHaveAccessForCommand(userAccessLevel.getValue(), AccessLevel.TRUSTED.getValue())) {
                    return aliasSetter.set(update, textMessage);
                }
            } else if (textMessage.toLowerCase().startsWith(TV)) {
                if (userService.isUserHaveAccessForCommand(userAccessLevel.getValue(), AccessLevel.TRUSTED.getValue())) {
                    return tvSetter.set(update, textMessage);
                }
            } else if (textMessage.toLowerCase().startsWith(HOLIDAY)) {
                if (userService.isUserHaveAccessForCommand(userAccessLevel.getValue(), AccessLevel.TRUSTED.getValue())) {
                    return holidaySetter.set(update, textMessage);
                }
            } else if (textMessage.toLowerCase().startsWith(COMMAND)) {
                if (userService.isUserHaveAccessForCommand(userAccessLevel.getValue(), AccessLevel.MODERATOR.getValue())) {
                    return disableCommandSetter.set(update, textMessage);
                }
            }
            if (callback) {
                return buildMainPageWithCallback(message);
            }
            commandWaitingService.remove(commandWaiting);

            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }

    private SendMessage buildMainPage(Message message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.enableHtml(true);
        sendMessage.setText("<b>Установки</b>");
        sendMessage.setReplyMarkup(buildMainKeyboard());

        return sendMessage;
    }

    private EditMessageText buildMainPageWithCallback(Message message) {
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.enableHtml(true);
        editMessageText.setText("<b>Установки</b>");
        editMessageText.setReplyMarkup(buildMainKeyboard());

        return editMessageText;
    }

    private InlineKeyboardMarkup buildMainKeyboard() {
        InlineKeyboardButton newsButton = new InlineKeyboardButton();
        String SET = "установить ";
        newsButton.setText(SET + NEWS);
        newsButton.setCallbackData(SET + NEWS);

        List<InlineKeyboardButton> newsRow = new ArrayList<>();
        newsRow.add(newsButton);

        InlineKeyboardButton cityButton = new InlineKeyboardButton();
        cityButton.setText(SET + CITY);
        cityButton.setCallbackData(SET + CITY);

        List<InlineKeyboardButton> cityRow = new ArrayList<>();
        cityRow.add(cityButton);

        InlineKeyboardButton aliasButton = new InlineKeyboardButton();
        aliasButton.setText(SET + ALIAS);
        aliasButton.setCallbackData(SET + ALIAS);

        List<InlineKeyboardButton> aliasRow = new ArrayList<>();
        aliasRow.add(aliasButton);

        InlineKeyboardButton tvButton = new InlineKeyboardButton();
        tvButton.setText(SET + TV);
        tvButton.setCallbackData(SET + TV);

        List<InlineKeyboardButton> tvRow = new ArrayList<>();
        tvRow.add(tvButton);

        InlineKeyboardButton holidayButton = new InlineKeyboardButton();
        holidayButton.setText(SET + HOLIDAY);
        holidayButton.setCallbackData(SET + HOLIDAY);

        List<InlineKeyboardButton> holidayRow = new ArrayList<>();
        holidayRow.add(holidayButton);

        InlineKeyboardButton commandButton = new InlineKeyboardButton();
        commandButton.setText(SET + COMMAND);
        commandButton.setCallbackData(SET + COMMAND);

        List<InlineKeyboardButton> commandRow = new ArrayList<>();
        commandRow.add(commandButton);

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(newsRow);
        rows.add(cityRow);
        rows.add(aliasRow);
        rows.add(tvRow);
        rows.add(holidayRow);
        rows.add(commandRow);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }
}
