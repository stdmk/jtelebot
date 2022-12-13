package org.telegram.bot.domain.commands.setters;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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
    private final String SELECT_PAGE = " page";
    private final String UPDATE_ALIAS_COMMAND = "алиас обновить";
    private final String DELETE_ALIAS_COMMAND = "алиас удалить";
    private final String SELECT_ALIAS_COMMAND = "алиас выбрать";
    private final String CALLBACK_DELETE_ALIAS_COMMAND = CALLBACK_COMMAND + DELETE_ALIAS_COMMAND;
    private final String CALLBACK_SELECT_ALIAS_COMMAND = CALLBACK_COMMAND + SELECT_ALIAS_COMMAND;
    private final String ADD_ALIAS_COMMAND = "алиас добавить";
    private final String CALLBACK_ADD_ALIAS_COMMAND = CALLBACK_COMMAND + ADD_ALIAS_COMMAND;
    private final String ADDING_ALIAS_TEXT = "\nНапиши мне имя алиаса и его содержимое, например: дождь правда завтра дождь?";
    private final int FIRST_PAGE = 0;

    @Override
    public PartialBotApiMethod<?> set(Update update, String commandText) {
        Message message = getMessageFromUpdate(update);
        Chat chat = new Chat().setChatId(message.getChatId());
        String lowerCaseCommandText = commandText.toLowerCase();
        String EMPTY_ALIAS_COMMAND = "алиас";

        if (update.hasCallbackQuery()) {
            User user = new User().setUserId(update.getCallbackQuery().getFrom().getId());

            if (lowerCaseCommandText.equals(EMPTY_ALIAS_COMMAND) || lowerCaseCommandText.equals(UPDATE_ALIAS_COMMAND)) {
                return getAliasListByCallbackWithKeyboard(message, chat, user, FIRST_PAGE);
            } else if (lowerCaseCommandText.startsWith(DELETE_ALIAS_COMMAND)) {
                return deleteAliasByCallback(message, chat, user, commandText);
            } else if (lowerCaseCommandText.startsWith(ADD_ALIAS_COMMAND)) {
                return addAliasByCallback(message, chat, user);
            } else if (commandText.startsWith(SELECT_ALIAS_COMMAND)) {
                return selectAliasByCallback(message, chat, user, commandText);
            }
        }

        User user = new User().setUserId(message.getFrom().getId());
        if (lowerCaseCommandText.equals(EMPTY_ALIAS_COMMAND)) {
            return getAliasListWithKeyboard(message, chat, user);
        } else if (lowerCaseCommandText.startsWith(DELETE_ALIAS_COMMAND)) {
            return deleteAlias(message, chat, user, commandText);
        } else if (lowerCaseCommandText.startsWith(ADD_ALIAS_COMMAND)) {
            return addAlias(message, chat, user, commandText);
        } else {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }

    private EditMessageText getAliasListByCallbackWithKeyboard(Message message, Chat chat, User user, int page) {
        log.debug("Request to list all aliases for chat {} and user {}, page: {}", chat.getChatId(), user.getUserId(), page);
        Page<Alias> aliasList = aliasService.getByChatAndUser(chat, user, page);

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.enableHtml(true);
        editMessageText.setText(prepareTextOfListAliases(aliasList));
        editMessageText.setReplyMarkup(prepareKeyboardWithAliasesForDelete(aliasList, page));

        return editMessageText;
    }

    private SendMessage getAliasListWithKeyboard(Message message, Chat chat, User user) {
        log.debug("Request to list all aliases for chat {} and user {}", chat.getChatId(), user.getUserId());
        Page<Alias> aliasList = aliasService.getByChatAndUser(chat, user, FIRST_PAGE);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.enableHtml(true);
        sendMessage.setText(prepareTextOfListAliases(aliasList));
        sendMessage.setReplyMarkup(prepareKeyboardWithAliasesForDelete(aliasList, FIRST_PAGE));

        return sendMessage;
    }

    private String prepareTextOfListAliases(Page<Alias> aliasList) {
        final StringBuilder buf = new StringBuilder();
        buf.append("<b>Список твоих алиасов:</b>\n");

        aliasList.forEach(alias -> buf
                .append(alias.getId()).append(". ")
                .append(alias.getName()).append("\n"));

        return buf.toString();
    }

    private InlineKeyboardMarkup prepareKeyboardWithAliasesForDelete(Page<Alias> aliasList, int page) {
        List<List<InlineKeyboardButton>> rows = aliasList.stream().map(alias -> {
            List<InlineKeyboardButton> aliasesRow = new ArrayList<>();

            InlineKeyboardButton aliasButton = new InlineKeyboardButton();
            aliasButton.setText(Emoji.DELETE.getEmoji() + alias.getName() + " — " + alias.getValue());
            aliasButton.setCallbackData(CALLBACK_DELETE_ALIAS_COMMAND + " " + alias.getId());

            aliasesRow.add(aliasButton);

            return aliasesRow;
        }).collect(Collectors.toList());

        addingMainRows(rows, CALLBACK_DELETE_ALIAS_COMMAND, page, aliasList.getTotalPages());

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }

    private EditMessageText deleteAliasByCallback(Message message, Chat chat, User user, String command) {
        String selectPageCommand = DELETE_ALIAS_COMMAND + SELECT_PAGE;
        if (command.startsWith(selectPageCommand)) {
            int page = Integer.parseInt(command.substring(selectPageCommand.length()));
            return getAliasListByCallbackWithKeyboard(message, chat, user, page);
        }

        log.debug("Request to delete alias");

        aliasService.remove(chat, user, Long.valueOf(command.substring(DELETE_ALIAS_COMMAND.length() + 1)));

        return getAliasListByCallbackWithKeyboard(message, chat, user, FIRST_PAGE);
    }

    private EditMessageText addAliasByCallback(Message message, Chat chat, User user) {
        commandWaitingService.add(chat, user, Set.class, CALLBACK_ADD_ALIAS_COMMAND);

        Page<Alias> aliasList = aliasService.getByChatAndUser(chat, user, FIRST_PAGE);

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.enableHtml(true);
        editMessageText.setReplyMarkup(prepareKeyboardWithAliasesForDelete(aliasList, FIRST_PAGE));
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

    private PartialBotApiMethod<?> selectAliasByCallback(Message message, Chat chat, User user, String command) {
        if (!command.startsWith(SELECT_ALIAS_COMMAND + SELECT_PAGE)) {
            Long aliasId = Long.parseLong(command.substring(SELECT_ALIAS_COMMAND.length() + 1));

            Alias alias = aliasService.get(aliasId);

            if (aliasService.get(chat, user, alias.getName()) != null) {
                return buildSendMessageWithText(message, speechService.getRandomMessageByTag(BotSpeechTag.DUPLICATE_ENTRY));
            }

            aliasService.save(new Alias()
                    .setChat(chat)
                    .setUser(user)
                    .setName(alias.getName())
                    .setValue(alias.getValue()));

            return getAliasListByCallbackWithKeyboard(message, chat, user, FIRST_PAGE);
        }

        int page = Integer.parseInt(command.substring((SELECT_ALIAS_COMMAND + SELECT_PAGE).length()));
        Page<Alias> chatAliasList = aliasService.getByChat(chat, page);
        List<Alias> userAliasList = aliasService.getByChatAndUser(chat, user);

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.enableHtml(true);
        editMessageText.setText("<b>Список алиасов данной группы:</b>");
        editMessageText.setReplyMarkup(prepareKeyboardWithAliasesForSelect(chatAliasList, userAliasList, page));

        return editMessageText;
    }

    private PartialBotApiMethod<?> addAlias(Message message, Chat chat, User user, String command) {
        log.debug("Request to add new alias");

        if (command.equals(ADD_ALIAS_COMMAND)) {
            Page<Alias> allNewsInChat = aliasService.getByChatAndUser(chat, user, FIRST_PAGE);

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId().toString());
            sendMessage.enableHtml(true);
            sendMessage.setReplyMarkup(prepareKeyboardWithAliasesForDelete(allNewsInChat, FIRST_PAGE));
            sendMessage.setText(prepareTextOfListAliases(allNewsInChat) + ADDING_ALIAS_TEXT);

            return sendMessage;
        }

        String params = command.substring(ADD_ALIAS_COMMAND.length() + 1);

        int i = params.indexOf(" ");
        if (i < 0) {
            commandWaitingService.remove(chat, user);
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
            return buildSendMessageWithText(message, speechService.getRandomMessageByTag(BotSpeechTag.DUPLICATE_ENTRY));
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

    private InlineKeyboardMarkup prepareKeyboardWithAliasesForSelect(Page<Alias> chatAliasList, List<Alias> userAliasList, int page) {
        List<List<InlineKeyboardButton>> rows = chatAliasList
                .stream()
                .map(alias -> {
                    List<InlineKeyboardButton> aliasesRow = new ArrayList<>();
                    InlineKeyboardButton aliasButton = new InlineKeyboardButton();
                    String buttonText;

                    if (containsAliasByNameAndValue(userAliasList, alias)) {
                        buttonText = alias.getName() + " — " + alias.getValue();
                    } else {
                        buttonText = Emoji.CHECK_MARK_BUTTON.getEmoji() + alias.getName() + " — " + alias.getValue();
                    }

                    aliasButton.setText(buttonText);
                    aliasButton.setCallbackData(CALLBACK_SELECT_ALIAS_COMMAND + " " + alias.getId());
                    aliasesRow.add(aliasButton);

                    return aliasesRow;
                })
                .collect(Collectors.toList());

        addingMainRows(rows, CALLBACK_SELECT_ALIAS_COMMAND, page, chatAliasList.getTotalPages());

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }

    private boolean containsAliasByNameAndValue(List<Alias> aliasList, Alias desiredAlias) {
        return aliasList
                .stream()
                .anyMatch(alias -> alias.getName().equalsIgnoreCase(desiredAlias.getName()) && alias.getValue().equalsIgnoreCase(desiredAlias.getValue()));
    }

    private void addingMainRows(List<List<InlineKeyboardButton>> rows, String callbackCommand, int page, int totalPages) {
        List<InlineKeyboardButton> pagesRow = new ArrayList<>();
        String CALLBACK_SELECT_PAGE_ALIAS_LIST = callbackCommand + SELECT_PAGE;
        if (page > 0) {
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText(Emoji.LEFT_ARROW.getEmoji() + "Назад");
            backButton.setCallbackData(CALLBACK_SELECT_PAGE_ALIAS_LIST + (page - 1));

            pagesRow.add(backButton);
        }

        if (page + 1 < totalPages) {
            InlineKeyboardButton forwardButton = new InlineKeyboardButton();
            forwardButton.setText("Вперёд" + Emoji.RIGHT_ARROW.getEmoji());
            forwardButton.setCallbackData(CALLBACK_SELECT_PAGE_ALIAS_LIST + (page + 1));

            pagesRow.add(forwardButton);
        }

        rows.add(pagesRow);

        List<InlineKeyboardButton> addButtonRow = new ArrayList<>();
        InlineKeyboardButton addButton = new InlineKeyboardButton();
        addButton.setText(Emoji.NEW.getEmoji() + "Добавить");
        addButton.setCallbackData(CALLBACK_ADD_ALIAS_COMMAND);
        addButtonRow.add(addButton);

        InlineKeyboardButton selectButton = new InlineKeyboardButton();
        selectButton.setText(Emoji.RIGHT_ARROW_CURVING_UP.getEmoji() + "Выбрать");
        selectButton.setCallbackData(CALLBACK_SELECT_ALIAS_COMMAND + SELECT_PAGE + FIRST_PAGE);
        addButtonRow.add(selectButton);

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
