package org.telegram.bot.commands.setters;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.telegram.bot.commands.Set;
import org.telegram.bot.domain.entities.Alias;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandWaiting;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.enums.AccessLevel;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.Emoji;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;
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
public class AliasSetter implements Setter<PartialBotApiMethod<?>> {

    private static final String CALLBACK_COMMAND = "${setter.command} ";
    private static final String SELECT_PAGE = " ${setter.alias.selectpage}";
    private static final String EMPTY_ALIAS_COMMAND = "${setter.alias.emptycommand}";
    private static final String UPDATE_ALIAS_COMMAND = EMPTY_ALIAS_COMMAND + " ${setter.alias.update}";
    private static final String DELETE_ALIAS_COMMAND = EMPTY_ALIAS_COMMAND + " ${setter.alias.remove}";
    private static final String SELECT_ALIAS_COMMAND = EMPTY_ALIAS_COMMAND + " ${setter.alias.select}";
    private static final String CALLBACK_DELETE_ALIAS_COMMAND = CALLBACK_COMMAND + DELETE_ALIAS_COMMAND;
    private static final String CALLBACK_SELECT_ALIAS_COMMAND = CALLBACK_COMMAND + SELECT_ALIAS_COMMAND;
    private static final String ADD_ALIAS_COMMAND = EMPTY_ALIAS_COMMAND + " ${setter.alias.add}";
    private static final String CALLBACK_ADD_ALIAS_COMMAND = CALLBACK_COMMAND + ADD_ALIAS_COMMAND;
    private static final String ADDING_ALIAS_TEXT = "${setter.alias.commandwaitingstart}";
    private static final int FIRST_PAGE = 0;

    private final java.util.Set<String> emptyAliasCommands = new HashSet<>();
    private final java.util.Set<String> updateAliasCommands = new HashSet<>();
    private final java.util.Set<String> deleteAliasCommands = new HashSet<>();
    private final java.util.Set<String> addAliasCommands = new HashSet<>();
    private final java.util.Set<String> selectAliasCommands = new HashSet<>();

    private final AliasService aliasService;
    private final SpeechService speechService;
    private final CommandWaitingService commandWaitingService;
    private final CommandPropertiesService commandPropertiesService;
    private final InternationalizationService internationalizationService;

    @PostConstruct
    private void postConstruct() {
        emptyAliasCommands.addAll(internationalizationService.getAllTranslations("setter.alias.emptycommand"));
        updateAliasCommands.addAll(internationalizationService.internationalize(UPDATE_ALIAS_COMMAND));
        deleteAliasCommands.addAll(internationalizationService.internationalize(DELETE_ALIAS_COMMAND));
        addAliasCommands.addAll(internationalizationService.internationalize(ADD_ALIAS_COMMAND));
        selectAliasCommands.addAll(internationalizationService.internationalize(SELECT_ALIAS_COMMAND));
    }

    @Override
    public boolean canProcessed(String command) {
        return emptyAliasCommands.stream().anyMatch(command::startsWith);
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

            if (emptyAliasCommands.contains(lowerCaseCommandText) || updateAliasCommands.contains(lowerCaseCommandText)) {
                return getAliasListByCallbackWithKeyboard(message, chat, user, FIRST_PAGE);
            } else if (containsStartWith(deleteAliasCommands, lowerCaseCommandText)) {
                return deleteAliasByCallback(message, chat, user, commandText);
            } else if (containsStartWith(addAliasCommands, lowerCaseCommandText)) {
                return addAliasByCallback(message, chat, user);
            } else if (containsStartWith(selectAliasCommands, lowerCaseCommandText)) {
                return selectAliasByCallback(message, chat, user, commandText);
            }
        }

        User user = new User().setUserId(message.getFrom().getId());
        if (emptyAliasCommands.contains(lowerCaseCommandText)) {
            return getAliasListWithKeyboard(message, chat, user);
        } else if (containsStartWith(deleteAliasCommands, lowerCaseCommandText)) {
            return deleteAlias(message, chat, user, commandText);
        } else if (containsStartWith(addAliasCommands, lowerCaseCommandText)) {
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
        buf.append("<b>${setter.alias.listcaption}:</b>\n");

        aliasList.forEach(alias -> buf
                .append(alias.getId()).append(". ")
                .append(alias.getName()).append("\n"));

        return buf.toString();
    }

    private InlineKeyboardMarkup prepareKeyboardWithAliasesForDelete(Page<Alias> aliasList, int page) {
        List<List<InlineKeyboardButton>> rows = aliasList.stream().map(alias -> {
            List<InlineKeyboardButton> aliasesRow = new ArrayList<>();

            InlineKeyboardButton aliasButton = new InlineKeyboardButton();
            aliasButton.setText(Emoji.DELETE.getSymbol() + alias.getName() + " — " + alias.getValue());
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
        String selectPageCommand = getStartsWith(
                internationalizationService.internationalize(DELETE_ALIAS_COMMAND + SELECT_PAGE),
                command.toLowerCase());

        if (selectPageCommand != null && command.startsWith(selectPageCommand)) {
            int page = Integer.parseInt(command.substring(selectPageCommand.length()));
            return getAliasListByCallbackWithKeyboard(message, chat, user, page);
        }

        log.debug("Request to delete alias");

        String deleteAliasCommand = getStartsWith(deleteAliasCommands, command.toLowerCase());
        if (deleteAliasCommand == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        aliasService.remove(chat, user, Long.valueOf(command.substring(deleteAliasCommand.length() + 1)));

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
        editMessageText.setText(prepareTextOfListAliases(aliasList) + "\n" + ADDING_ALIAS_TEXT);

        return editMessageText;
    }

    private SendMessage deleteAlias(Message message, Chat chat, User user, String command) throws BotException {
        log.debug("Request to delete alias");

        String deleteAliasCommand = getStartsWith(deleteAliasCommands, command.toLowerCase());
        if (deleteAliasCommand == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        String params;
        try {
            params = command.substring(deleteAliasCommand.length() + 1);
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
        String selectPageCommand = getStartsWith(
                internationalizationService.internationalize(SELECT_ALIAS_COMMAND + SELECT_PAGE),
                command.toLowerCase());

        if (selectPageCommand != null && command.startsWith(selectPageCommand)) {
            int page = Integer.parseInt(command.substring((selectPageCommand).length()));
            Page<Alias> chatAliasList = aliasService.getByChat(chat, page);
            List<Alias> userAliasList = aliasService.getByChatAndUser(chat, user);

            EditMessageText editMessageText = new EditMessageText();
            editMessageText.setChatId(message.getChatId().toString());
            editMessageText.setMessageId(message.getMessageId());
            editMessageText.enableHtml(true);
            editMessageText.setText("<b>${setter.alias.chatlistcaption}:</b>");
            editMessageText.setReplyMarkup(prepareKeyboardWithAliasesForSelect(chatAliasList, userAliasList, page));

            return editMessageText;
        } else {
            String selectAliasCommand = getStartsWith(internationalizationService.internationalize(SELECT_ALIAS_COMMAND), command.toLowerCase());
            if (selectAliasCommand == null) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
            }

            Long aliasId = Long.parseLong(command.substring(selectAliasCommand.length() + 1));

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
    }

    private PartialBotApiMethod<?> addAlias(Message message, Chat chat, User user, String command) {
        log.debug("Request to add new alias");

        String addAliasCommand = getStartsWith(addAliasCommands, command.toLowerCase());
        if (addAliasCommand == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        if (addAliasCommand.contains(command.toLowerCase())) {
            Page<Alias> allNewsInChat = aliasService.getByChatAndUser(chat, user, FIRST_PAGE);

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId().toString());
            sendMessage.enableHtml(true);
            sendMessage.setReplyMarkup(prepareKeyboardWithAliasesForDelete(allNewsInChat, FIRST_PAGE));
            sendMessage.setText(prepareTextOfListAliases(allNewsInChat) + "\n" + ADDING_ALIAS_TEXT);

            return sendMessage;
        }

        String params = command.substring(addAliasCommand.length() + 1);

        int i = params.indexOf(" ");
        if (i < 0) {
            commandWaitingService.remove(chat, user);
            return buildSendMessageWithText(message, speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
        String aliasName = params.substring(0, i).toLowerCase(Locale.ROOT);
        String aliasValue = params.substring(i + 1);

        validateAliasValue(aliasValue);

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

    private void validateAliasValue(String aliasValue) {
        if (aliasValue.startsWith("{")) {
            String[] values = aliasValue.substring(1, aliasValue.length() - 1).split(";");
            if (values.length > org.telegram.bot.commands.Alias.MAX_COMMANDS_IN_ALIAS) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }
        }
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
                        buttonText = Emoji.CHECK_MARK_BUTTON.getSymbol() + alias.getName() + " — " + alias.getValue();
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
        String callbackSelectPageAliasList = callbackCommand + SELECT_PAGE;
        if (page > 0) {
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText(Emoji.LEFT_ARROW.getSymbol() + "${setter.alias.button.back}");
            backButton.setCallbackData(callbackSelectPageAliasList + (page - 1));

            pagesRow.add(backButton);
        }

        if (page + 1 < totalPages) {
            InlineKeyboardButton forwardButton = new InlineKeyboardButton();
            forwardButton.setText("${setter.alias.button.forward}" + Emoji.RIGHT_ARROW.getSymbol());
            forwardButton.setCallbackData(callbackSelectPageAliasList + (page + 1));

            pagesRow.add(forwardButton);
        }

        rows.add(pagesRow);

        List<InlineKeyboardButton> addButtonRow = new ArrayList<>();
        InlineKeyboardButton addButton = new InlineKeyboardButton();
        addButton.setText(Emoji.NEW.getSymbol() + "${setter.alias.button.add}");
        addButton.setCallbackData(CALLBACK_ADD_ALIAS_COMMAND);
        addButtonRow.add(addButton);

        InlineKeyboardButton selectButton = new InlineKeyboardButton();
        selectButton.setText(Emoji.RIGHT_ARROW_CURVING_UP.getSymbol() + "${setter.alias.button.select}");
        selectButton.setCallbackData(CALLBACK_SELECT_ALIAS_COMMAND + SELECT_PAGE + FIRST_PAGE);
        addButtonRow.add(selectButton);

        List<InlineKeyboardButton> updateButtonRow = new ArrayList<>();
        InlineKeyboardButton updateButton = new InlineKeyboardButton();
        updateButton.setText(Emoji.UPDATE.getSymbol() + "${setter.alias.button.update}");
        updateButton.setCallbackData(CALLBACK_COMMAND + UPDATE_ALIAS_COMMAND);
        updateButtonRow.add(updateButton);

        List<InlineKeyboardButton> backButtonRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(Emoji.BACK.getSymbol() + "${setter.alias.button.settings}");
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
