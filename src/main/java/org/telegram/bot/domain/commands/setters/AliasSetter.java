package org.telegram.bot.domain.commands.setters;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.commands.Set;
import org.telegram.bot.domain.entities.Alias;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.entities.CommandWaiting;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.domain.enums.Emoji;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.AliasService;
import org.telegram.bot.services.CommandPropertiesService;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AliasSetter implements SetterParent<PartialBotApiMethod<?>> {

    private final AliasService aliasService;
    private final SpeechService speechService;
    private final CommandWaitingService commandWaitingService;
    private final CommandPropertiesService commandPropertiesService;

    private final String CALLBACK_COMMAND = "установить ";
    private final String UPDATE_ALIAS_COMMAND = "алиас обновить";
    private final String DELETE_ALIAS_COMMAND = "алиас удалить";
    private final String CALLBACK_DELETE_ALIAS_COMMAND = CALLBACK_COMMAND + DELETE_ALIAS_COMMAND;
    private final String ADD_ALIAS_COMMAND = "алиас добавить";
    private final String CALLBACK_ADD_ALIAS_COMMAND = CALLBACK_COMMAND + ADD_ALIAS_COMMAND;
    private final String ADDING_ALIAS_TEXT = "\nНапиши мне имя алиаса и его содержимое, например: дождь правда завтра дождь?";

    @Override
    public PartialBotApiMethod<?> set(Update update, String commandText) {
        Message message = getMessageFromUpdate(update);
        Chat chat = new Chat().setChatId(message.getChatId());
        String lowerCaseCommandText = commandText.toLowerCase();
        String EMPTY_ALIAS_COMMAND = "алиас";

        if (update.hasCallbackQuery()) {
            User user = new User().setUserId(update.getCallbackQuery().getFrom().getId());

            if (lowerCaseCommandText.equals(EMPTY_ALIAS_COMMAND) || lowerCaseCommandText.equals(UPDATE_ALIAS_COMMAND)) {
                return getAliasListWithKeyboard(message, chat, user, false);
            } else if (lowerCaseCommandText.startsWith(DELETE_ALIAS_COMMAND)) {
                return deleteAliasByCallback(message, chat, user, commandText);
            } else if (lowerCaseCommandText.startsWith(ADD_ALIAS_COMMAND)) {
                return addAliasByCallback(message, chat, user);
            }
        }

        User user = new User().setUserId(message.getFrom().getId());
        if (lowerCaseCommandText.equals(EMPTY_ALIAS_COMMAND)) {
            return getAliasListWithKeyboard(message, chat, user, true);
        } else if (lowerCaseCommandText.startsWith(DELETE_ALIAS_COMMAND)) {
            return deleteAlias(message, chat, user, commandText);
        } else if (lowerCaseCommandText.startsWith(ADD_ALIAS_COMMAND)) {
            return addAlias(message, chat, user, commandText);
        } else {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }

    private PartialBotApiMethod<?> getAliasListWithKeyboard(Message message, Chat chat, User user, Boolean newMessage) {
        log.debug("Request to list all aliases for chat {} and user {}", chat.getChatId(), user.getUserId());
        List<Alias> aliasList = aliasService.get(chat, user);

        if (newMessage) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId().toString());
            sendMessage.enableHtml(true);
            sendMessage.setText(prepareTextOfListAliases(aliasList));
            sendMessage.setReplyMarkup(prepareKeyboardWithAliases(aliasList));

            return sendMessage;
        }

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.enableHtml(true);
        editMessageText.setText(prepareTextOfListAliases(aliasList));
        editMessageText.setReplyMarkup(prepareKeyboardWithAliases(aliasList));

        return editMessageText;
    }

    private String prepareTextOfListAliases(List<Alias> aliasList) {
        final StringBuilder buf = new StringBuilder();
        buf.append("<b>Список твоих алиасов:</b>\n");

        aliasList.forEach(alias -> buf
                .append(alias.getId()).append(". ")
                .append(alias.getName()).append("\n"));

        return buf.toString();
    }

    private InlineKeyboardMarkup prepareKeyboardWithAliases(List<Alias> aliasList) {
        List<List<InlineKeyboardButton>> rows = aliasList.stream().map(alias -> {
            List<InlineKeyboardButton> aliasesRow = new ArrayList<>();

            InlineKeyboardButton aliasButton = new InlineKeyboardButton();
            aliasButton.setText(Emoji.DELETE.getEmoji() + alias.getName());
            aliasButton.setCallbackData(CALLBACK_DELETE_ALIAS_COMMAND + " " + alias.getId());

            aliasesRow.add(aliasButton);

            return aliasesRow;
        }).collect(Collectors.toList());

        List<InlineKeyboardButton> addButtonRow = new ArrayList<>();
        InlineKeyboardButton addButton = new InlineKeyboardButton();
        addButton.setText(Emoji.NEW.getEmoji() + "Добавить");
        addButton.setCallbackData(CALLBACK_ADD_ALIAS_COMMAND);
        addButtonRow.add(addButton);

        List<InlineKeyboardButton> updateButtonRow = new ArrayList<>();
        InlineKeyboardButton updateButton = new InlineKeyboardButton();
        updateButton.setText(Emoji.UPDATE.getEmoji() + "Обновить");
        updateButton.setCallbackData(CALLBACK_COMMAND + UPDATE_ALIAS_COMMAND);
        updateButtonRow.add(updateButton);

        List<InlineKeyboardButton> backButtonRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(Emoji.BACK.getEmoji() + "Установки");
        backButton.setCallbackData(CALLBACK_COMMAND + "back");
        backButtonRow.add(backButton);

        rows.add(addButtonRow);
        rows.add(updateButtonRow);
        rows.add(backButtonRow);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }

    private EditMessageText deleteAliasByCallback(Message message, Chat chat, User user, String command) {
        log.debug("Request to delete alias");

        aliasService.remove(chat, user, Long.valueOf(command.substring(DELETE_ALIAS_COMMAND.length() + 1)));

        return (EditMessageText) getAliasListWithKeyboard(message, chat, user, false);
    }

    private EditMessageText addAliasByCallback(Message message, Chat chat, User user) {
        commandWaitingService.add(chat, user, Set.class, CALLBACK_ADD_ALIAS_COMMAND);

        List<Alias> aliasList = aliasService.get(chat, user);

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.enableHtml(true);
        editMessageText.setReplyMarkup(prepareKeyboardWithAliases(aliasList));
        editMessageText.setText(prepareTextOfListAliases(aliasList) + ADDING_ALIAS_TEXT);

        return editMessageText;
    }

    private SendMessage deleteAlias(Message message, Chat chat, User user, String command) throws BotException {
        log.debug("Request to delete alias");

        String params;
        try {
            params = command.substring(DELETE_ALIAS_COMMAND.length() + 1);
        } catch (Exception e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        String responseText;

        try {
            Long aliasId = Long.parseLong(params);
            if (aliasService.remove(chat, user, aliasId)) {
                responseText = speechService.getRandomMessageByTag(BotSpeechTag.SAVED);
            } else {
                responseText = speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
            }
        } catch (Exception e) {
            if (aliasService.remove(chat, user, params)) {
                responseText = speechService.getRandomMessageByTag(BotSpeechTag.SAVED);
            } else {
                responseText = speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
            }
        }

        return buildSendMessageWithText(message, responseText);
    }

    private PartialBotApiMethod<?> addAlias(Message message, Chat chat, User user, String command) {
        log.debug("Request to add new alias");

        if (command.equals(ADD_ALIAS_COMMAND)) {
            List<Alias> allNewsInChat = aliasService.get(chat, user);

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId().toString());
            sendMessage.enableHtml(true);
            sendMessage.setReplyMarkup(prepareKeyboardWithAliases(allNewsInChat));
            sendMessage.setText(prepareTextOfListAliases(allNewsInChat) + ADDING_ALIAS_TEXT);

            return sendMessage;
        }

        String params = command.substring(ADD_ALIAS_COMMAND.length() + 1);

        int i = params.indexOf(" ");
        if (i < 0) {
            return buildSendMessageWithText(message, speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
        String aliasName = params.substring(0, i).toLowerCase(Locale.ROOT);
        String aliasValue = params.substring(i + 1);

        CommandProperties setProperties = commandPropertiesService.getCommand(Set.class);
        if (aliasValue.startsWith(setProperties.getCommandName()) ||
                aliasValue.startsWith(setProperties.getEnRuName()) ||
                aliasValue.startsWith(setProperties.getRussifiedName())) {
            commandWaitingService.remove(commandWaitingService.get(chat, user));
            return buildSendMessageWithText(message, speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        Alias alias = aliasService.get(chat, user, aliasName);
        if (alias != null) {
            return buildSendMessageWithText(message, "Алиас под таким именем уже существует");
        }

        alias = new Alias();
        alias.setChat(chat);
        alias.setUser(user);
        alias.setName(aliasName);
        alias.setValue(aliasValue);

        aliasService.save(alias);

        CommandWaiting commandWaiting = commandWaitingService.get(chat, user);
        if (commandWaiting != null && commandWaiting.getCommandName().equals("set")) {
            commandWaitingService.remove(commandWaiting);
        }

        return buildSendMessageWithText(message, speechService.getRandomMessageByTag(BotSpeechTag.SAVED));
    }

    private SendMessage buildSendMessageWithText(Message message, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.enableHtml(true);
        sendMessage.setText(text);

        return sendMessage;
    }
}
