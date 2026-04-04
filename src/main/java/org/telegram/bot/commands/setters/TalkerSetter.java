package org.telegram.bot.commands.setters;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.commands.Set;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.TalkerDegree;
import org.telegram.bot.domain.entities.TalkerUserSettings;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.AccessLevel;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.Emoji;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.telegram.bot.utils.TextUtils.containsStartWith;
import static org.telegram.bot.utils.TextUtils.getStartsWith;

@Service
@RequiredArgsConstructor
@Slf4j
public class TalkerSetter implements Setter<BotResponse> {

    private static final String CALLBACK_COMMAND = "${setter.command} ";
    private static final String EMPTY_TALKER_COMMAND = "${setter.talker.emptycommand}";
    private static final String CALLBACK_SET_TALKER_COMMAND = CALLBACK_COMMAND + EMPTY_TALKER_COMMAND;
    private static final String SET_IDLE_MINUTES_COMMAND = EMPTY_TALKER_COMMAND + "m";
    private static final String CALLBACK_SET_IDLE_MINUTES_COMMAND = CALLBACK_COMMAND + SET_IDLE_MINUTES_COMMAND;
    private static final String SET_DO_NOT_REPLY_COMMAND = EMPTY_TALKER_COMMAND + "d";
    private static final String CALLBACK_SET_DO_NOT_REPLY_COMMAND = CALLBACK_COMMAND + SET_DO_NOT_REPLY_COMMAND;

    private final java.util.Set<String> emptyTalkerCommands = new HashSet<>();
    private final java.util.Set<String> setIdleCommands = new HashSet<>();
    private final java.util.Set<String> setDoNotReplyMeCommands = new HashSet<>();

    private final TalkerDegreeService talkerDegreeService;
    private final CommandWaitingService commandWaitingService;
    private final SpeechService speechService;
    private final InternationalizationService internationalizationService;
    private final UserService userService;
    private final TalkerUserSettingsService talkerUserSettingsService;

    @PostConstruct
    private void postConstruct() {
        emptyTalkerCommands.addAll(internationalizationService.getAllTranslations("setter.talker.emptycommand"));
        setIdleCommands.addAll(internationalizationService.internationalize(SET_IDLE_MINUTES_COMMAND));
        setDoNotReplyMeCommands.addAll(internationalizationService.internationalize(SET_DO_NOT_REPLY_COMMAND));
    }

    @Override
    public boolean canProcessed(String command) {
        return emptyTalkerCommands.stream().anyMatch(command::startsWith);
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.FAMILIAR;
    }

    @Override
    public BotResponse set(BotRequest request, String commandText) {
        Message message = request.getMessage();
        Chat chat = message.getChat();
        User user = message.getUser();
        String lowerCaseCommandText = commandText.toLowerCase(Locale.ROOT);

        if (message.isCallback()) {
            if (emptyTalkerCommands.contains(lowerCaseCommandText)) {
                return getTalkerSetterWithKeyboard(message, chat, user, false);
            } else if (containsStartWith(setIdleCommands, lowerCaseCommandText)) {
                return setIdleMinutes(message, chat, user, commandText);
            } else if (containsStartWith(setDoNotReplyMeCommands, lowerCaseCommandText)) {
                return setDoNotReplyMeByCallback(message, chat, user, commandText);
            } else if (containsStartWith(emptyTalkerCommands, lowerCaseCommandText)) {
                return selectTalkerDegreeByCallback(message, chat, user, commandText);
            }
        }

        if (emptyTalkerCommands.contains(lowerCaseCommandText)) {
            return getTalkerSetterWithKeyboard(message, chat, user, true);
        } else if (containsStartWith(setIdleCommands, lowerCaseCommandText)) {
            return setIdleMinutes(message, chat, user, commandText);
        } else if (containsStartWith(emptyTalkerCommands, lowerCaseCommandText)) {
            return selectTalkerDegree(message, chat, user, commandText);
        } else {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }

    private BotResponse setIdleMinutes(Message message, Chat chat, User user, String command) {
        if (!userService.isUserHaveAccessForCommand(user, AccessLevel.MODERATOR.getValue())) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_ACCESS));
        }

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

        return new TextResponse(message)
                .setText(responseText)
                .setResponseSettings(FormattingStyle.HTML);
    }

    private BotResponse getTalkerSetterWithKeyboard(Message message, Chat chat, User user, boolean newMessage) {
        log.debug("Request to get talker setter for chat {}", chat.getChatId());

        TalkerDegree talkerDegree = talkerDegreeService.get(chat.getChatId());

        Integer currentDegreeValue = talkerDegree.getDegree();
        String responseText = "${setter.talker.probability}: <b>" + currentDegreeValue + "%</b>\n" +
                "${setter.talker.downtime}: <b>" + talkerDegree.getChatIdleMinutes() + " ${setter.talker.m}.</b>";

        if (newMessage) {
            return new TextResponse(message)
                    .setText(responseText)
                    .setKeyboard(prepareKeyboardWithDegreeButtons(user))
                    .setResponseSettings(FormattingStyle.HTML);
        }

        return new EditResponse(message)
                .setText(responseText)
                .setKeyboard(prepareKeyboardWithDegreeButtons(user))
                .setResponseSettings(FormattingStyle.HTML);
    }

    private BotResponse selectTalkerDegreeByCallback(Message message, Chat chat, User user, String command) {
        if (!userService.isUserHaveAccessForCommand(user, AccessLevel.MODERATOR.getValue())) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_ACCESS));
        }

        String emptyTalkerCommand = getLocalizedCommand(command, EMPTY_TALKER_COMMAND);

        if (command.equals(emptyTalkerCommand)) {
            return getTalkerSetterWithKeyboard(message, chat, user, false);
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

        return getTalkerSetterWithKeyboard(message, chat, user, false);
    }

    private BotResponse selectTalkerDegree(Message message, Chat chat, User user, String command) {
        if (!userService.isUserHaveAccessForCommand(user, AccessLevel.MODERATOR.getValue())) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_ACCESS));
        }

        String emptyTalkerCommand = getLocalizedCommand(command, EMPTY_TALKER_COMMAND);
        log.debug("Request to select talker degree");

        if (command.equals(emptyTalkerCommand)) {
            return getTalkerSetterWithKeyboard(message, chat, user, true);
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

        return new TextResponse(message)
                .setText(speechService.getRandomMessageByTag(BotSpeechTag.SAVED));
    }

    private BotResponse setDoNotReplyMeByCallback(Message message, Chat chat, User user, String command) {
        String emptyTalkerCommand = getLocalizedCommand(command, EMPTY_TALKER_COMMAND);

        if (command.equals(emptyTalkerCommand)) {
            return getTalkerSetterWithKeyboard(message, chat, user, false);
        }

        TalkerUserSettings talkerUserSettings = talkerUserSettingsService.get(user);
        if (talkerUserSettings != null) {
            talkerUserSettingsService.remove(talkerUserSettings);
        } else {
            talkerUserSettingsService.save(new TalkerUserSettings().setChat(chat).setUser(user).setDoNotReply(true));
        }

        return getTalkerSetterWithKeyboard(message, chat, user, false);
    }

    private Keyboard prepareKeyboardWithDegreeButtons(User user) {
        String doNotReplyButtonEmoji;
        if (talkerUserSettingsService.doNotReply(user)) {
            doNotReplyButtonEmoji = Emoji.CHECK_MARK_BUTTON.getSymbol();
        } else {
            doNotReplyButtonEmoji = Emoji.NO_ENTRY.getSymbol();
        }

        return new Keyboard(List.of(
                Stream.of(0, 5, 10, 15, 20, 25, 30)
                        .map(value -> new KeyboardButton()
                                .setName(value.toString())
                                .setCallback(CALLBACK_SET_TALKER_COMMAND + " " + value))
                        .toList(),
                Stream.of(35, 40, 45, 50, 55, 60, 65)
                        .map(value -> new KeyboardButton()
                                .setName(value.toString())
                                .setCallback(CALLBACK_SET_TALKER_COMMAND + " " + value))
                        .toList(),
                Stream.of(70, 75, 80, 85, 90, 95, 100)
                        .map(value -> new KeyboardButton()
                                .setName(value.toString())
                                .setCallback(CALLBACK_SET_TALKER_COMMAND + " " + value))
                        .toList(),
                List.of(new KeyboardButton()
                        .setName(Emoji.HOURGLASS_NOT_DONE.getSymbol() + "${setter.talker.button.downtime}")
                        .setCallback(CALLBACK_SET_IDLE_MINUTES_COMMAND)),
                List.of(new KeyboardButton()
                        .setName(doNotReplyButtonEmoji + "${setter.talker.button.donotreplyme}")
                        .setCallback(CALLBACK_SET_DO_NOT_REPLY_COMMAND)),
                List.of(new KeyboardButton()
                        .setName(Emoji.BACK.getSymbol() + "${setter.talker.button.settings}")
                        .setCallback(CALLBACK_COMMAND + "back"))));
    }

    private String getLocalizedCommand(String text, String command) {
        String localizedCommand = getStartsWith(
                internationalizationService.internationalize(command),
                text.toLowerCase(Locale.ROOT));

        if (localizedCommand == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        return localizedCommand;
    }

}
