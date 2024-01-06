package org.telegram.bot.commands.setters;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.commands.Set;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.TalkerDegree;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.enums.AccessLevel;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.Emoji;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.InternationalizationService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.TalkerDegreeService;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

import static org.telegram.bot.utils.TextUtils.containsStartWith;
import static org.telegram.bot.utils.TextUtils.getStartsWith;

@Service
@RequiredArgsConstructor
@Slf4j
public class TalkerSetter implements Setter<PartialBotApiMethod<?>> {

    private static final String CALLBACK_COMMAND = "${setter.command} ";
    private static final String EMPTY_TALKER_COMMAND = "${setter.talker.emptycommand}";
    private static final String CALLBACK_SET_TALKER_COMMAND = CALLBACK_COMMAND + EMPTY_TALKER_COMMAND;
    private static final String SET_IDLE_MINUTES_COMMAND = EMPTY_TALKER_COMMAND + "m";
    private static final String CALLBACK_SET_IDLE_MINUTES_COMMAND = CALLBACK_COMMAND + SET_IDLE_MINUTES_COMMAND;

    private final java.util.Set<String> emptyTalkerCommands = new HashSet<>();
    private final java.util.Set<String> setIdleCommands = new HashSet<>();

    private final TalkerDegreeService talkerDegreeService;
    private final CommandWaitingService commandWaitingService;
    private final SpeechService speechService;
    private final InternationalizationService internationalizationService;

    @PostConstruct
    private void postConstruct() {
        emptyTalkerCommands.addAll(internationalizationService.getAllTranslations("setter.talker.emptycommand"));
        setIdleCommands.addAll(internationalizationService.internationalize(SET_IDLE_MINUTES_COMMAND));
    }

    @Override
    public boolean canProcessed(String command) {
        return emptyTalkerCommands.stream().anyMatch(command::startsWith);
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.MODERATOR;
    }

    @Override
    public PartialBotApiMethod<?> set(Update update, String commandText) {
        Message message = getMessageFromUpdate(update);
        Chat chat = new Chat().setChatId(message.getChatId());
        String lowerCaseCommandText = commandText.toLowerCase();

        if (update.hasCallbackQuery()) {
            User user = new User().setUserId(update.getCallbackQuery().getFrom().getId());

            if (emptyTalkerCommands.contains(lowerCaseCommandText)) {
                return getTalkerSetterWithKeyboard(message, chat, false);
            } else if (containsStartWith(setIdleCommands, lowerCaseCommandText)) {
                return setIdleMinutes(message, chat, user, commandText);
            } else if (containsStartWith(emptyTalkerCommands, lowerCaseCommandText)) {
                return selectTalkerDegreeByCallback(message, chat, commandText);
            }
        }

        User user = new User().setUserId(message.getFrom().getId());
        if (emptyTalkerCommands.contains(lowerCaseCommandText)) {
            return getTalkerSetterWithKeyboard(message, chat, true);
        } else if (containsStartWith(setIdleCommands, lowerCaseCommandText)) {
            return setIdleMinutes(message, chat, user, commandText);
        } else if (containsStartWith(emptyTalkerCommands, lowerCaseCommandText)) {
            return selectTalkerDegree(message, chat, commandText);
        } else {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }

    private PartialBotApiMethod<?> setIdleMinutes(Message message, Chat chat, User user, String command) {
        commandWaitingService.remove(chat, user);

        String setIdleMinutesCommand = getLocalizedCommand(command, SET_IDLE_MINUTES_COMMAND);
        String responseText;

        if (setIdleMinutesCommand.equals(command)) {
            commandWaitingService.add(chat, user, Set.class, CALLBACK_SET_IDLE_MINUTES_COMMAND);
            responseText = "${setter.talker.sethelp}";
        } else {
            int minutes;
            try {
                minutes = Integer.parseInt(command.substring(setIdleMinutesCommand.length() + 1));
            } catch (NumberFormatException e) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            TalkerDegree talkerDegree = talkerDegreeService.get(chat.getChatId());
            talkerDegree.setChatIdleMinutes(minutes);
            talkerDegreeService.save(talkerDegree);

            responseText = speechService.getRandomMessageByTag(BotSpeechTag.SAVED);
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.enableHtml(true);
        sendMessage.setText(responseText);

        return sendMessage;
    }

    private PartialBotApiMethod<?> getTalkerSetterWithKeyboard(Message message, Chat chat, boolean newMessage) {
        log.debug("Request to get talker setter for chat {}", chat.getChatId());

        TalkerDegree talkerDegree = talkerDegreeService.get(chat.getChatId());

        Integer currentDegreeValue = talkerDegree.getDegree();
        String responseText = "${setter.talker.probability}: <b>" + currentDegreeValue + "%</b>\n" +
                "${setter.talker.downtime}: <b>" + talkerDegree.getChatIdleMinutes() + " ${setter.talker.m}.</b>";

        if (newMessage) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId().toString());
            sendMessage.enableHtml(true);
            sendMessage.setText(responseText);
            sendMessage.setReplyMarkup(prepareKeyboardWithDegreeButtons());

            return sendMessage;
        }

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.enableHtml(true);
        editMessageText.setText(responseText);
        editMessageText.setReplyMarkup(prepareKeyboardWithDegreeButtons());

        return editMessageText;
    }

    private PartialBotApiMethod<?> selectTalkerDegreeByCallback(Message message, Chat chat, String command) {
        String emptyTalkerCommand = getLocalizedCommand(command, EMPTY_TALKER_COMMAND);

        if (command.equals(emptyTalkerCommand)) {
            return getTalkerSetterWithKeyboard(message, chat, false);
        }

        int degree;
        try {
            degree = Integer.parseInt(command.substring(emptyTalkerCommand.length() + 1));
        } catch (Exception e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        if (degree > 100) {
            degree = 100;
        } else if (degree < 0) {
            degree = 0;
        }

        TalkerDegree talkerDegree = talkerDegreeService.get(chat.getChatId());
        talkerDegreeService.save(talkerDegree.setDegree(degree));

        return getTalkerSetterWithKeyboard(message, chat, false);
    }

    private PartialBotApiMethod<?> selectTalkerDegree(Message message, Chat chat, String command) {
        String emptyTalkerCommand = getLocalizedCommand(command, EMPTY_TALKER_COMMAND);
        log.debug("Request to select talker degree");

        if (command.equals(emptyTalkerCommand)) {
            return getTalkerSetterWithKeyboard(message, chat, true);
        }

        int degree;
        try {
            degree = Integer.parseInt(command.substring(emptyTalkerCommand.length() + 1));
        } catch (Exception e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        if (degree > 100) {
            degree = 100;
        } else if (degree < 0) {
            degree = 0;
        }

        TalkerDegree talkerDegree = talkerDegreeService.get(chat.getChatId());
        talkerDegreeService.save(talkerDegree.setDegree(degree));

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.enableHtml(true);
        sendMessage.setText(speechService.getRandomMessageByTag(BotSpeechTag.SAVED));

        return sendMessage;
    }

    private InlineKeyboardMarkup prepareKeyboardWithDegreeButtons() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> degreeRow1 = new ArrayList<>();
        List<InlineKeyboardButton> degreeRow2 = new ArrayList<>();
        List<InlineKeyboardButton> degreeRow3 = new ArrayList<>();

        Stream.of(0, 5, 10, 15, 20, 25, 30)
                .forEach(value -> {
                    InlineKeyboardButton degreeButton = new InlineKeyboardButton();
                    degreeButton.setText(value.toString());
                    degreeButton.setCallbackData(CALLBACK_SET_TALKER_COMMAND + " " + value);
                    degreeRow1.add(degreeButton);});

        Stream.of(35, 40, 45, 50, 55, 60, 65)
                .forEach(value -> {
                    InlineKeyboardButton degreeButton = new InlineKeyboardButton();
                    degreeButton.setText(value.toString());
                    degreeButton.setCallbackData(CALLBACK_SET_TALKER_COMMAND + " " + value);
                    degreeRow2.add(degreeButton);});

        Stream.of(70, 75, 80, 85, 90, 95, 100)
                .forEach(value -> {
                    InlineKeyboardButton degreeButton = new InlineKeyboardButton();
                    degreeButton.setText(value.toString());
                    degreeButton.setCallbackData(CALLBACK_SET_TALKER_COMMAND + " " + value);
                    degreeRow3.add(degreeButton);});

        List<InlineKeyboardButton> setIdleRow = new ArrayList<>();
        InlineKeyboardButton setIdleButton = new InlineKeyboardButton();
        setIdleButton.setText(Emoji.HOURGLASS_NOT_DONE.getSymbol() + "${setter.talker.button.downtime}");
        setIdleButton.setCallbackData(CALLBACK_SET_IDLE_MINUTES_COMMAND);
        setIdleRow.add(setIdleButton);

        List<InlineKeyboardButton> backButtonRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(Emoji.BACK.getSymbol() + "${setter.talker.button.settings}");
        backButton.setCallbackData(CALLBACK_COMMAND + "back");
        backButtonRow.add(backButton);

        rows.add(degreeRow1);
        rows.add(degreeRow2);
        rows.add(degreeRow3);
        rows.add(setIdleRow);
        rows.add(backButtonRow);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }

    private String getLocalizedCommand(String text, String command) {
        String localizedCommand = getStartsWith(
                internationalizationService.internationalize(command),
                text.toLowerCase());

        if (localizedCommand == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        return localizedCommand;
    }

}
