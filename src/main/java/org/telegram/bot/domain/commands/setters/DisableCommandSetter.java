package org.telegram.bot.domain.commands.setters;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.entities.CommandWaiting;
import org.telegram.bot.domain.entities.DisableCommand;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.domain.enums.Emoji;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.ChatService;
import org.telegram.bot.services.CommandPropertiesService;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.DisableCommandService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserService;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class DisableCommandSetter implements SetterParent<PartialBotApiMethod<?>> {

    private final ChatService chatService;
    private final UserService userService;
    private final DisableCommandService disableCommandService;
    private final CommandPropertiesService commandPropertiesService;
    private final SpeechService speechService;
    private final CommandWaitingService commandWaitingService;

    private final String CALLBACK_COMMAND = "установить ";
    private final String UPDATE_COMMAND_LIST = "команда обновить";
    private final String DISABLE_COMMAND = "команда выкл";
    private final String CALLBACK_DISABLE_COMMAND = CALLBACK_COMMAND + DISABLE_COMMAND;
    private final String SELECT_PAGE_COMMAND_LIST = DISABLE_COMMAND + " page";
    private final String ENABLE_COMMAND = "команда вкл";
    private final String CALLBACK_ENABLE_COMMAND = CALLBACK_COMMAND + ENABLE_COMMAND;
    private final String SET_COMMAND_NAME = "set";

    @Override
    public PartialBotApiMethod<?> set(Update update, String commandText) {
        Message message = getMessageFromUpdate(update);
        Chat chat = chatService.get(message.getChatId());
        String lowerCaseCommandText = commandText.toLowerCase();

        String EMPTY_CITY_COMMAND = "команда";
        if (update.hasCallbackQuery()) {
            if (lowerCaseCommandText.equals(EMPTY_CITY_COMMAND) || lowerCaseCommandText.equals(UPDATE_COMMAND_LIST)) {
                return getDisabledCommandListWithKeyboard(message, chat, false);
            } else if (lowerCaseCommandText.startsWith(DISABLE_COMMAND)) {
                return disableCommandByCallback(message, chat, commandText);
            } else if (lowerCaseCommandText.startsWith(ENABLE_COMMAND)) {
                return getKeyboardWithEnablingCommands(message, chat, commandText);
            }
        }

        if (lowerCaseCommandText.equals(EMPTY_CITY_COMMAND)) {
            return getDisabledCommandListWithKeyboard(message,  chat, true);
        } else if (lowerCaseCommandText.startsWith(ENABLE_COMMAND)) {
            return enableCommand(message, chat, commandText);
        } else if (lowerCaseCommandText.startsWith(DISABLE_COMMAND)) {
            return disableCommand(message, chat, commandText);
        } else {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }

    private PartialBotApiMethod<?> getDisabledCommandListWithKeyboard(Message message, Chat chat, Boolean newMessage) {
        log.debug("Request to list of disabled commands for chat {}", chat);
        List<DisableCommand> disableCommandList = disableCommandService.getByChat(chat);

        if (newMessage) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId().toString());
            sendMessage.enableHtml(true);
            sendMessage.setText(prepareTextOfDisableCommandList(disableCommandList));
            sendMessage.setReplyMarkup(prepareKeyboardWithMainButtons());

            return sendMessage;
        }

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.enableHtml(true);
        editMessageText.setText(prepareTextOfDisableCommandList(disableCommandList));
        editMessageText.setReplyMarkup(prepareKeyboardWithMainButtons());

        return editMessageText;
    }

    private EditMessageText disableCommandByCallback(Message message, Chat chat, String command) throws BotException {
        command = command.replace(CALLBACK_COMMAND, "");

        if (command.equals(DISABLE_COMMAND)) {
            return getKeyboardWithDisablingCommands(message, 0);
        } else if (command.startsWith(SELECT_PAGE_COMMAND_LIST)) {
            int page = Integer.parseInt(command.substring(SELECT_PAGE_COMMAND_LIST.length()));
            return getKeyboardWithDisablingCommands(message, page);
        }

        long commandPropertiesId;
        try {
            commandPropertiesId = Long.parseLong(command.substring(DISABLE_COMMAND.length() + 1));
        } catch (Exception e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        CommandProperties commandProperties = commandPropertiesService.getCommand(commandPropertiesId);
        if (commandProperties == null || SET_COMMAND_NAME.equals(commandProperties.getCommandName())) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        DisableCommand disableCommand = disableCommandService.get(chat, commandProperties);
        if (disableCommand == null) {
            disableCommand = new DisableCommand().setChat(chat).setCommandProperties(commandProperties);
        }

        disableCommandService.save(disableCommand);

        return (EditMessageText) getDisabledCommandListWithKeyboard(message, chat, false);
    }

    private EditMessageText getKeyboardWithEnablingCommands(Message message, Chat chat, String commandText) throws BotException {
        if (!ENABLE_COMMAND.equals(commandText)) {
            enableCommand(message, chat, commandText);
        }

        List<DisableCommand> disableCommandList = disableCommandService.getByChat(chat);

        List<List<InlineKeyboardButton>> commandPropertyRows = disableCommandList
                .stream()
                .map(disableCommand -> {
                    List<InlineKeyboardButton> disableCommandRow = new ArrayList<>();

                    InlineKeyboardButton disableCommandButton = new InlineKeyboardButton();
                    disableCommandButton.setText(Emoji.CHECK_MARK.getEmoji() + disableCommand.getCommandProperties().getRussifiedName());
                    disableCommandButton.setCallbackData(CALLBACK_ENABLE_COMMAND + " " + disableCommand.getId());

                    disableCommandRow.add(disableCommandButton);

                    return disableCommandRow;
                })
                .collect(Collectors.toList());

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(addingMainRows(commandPropertyRows));

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.enableMarkdown(true);
        editMessageText.setText("<b>Список отключенных команд</b>\n");
        editMessageText.setReplyMarkup(inlineKeyboardMarkup);

        return editMessageText;
    }

    private SendMessage enableCommand(Message message, Chat chat, String command) throws BotException {
        log.debug("Request to enable command");

        String params;
        try {
            params = command.substring(ENABLE_COMMAND.length() + 1);
        } catch (Exception e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        DisableCommand disableCommand;
        try {
            disableCommand = disableCommandService.get(Long.parseLong(params));
        } catch (Exception e) {
            String commandName = params.toLowerCase(Locale.ROOT);
            disableCommand = disableCommandService.getByChat(chat)
                    .stream()
                    .filter(disableCommand1 -> disableCommand1.getCommandProperties().getRussifiedName().toLowerCase(Locale.ROOT).equals(commandName))
                    .findFirst()
                    .orElse(null);

            if (disableCommand == null) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }
        }

        disableCommandService.remove(disableCommand);

        return buildSendMessageWithText(message, speechService.getRandomMessageByTag(BotSpeechTag.SAVED));
    }

    private PartialBotApiMethod<?> disableCommand(Message message, Chat chat, String command) throws BotException {
        log.debug("Request to disable command");
        if (command.equals(DISABLE_COMMAND)) {
            return disableCommandByCallback(message, chat, DISABLE_COMMAND);
        }

        String param = command.substring(DISABLE_COMMAND.length() + 1);

        CommandProperties commandProperties = commandPropertiesService.getCommand(param.toLowerCase(Locale.ROOT));
        if (commandProperties == null || SET_COMMAND_NAME.equals(commandProperties.getCommandName())) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        DisableCommand disableCommand = disableCommandService.get(chat, commandProperties);
        if (disableCommand != null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        disableCommand = new DisableCommand().setChat(chat).setCommandProperties(commandProperties);
        disableCommandService.save(disableCommand);

        CommandWaiting commandWaiting = commandWaitingService.get(chat, userService.get(message.getFrom().getId()));
        if (commandWaiting != null && commandWaiting.getCommandName().equals("set")) {
            commandWaitingService.remove(commandWaiting);
        }

        return buildSendMessageWithText(message, speechService.getRandomMessageByTag(BotSpeechTag.SAVED));
    }

    private String prepareTextOfDisableCommandList(List<DisableCommand> disableCommandList) {
        final StringBuilder buf = new StringBuilder();
        buf.append("<b>Список отключеных команд:</b>\n");

        disableCommandList.forEach(disableCommand -> buf
                .append(disableCommand.getId()).append(". ")
                .append(disableCommand.getCommandProperties().getRussifiedName()).append("\n"));

        return buf.toString();
    }

    private EditMessageText getKeyboardWithDisablingCommands(Message message, int page) {
        Page<CommandProperties> commandPropertiesList = commandPropertiesService.getAll(page);

        List<List<InlineKeyboardButton>> commandPropertyRows = commandPropertiesList
                .stream()
                .filter(commandProperties -> !SET_COMMAND_NAME.equals(commandProperties.getCommandName()))
                .map(commandProperties -> {
                    List<InlineKeyboardButton> commandPropertyRow = new ArrayList<>();

                    InlineKeyboardButton commandProperty = new InlineKeyboardButton();
                    commandProperty.setText(commandProperties.getRussifiedName());
                    commandProperty.setCallbackData(CALLBACK_DISABLE_COMMAND + " " + commandProperties.getId());

                    commandPropertyRow.add(commandProperty);

                    return commandPropertyRow;
                })
                .collect(Collectors.toList());

        List<InlineKeyboardButton> pagesRow = new ArrayList<>();
        String CALLBACK_SELECT_PAGE_COMMAND_LIST = CALLBACK_COMMAND + SELECT_PAGE_COMMAND_LIST;
        if (page > 0) {
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText(Emoji.LEFT_ARROW.getEmoji() + "Назад");
            backButton.setCallbackData(CALLBACK_SELECT_PAGE_COMMAND_LIST + (page - 1));

            pagesRow.add(backButton);
        }

        if (page < commandPropertiesList.getTotalPages()) {
            InlineKeyboardButton forwardButton = new InlineKeyboardButton();
            forwardButton.setText("Вперёд" + Emoji.RIGHT_ARROW.getEmoji());
            forwardButton.setCallbackData(CALLBACK_SELECT_PAGE_COMMAND_LIST + (page + 1));

            pagesRow.add(forwardButton);
        }

        commandPropertyRows.add(pagesRow);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(addingMainRows(commandPropertyRows));

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.enableMarkdown(true);
        editMessageText.setText("Выбери команду из списка");
        editMessageText.setReplyMarkup(inlineKeyboardMarkup);

        return editMessageText;
    }

    private InlineKeyboardMarkup prepareKeyboardWithMainButtons() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(addingMainRows(new ArrayList<>()));

        return inlineKeyboardMarkup;
    }

    private List<List<InlineKeyboardButton>> addingMainRows(List<List<InlineKeyboardButton>> rows) {

        List<InlineKeyboardButton> deleteButtonRow = new ArrayList<>();
        InlineKeyboardButton deleteButton = new InlineKeyboardButton();
        deleteButton.setText(Emoji.CHECK_MARK.getEmoji() + "Включить");
        deleteButton.setCallbackData(CALLBACK_ENABLE_COMMAND);
        deleteButtonRow.add(deleteButton);

        List<InlineKeyboardButton> selectButtonRow = new ArrayList<>();
        InlineKeyboardButton selectButton = new InlineKeyboardButton();
        selectButton.setText(Emoji.NO_ENTRY_SIGN.getEmoji() + "Отключить");
        selectButton.setCallbackData(CALLBACK_DISABLE_COMMAND);
        selectButtonRow.add(selectButton);

        List<InlineKeyboardButton> updateButtonRow = new ArrayList<>();
        InlineKeyboardButton updateButton = new InlineKeyboardButton();
        updateButton.setText(Emoji.UPDATE.getEmoji() + "Обновить");
        updateButton.setCallbackData(CALLBACK_COMMAND + UPDATE_COMMAND_LIST);
        updateButtonRow.add(updateButton);

        List<InlineKeyboardButton> backButtonRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(Emoji.BACK.getEmoji() + "Установки");
        backButton.setCallbackData(CALLBACK_COMMAND + "back");
        backButtonRow.add(backButton);

        rows.add(selectButtonRow);
        rows.add(deleteButtonRow);
        rows.add(updateButtonRow);
        rows.add(backButtonRow);

        return rows;
    }

    private SendMessage buildSendMessageWithText(Message message, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.enableMarkdown(true);
        sendMessage.setText(text);

        return sendMessage;
    }
}
