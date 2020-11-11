package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.commands.setters.NewsSetter;
import org.telegram.bot.domain.entities.CommandWaiting;
import org.telegram.bot.domain.enums.ParseModes;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
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

    private final String SET = "установить ";
    private final String NEWS = "новости";
    private final String CITY = "город";

    @Override
    public PartialBotApiMethod<?> parse(Update update) throws Exception {
        //TODO проверка доступа и вывод соответствующих сеттеров
        Message message = getMessageFromUpdate(update);
        CommandWaiting commandWaiting = commandWaitingService.get(message.getChatId(), message.getFrom().getId());
        String textMessage = message.getText();
        if (update.hasCallbackQuery()) {
            textMessage = cutCommandInText(update.getCallbackQuery().getData());
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
            if (textMessage.toLowerCase().startsWith(NEWS)) {
                return newsSetter.set(update, textMessage);
            } else {
                return buildMainPage(message);
            }
        }
    }

    private SendMessage buildMainPage(Message message) {
        return new SendMessage()
                .setChatId(message.getChatId())
                .setReplyToMessageId(message.getMessageId())
                .setParseMode(ParseModes.HTML.getValue())
                .setText("<b>Установки</b>")
                .setReplyMarkup(buildMainKeyboard());
    }

    private EditMessageText buildMainPageWithCallback(Message message) {
        return new EditMessageText()
                .setChatId(message.getChatId())
                .setMessageId(message.getMessageId())
                .setParseMode(ParseModes.HTML.getValue())
                .setText("<b>Установки</b>")
                .setReplyMarkup(buildMainKeyboard());
    }

    private InlineKeyboardMarkup buildMainKeyboard() {
        InlineKeyboardButton newsButton = new InlineKeyboardButton();
        newsButton.setText("Установить новости");
        newsButton.setCallbackData("Установить новости");

        List<InlineKeyboardButton> newsRow = new ArrayList<>();
        newsRow.add(newsButton);

        InlineKeyboardButton cityButton = new InlineKeyboardButton();
        cityButton.setText(SET + CITY);
        cityButton.setCallbackData(SET + CITY);

        List<InlineKeyboardButton> cityRow = new ArrayList<>();
        newsRow.add(cityButton);

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(newsRow);
        rows.add(cityRow);

        return new InlineKeyboardMarkup().setKeyboard(rows);
    }
}
