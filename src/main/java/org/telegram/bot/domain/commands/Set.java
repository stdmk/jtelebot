package org.telegram.bot.domain.commands;

import liquibase.pro.packaged.E;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.commands.setters.CitySetter;
import org.telegram.bot.domain.commands.setters.NewsSetter;
import org.telegram.bot.domain.entities.CommandWaiting;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.enums.AccessLevels;
import org.telegram.bot.domain.enums.ParseModes;
import org.telegram.bot.services.CommandWaitingService;
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
@AllArgsConstructor
public class Set implements CommandParent<PartialBotApiMethod<?>> {

    private final CommandWaitingService commandWaitingService;
    private final NewsSetter newsSetter;
    private final CitySetter citySetter;
    private final UserService userService;

    private final String NEWS = "новости";
    private final String CITY = "город";

    @Override
    public PartialBotApiMethod<?> parse(Update update) throws Exception {
        Message message = getMessageFromUpdate(update);
        CommandWaiting commandWaiting = commandWaitingService.get(message.getChatId(), message.getFrom().getId());
        Integer userId = message.getFrom().getId();
        String textMessage = message.getText();
        if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            textMessage = cutCommandInText(callbackQuery.getData());
            userId = callbackQuery.getFrom().getId();
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
            AccessLevels userAccessLevel = userService.getCurrentAccessLevel(userId, message.getChatId());
            if (textMessage.toLowerCase().startsWith(NEWS)) {
                if (userService.isUserHaveAccessForCommand(userAccessLevel.getValue(), AccessLevels.MODERATOR.getValue())) {
                    return newsSetter.set(update, textMessage);
                }
            } else if (textMessage.toLowerCase().startsWith(CITY)) {
                if (userService.isUserHaveAccessForCommand(userAccessLevel.getValue(), AccessLevels.TRUSTED.getValue())) {
                    return citySetter.set(update, textMessage);
                }
            }
            return buildMainPageWithCallback(message);
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
        String CITY = "город";
        cityButton.setText(SET + CITY);
        cityButton.setCallbackData(SET + CITY);

        List<InlineKeyboardButton> cityRow = new ArrayList<>();
        newsRow.add(cityButton);

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(newsRow);
        rows.add(cityRow);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }
}
