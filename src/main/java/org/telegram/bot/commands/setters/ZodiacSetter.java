package org.telegram.bot.commands.setters;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.*;
import org.telegram.bot.enums.AccessLevel;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.Emoji;
import org.telegram.bot.enums.Zodiac;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.InternationalizationService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserZodiacService;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.TextUtils.containsStartWith;
import static org.telegram.bot.utils.TextUtils.getStartsWith;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZodiacSetter implements Setter<PartialBotApiMethod<?>> {

    private static final String CALLBACK_COMMAND = "${setter.command} ";
    private static final String SELECT_ZODIAC_COMMAND = "зодиак ${setter.zodiac.select}";
    private static final String UPDATE_ZODIAC_COMMAND = "зодиак ${setter.zodiac.update}";
    private static final String CALLBACK_SELECT_ZODIAC_COMMAND = CALLBACK_COMMAND + SELECT_ZODIAC_COMMAND;

    private final java.util.Set<String> emptyZodiacCommands = new HashSet<>();
    private final java.util.Set<String> selectZodiacCommands = new HashSet<>();
    private final java.util.Set<String> updateZodiacCommands = new HashSet<>();

    private final UserZodiacService userZodiacService;
    private final SpeechService speechService;
    private final InternationalizationService internationalizationService;

    @PostConstruct
    private void postConstruct() {
        emptyZodiacCommands.addAll(internationalizationService.getAllTranslations("setter.zodiac.emptycommand"));
        selectZodiacCommands.addAll(internationalizationService.internationalize(SELECT_ZODIAC_COMMAND));
        updateZodiacCommands.addAll(internationalizationService.internationalize(UPDATE_ZODIAC_COMMAND));
    }

    @Override
    public boolean canProcessed(String command) {
        return emptyZodiacCommands.stream().anyMatch(command::startsWith);
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.TRUSTED;
    }

    @Override
    public PartialBotApiMethod<?> set(Update update, String commandText) {
        Message message = getMessageFromUpdate(update);
        Chat chat = new Chat().setChatId(message.getChatId());
        String lowerCaseCommandText = commandText.toLowerCase();

        if (update.hasCallbackQuery()) {
            User user = new User().setUserId(update.getCallbackQuery().getFrom().getId());

            if (emptyZodiacCommands.contains(lowerCaseCommandText) || updateZodiacCommands.contains(lowerCaseCommandText)) {
                return getUserZodiacWithKeyboard(message, chat, user, false);
            } else if (containsStartWith(selectZodiacCommands, lowerCaseCommandText)) {
                return selectZodiacByCallback(message, chat, user, commandText);
            }
        }

        User user = new User().setUserId(message.getFrom().getId());
        if (emptyZodiacCommands.contains(lowerCaseCommandText)) {
            return getUserZodiacWithKeyboard(message,  chat, user, true);
        } else if (containsStartWith(selectZodiacCommands, lowerCaseCommandText)) {
            return selectUserZodiac(message, chat, user, commandText);
        } else {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }

    private PartialBotApiMethod<?> selectZodiacByCallback(Message message, Chat chat, User user, String command) throws BotException {
        String selectZodiacCommand = getLocalizedCommand(command);
        if (command.equals(selectZodiacCommand)) {
            return getUserZodiacWithKeyboard(message, chat, user, false);
        }

        Zodiac zodiac;
        try {
            zodiac = Zodiac.valueOf(command.substring(selectZodiacCommand.length() + 1));
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
        String selectZodiacCommand = getLocalizedCommand(command);
        log.debug("Request to select userTv");

        if (command.equals(selectZodiacCommand)) {
            return selectZodiacByCallback(message,  chat, user, SELECT_ZODIAC_COMMAND);
        }

        Zodiac zodiac;
        try {
            zodiac = Zodiac.findByNames((command.substring(selectZodiacCommand.length() + 1)).toUpperCase(Locale.ROOT));
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

    private PartialBotApiMethod<?> getUserZodiacWithKeyboard(Message message, Chat chat, User user, boolean newMessage) {
        UserZodiac userZodiac = userZodiacService.get(chat, user);

        String zodiacName;
        if (userZodiac == null) {
            zodiacName = "${setter.zodiac.notset}";
        } else {
            zodiacName = userZodiac.getZodiac().getName();
        }

        if (newMessage) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId().toString());
            sendMessage.enableHtml(true);
            sendMessage.setText("${setter.zodiac.chosenzodiac}: <b>" + zodiacName + "</b>");
            sendMessage.setReplyMarkup(prepareKeyboardWithUserTvList());

            return sendMessage;
        }

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.enableHtml(true);
        editMessageText.setText("${setter.zodiac.chosenzodiac}: <b>" + zodiacName + "</b>");
        editMessageText.setReplyMarkup(prepareKeyboardWithUserTvList());

        return editMessageText;
    }

    private InlineKeyboardMarkup prepareKeyboardWithUserTvList() {
        List<List<InlineKeyboardButton>> rows = Arrays.stream(Zodiac.values()).map(zodiac -> {
            List<InlineKeyboardButton> selectButtonRow = new ArrayList<>();

            InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
            inlineKeyboardButton.setText(zodiac.getEmoji() + zodiac.getName());
            inlineKeyboardButton.setCallbackData(CALLBACK_SELECT_ZODIAC_COMMAND + " " + zodiac.name());

            selectButtonRow.add(inlineKeyboardButton);

            return selectButtonRow;
        }).collect(Collectors.toList());

        List<InlineKeyboardButton> updateButtonRow = new ArrayList<>();
        InlineKeyboardButton updateButton = new InlineKeyboardButton();
        updateButton.setText(Emoji.UPDATE.getSymbol() + "${setter.zodiac.button.update}");
        updateButton.setCallbackData(CALLBACK_COMMAND + UPDATE_ZODIAC_COMMAND);
        updateButtonRow.add(updateButton);

        List<InlineKeyboardButton> backButtonRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(Emoji.BACK.getSymbol() + "${setter.zodiac.button.settings}");
        backButton.setCallbackData(CALLBACK_COMMAND + "back");
        backButtonRow.add(backButton);

        rows.add(updateButtonRow);
        rows.add(backButtonRow);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }

    private String getLocalizedCommand(String text) {
        String localizedCommand = getStartsWith(
                internationalizationService.internationalize(ZodiacSetter.SELECT_ZODIAC_COMMAND),
                text.toLowerCase());

        if (localizedCommand == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        return localizedCommand;
    }

}
