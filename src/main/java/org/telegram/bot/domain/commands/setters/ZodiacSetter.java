package org.telegram.bot.domain.commands.setters;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.*;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.domain.enums.Emoji;
import org.telegram.bot.domain.enums.Zodiac;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserZodiacService;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZodiacSetter implements SetterParent<PartialBotApiMethod<?>> {

    private final UserZodiacService userZodiacService;
    private final SpeechService speechService;

    private final String CALLBACK_COMMAND = "установить ";
    private final String SELECT_ZODIAC_COMMAND = "зодиак выбрать";
    private final String UPDATE_ZODIAC_COMMAND = "зодиак обновить";
    private final String CALLBACK_SELECT_ZODIAC_COMMAND = CALLBACK_COMMAND + SELECT_ZODIAC_COMMAND;

    @Override
    public PartialBotApiMethod<?> set(Update update, String commandText) {
        Message message = getMessageFromUpdate(update);
        Chat chat = new Chat().setChatId(message.getChatId());
        String lowerCaseCommandText = commandText.toLowerCase();

        String EMPTY_CITY_COMMAND = "зодиак";
        if (update.hasCallbackQuery()) {
            User user = new User().setUserId(update.getCallbackQuery().getFrom().getId());

            if (lowerCaseCommandText.equals(EMPTY_CITY_COMMAND) || lowerCaseCommandText.equals(UPDATE_ZODIAC_COMMAND)) {
                return getUserZodiacWithKeyboard(message, chat, user, false);
            } else if (lowerCaseCommandText.startsWith(SELECT_ZODIAC_COMMAND)) {
                return selectZodiacByCallback(message, chat, user, commandText);
            }
        }

        User user = new User().setUserId(message.getFrom().getId());
        if (lowerCaseCommandText.equals(EMPTY_CITY_COMMAND)) {
            return getUserZodiacWithKeyboard(message,  chat, user, true);
        } else if (lowerCaseCommandText.startsWith(SELECT_ZODIAC_COMMAND)) {
            return selectUserZodiac(message, chat, user, commandText);
        } else {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }

    private PartialBotApiMethod<?> selectZodiacByCallback(Message message, Chat chat, User user, String command) throws BotException {
        command = command.replace(CALLBACK_COMMAND, "");

        if (command.equals(SELECT_ZODIAC_COMMAND)) {
            return getUserZodiacWithKeyboard(message, chat, user, false);
        }

        Zodiac zodiac;
        try {
            zodiac = Zodiac.valueOf(command.substring(SELECT_ZODIAC_COMMAND.length() + 1));
        } catch (IllegalArgumentException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        UserZodiac userZodiac = userZodiacService.get(chat, user);
        if (userZodiac == null) {
            userZodiac = new UserZodiac().setChat(chat).setUser(user);
        }
        userZodiac.setZodiac(zodiac);

        userZodiacService.save(userZodiac);

        return getUserZodiacWithKeyboard(message, chat, user, false);
    }

    private PartialBotApiMethod<?> selectUserZodiac(Message message, Chat chat, User user, String command) throws BotException {
        log.debug("Request to select userTv");
        if (command.equals(SELECT_ZODIAC_COMMAND)) {
            return selectZodiacByCallback(message,  chat, user, SELECT_ZODIAC_COMMAND);
        }

        Zodiac zodiac;
        try {
            zodiac = Zodiac.findByNames((command.substring(SELECT_ZODIAC_COMMAND.length() + 1)).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        UserZodiac userZodiac = userZodiacService.get(chat, user);
        if (userZodiac == null) {
            userZodiac = new UserZodiac().setChat(chat).setUser(user);
        }
        userZodiac.setZodiac(zodiac);

        userZodiacService.save(userZodiac);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.enableMarkdown(true);
        sendMessage.setText(speechService.getRandomMessageByTag(BotSpeechTag.SAVED));

        return sendMessage;
    }

    private PartialBotApiMethod<?> getUserZodiacWithKeyboard(Message message, Chat chat, User user, Boolean newMessage) {
        UserZodiac userZodiac = userZodiacService.get(chat, user);

        String zodiacNameRu;
        if (userZodiac == null) {
            zodiacNameRu = "Не выбран";
        } else {
            zodiacNameRu = userZodiac.getZodiac().getNameRu();
        }

        if (newMessage) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId().toString());
            sendMessage.enableHtml(true);
            sendMessage.setText("Выбранный зодиак: <b>" + zodiacNameRu + "</b>");
            sendMessage.setReplyMarkup(prepareKeyboardWithUserTvList());

            return sendMessage;
        }

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.enableHtml(true);
        editMessageText.setText("Выбранный зодиак: <b>" + zodiacNameRu + "</b>");
        editMessageText.setReplyMarkup(prepareKeyboardWithUserTvList());

        return editMessageText;
    }

    private InlineKeyboardMarkup prepareKeyboardWithUserTvList() {
        List<List<InlineKeyboardButton>> rows = Arrays.stream(Zodiac.values()).map(zodiac -> {
            List<InlineKeyboardButton> selectButtonRow = new ArrayList<>();

            InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
            inlineKeyboardButton.setText(zodiac.getEmoji() + zodiac.getNameRu());
            inlineKeyboardButton.setCallbackData(CALLBACK_SELECT_ZODIAC_COMMAND + " " + zodiac.name());

            selectButtonRow.add(inlineKeyboardButton);

            return selectButtonRow;
        }).collect(Collectors.toList());

        List<InlineKeyboardButton> updateButtonRow = new ArrayList<>();
        InlineKeyboardButton updateButton = new InlineKeyboardButton();
        updateButton.setText(Emoji.UPDATE.getEmoji() + "Обновить");
        updateButton.setCallbackData(CALLBACK_COMMAND + UPDATE_ZODIAC_COMMAND);
        updateButtonRow.add(updateButton);

        List<InlineKeyboardButton> backButtonRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(Emoji.BACK.getEmoji() + "Установки");
        backButton.setCallbackData(CALLBACK_COMMAND + "back");
        backButtonRow.add(backButton);

        rows.add(updateButtonRow);
        rows.add(backButtonRow);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }
}
