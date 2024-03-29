package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.commands.setters.*;
import org.telegram.bot.domain.Command;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandWaiting;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.enums.AccessLevel;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
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
public class Set implements Command<PartialBotApiMethod<?>> {

    private static final String EMPTY_COMMAND = "set ";

    private final Bot bot;
    private final CommandWaitingService commandWaitingService;
    private final UserService userService;
    private final SpeechService speechService;

    private final List<Setter<?>> setters;

    @Override
    public List<PartialBotApiMethod<?>> parse(Update update) {
        Message message = getMessageFromUpdate(update);
        bot.sendTyping(message.getChatId());
        Long userId = message.getFrom().getId();
        String textMessage = message.getText();

        CommandWaiting commandWaiting = commandWaitingService.get(new Chat().setChatId(message.getChatId()), new User().setUserId(message.getFrom().getId()));

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
                return returnOneResult(buildMainPageWithCallback(message));
            } else {
                return returnOneResult(buildMainPage(message));
            }
        } else {
            String lowerCasedTextMessage = textMessage.toLowerCase();
            Setter<?> setter = setters
                    .stream()
                    .filter(setter1 -> setter1.canProcessed(lowerCasedTextMessage))
                    .findFirst()
                    .orElseThrow(() -> {
                        commandWaitingService.remove(commandWaiting);
                        return new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
                    });

            AccessLevel userAccessLevel = userService.getCurrentAccessLevel(userId, message.getChatId());
            if (userService.isUserHaveAccessForCommand(userAccessLevel, setter.getAccessLevel())) {
                return returnOneResult(setter.set(update, textMessage));
            } else {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_ACCESS));
            }
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

        newsButton.setText("${setter.set.news}");
        newsButton.setCallbackData(EMPTY_COMMAND + "${setter.set.news}");

        List<InlineKeyboardButton> newsRow = new ArrayList<>();
        newsRow.add(newsButton);

        InlineKeyboardButton cityButton = new InlineKeyboardButton();
        cityButton.setText("${setter.set.city}");
        cityButton.setCallbackData(EMPTY_COMMAND + "${setter.set.city}");

        List<InlineKeyboardButton> cityRow = new ArrayList<>();
        cityRow.add(cityButton);

        InlineKeyboardButton aliasButton = new InlineKeyboardButton();
        aliasButton.setText("${setter.set.alias}");
        aliasButton.setCallbackData(EMPTY_COMMAND + "${setter.set.alias}");

        List<InlineKeyboardButton> aliasRow = new ArrayList<>();
        aliasRow.add(aliasButton);

        InlineKeyboardButton tvButton = new InlineKeyboardButton();
        tvButton.setText("${setter.set.tv}");
        tvButton.setCallbackData(EMPTY_COMMAND + "${setter.set.tv}");

        List<InlineKeyboardButton> tvRow = new ArrayList<>();
        tvRow.add(tvButton);

        InlineKeyboardButton holidayButton = new InlineKeyboardButton();
        holidayButton.setText("${setter.set.holiday}");
        holidayButton.setCallbackData(EMPTY_COMMAND + "${setter.set.holiday}");

        List<InlineKeyboardButton> holidayRow = new ArrayList<>();
        holidayRow.add(holidayButton);

        InlineKeyboardButton commandButton = new InlineKeyboardButton();
        commandButton.setText("${setter.set.command}");
        commandButton.setCallbackData(EMPTY_COMMAND + "${setter.set.command}");

        List<InlineKeyboardButton> commandRow = new ArrayList<>();
        commandRow.add(commandButton);

        InlineKeyboardButton talkerButton = new InlineKeyboardButton();
        talkerButton.setText("${setter.set.talker}");
        talkerButton.setCallbackData(EMPTY_COMMAND + "${setter.set.talker}");

        List<InlineKeyboardButton> talkerRow = new ArrayList<>();
        talkerRow.add(talkerButton);

        InlineKeyboardButton zodiacButton = new InlineKeyboardButton();
        zodiacButton.setText("${setter.set.zodiac}");
        zodiacButton.setCallbackData(EMPTY_COMMAND + "${setter.set.zodiac}");

        List<InlineKeyboardButton> zodiacRow = new ArrayList<>();
        zodiacRow.add(zodiacButton);

        InlineKeyboardButton trainingButton = new InlineKeyboardButton();
        trainingButton.setText("${setter.set.trainings}");
        trainingButton.setCallbackData(EMPTY_COMMAND + "${setter.set.trainings}");

        List<InlineKeyboardButton> trainingRow = new ArrayList<>();
        trainingRow.add(trainingButton);

        InlineKeyboardButton chatGPTButton = new InlineKeyboardButton();
        chatGPTButton.setText("${setter.set.chatgpt}");
        chatGPTButton.setCallbackData(EMPTY_COMMAND + "${setter.set.chatgpt}");

        List<InlineKeyboardButton> chatGPTRow = new ArrayList<>();
        chatGPTRow.add(chatGPTButton);

        InlineKeyboardButton langButton = new InlineKeyboardButton();
        langButton.setText("${setter.set.lang}");
        langButton.setCallbackData(EMPTY_COMMAND + "${setter.set.lang}");

        List<InlineKeyboardButton> langRow = new ArrayList<>();
        langRow.add(langButton);

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(newsRow);
        rows.add(cityRow);
        rows.add(aliasRow);
        rows.add(tvRow);
        rows.add(holidayRow);
        rows.add(commandRow);
        rows.add(talkerRow);
        rows.add(zodiacRow);
        rows.add(trainingRow);
        rows.add(chatGPTRow);
        rows.add(langRow);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }
}
