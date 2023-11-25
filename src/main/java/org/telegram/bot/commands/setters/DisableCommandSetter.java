package org.telegram.bot.commands.setters;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.entities.CommandWaiting;
import org.telegram.bot.domain.entities.DisableCommand;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class DisableCommandSetter implements Setter<PartialBotApiMethod<?>> {

    private static final String CALLBACK_COMMAND = "${setter.command} ";
    private static final String EMPTY_COMMAND = "${setter.disablecommand.emptycommand}";
    private static final String UPDATE_COMMAND_LIST = EMPTY_COMMAND + " ${setter.disablecommand.update}";
    private static final String DISABLE_COMMAND = EMPTY_COMMAND + " ${setter.disablecommand.disable}";
    private static final String CALLBACK_DISABLE_COMMAND = CALLBACK_COMMAND + DISABLE_COMMAND;
    private static final String SELECT_PAGE_COMMAND_LIST = DISABLE_COMMAND + " page";
    private static final String ENABLE_COMMAND = EMPTY_COMMAND + " ${setter.disablecommand.enable}";
    private static final String CALLBACK_ENABLE_COMMAND = CALLBACK_COMMAND + ENABLE_COMMAND;
    private static final String SET_COMMAND_NAME = "set";
    private static final String CALLBACK_SELECT_PAGE_COMMAND_LIST = CALLBACK_COMMAND + SELECT_PAGE_COMMAND_LIST;

    private final Set<String> emptyDisableCommands = new HashSet<>();
    private final Set<String> updateDisableCommands = new HashSet<>();
    private final Set<String> disableCommands = new HashSet<>();
    private final Set<String> enableCommands = new HashSet<>();


    private final DisableCommandService disableCommandService;
    private final CommandPropertiesService commandPropertiesService;
    private final SpeechService speechService;
    private final CommandWaitingService commandWaitingService;
    private final InternationalizationService internationalizationService;

    @PostConstruct
    private void postConstruct() {
        emptyDisableCommands.addAll(internationalizationService.getAllTranslations("setter.disablecommand.emptycommand"));
        updateDisableCommands.addAll(internationalizationService.internationalize(UPDATE_COMMAND_LIST));
        disableCommands.addAll(internationalizationService.internationalize(DISABLE_COMMAND));
        enableCommands.addAll(internationalizationService.internationalize(ENABLE_COMMAND));
    }

    @Override
    public boolean canProcessed(String command) {
        return emptyDisableCommands.stream().anyMatch(command::startsWith);
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
            if (emptyDisableCommands.contains(lowerCaseCommandText) || updateDisableCommands.contains(lowerCaseCommandText)) {
                return getDisabledCommandListWithKeyboard(message, chat, false);
            } else if (containsStartWith(disableCommands, lowerCaseCommandText)) {
                return disableCommandByCallback(message, chat, commandText);
            } else if (containsStartWith(enableCommands, lowerCaseCommandText)) {
                return getKeyboardWithEnablingCommands(message, chat, commandText);
            }
        }

        if (emptyDisableCommands.contains(lowerCaseCommandText)) {
            return getDisabledCommandListWithKeyboard(message,  chat, true);
        } else if (containsStartWith(enableCommands, lowerCaseCommandText)) {
            return enableCommand(message, chat, commandText);
        } else if (containsStartWith(disableCommands, lowerCaseCommandText)) {
            return disableCommand(message, chat, commandText);
        } else {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }

    private PartialBotApiMethod<?> getDisabledCommandListWithKeyboard(Message message, Chat chat, boolean newMessage) {
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
        String callbackCommand = getStartsWith(
                internationalizationService.internationalize(CALLBACK_COMMAND),
                command.toLowerCase());

        if (callbackCommand != null) {
            command = command.replace(callbackCommand, "");
        }

        String disableCommand = getLocalizedCommand(command, DISABLE_COMMAND);
        if (command.equals(disableCommand)) {
            return getKeyboardWithDisablingCommands(message, 0);
        }

        String selectPageCommand = getStartsWith(
                internationalizationService.internationalize(SELECT_PAGE_COMMAND_LIST),
                command.toLowerCase());
        if (selectPageCommand != null && command.startsWith(selectPageCommand)) {
            int page = Integer.parseInt(command.substring(selectPageCommand.length()));
            return getKeyboardWithDisablingCommands(message, page);
        }

        long commandPropertiesId;
        try {
            commandPropertiesId = Long.parseLong(command.substring(disableCommand.length() + 1));
        } catch (Exception e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        CommandProperties commandProperties = commandPropertiesService.getCommand(commandPropertiesId);
        if (commandProperties == null || SET_COMMAND_NAME.equals(commandProperties.getCommandName())) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        DisableCommand disableCommandEntity = disableCommandService.get(chat, commandProperties);
        if (disableCommandEntity == null) {
            disableCommandEntity = new DisableCommand().setChat(chat).setCommandProperties(commandProperties);
        }

        disableCommandService.save(disableCommandEntity);

        return (EditMessageText) getDisabledCommandListWithKeyboard(message, chat, false);
    }

    private EditMessageText getKeyboardWithEnablingCommands(Message message, Chat chat, String commandText) throws BotException {
        String enableCommand = getLocalizedCommand(commandText, ENABLE_COMMAND);

        if (!enableCommand.equals(commandText)) {
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
        editMessageText.enableHtml(true);
        editMessageText.setText("<b>${setter.disablecommand.caption}</b>\n");
        editMessageText.setReplyMarkup(inlineKeyboardMarkup);

        return editMessageText;
    }

    private SendMessage enableCommand(Message message, Chat chat, String command) throws BotException {
        log.debug("Request to enable command");

        String enableCommand = getLocalizedCommand(command, ENABLE_COMMAND);

        String params;
        try {
            params = command.substring(enableCommand.length() + 1);
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
        String disableCommand = getLocalizedCommand(command, DISABLE_COMMAND);

        log.debug("Request to disable command");
        if (command.equals(disableCommand)) {
            return disableCommandByCallback(message, chat, DISABLE_COMMAND);
        }

        String param = command.substring(disableCommand.length() + 1);

        CommandProperties commandProperties = commandPropertiesService.getCommand(param.toLowerCase(Locale.ROOT));
        if (commandProperties == null || SET_COMMAND_NAME.equals(commandProperties.getCommandName())) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        DisableCommand disableCommandEntity = disableCommandService.get(chat, commandProperties);
        if (disableCommandEntity != null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        disableCommandEntity = new DisableCommand().setChat(chat).setCommandProperties(commandProperties);
        disableCommandService.save(disableCommandEntity);

        CommandWaiting commandWaiting = commandWaitingService.get(chat, new User().setUserId(message.getFrom().getId()));
        if (commandWaiting != null && commandWaiting.getCommandName().equals("set")) {
            commandWaitingService.remove(commandWaiting);
        }

        return buildSendMessageWithText(message, speechService.getRandomMessageByTag(BotSpeechTag.SAVED));
    }

    private String prepareTextOfDisableCommandList(List<DisableCommand> disableCommandList) {
        final StringBuilder buf = new StringBuilder();
        buf.append("<b>${setter.disablecommand.caption}:</b>\n");

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
        if (page > 0) {
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText(Emoji.LEFT_ARROW.getEmoji() + "${setter.disablecommand.button.back}");
            backButton.setCallbackData(CALLBACK_SELECT_PAGE_COMMAND_LIST + (page - 1));

            pagesRow.add(backButton);
        }

        if (page < commandPropertiesList.getTotalPages()) {
            InlineKeyboardButton forwardButton = new InlineKeyboardButton();
            forwardButton.setText("${setter.disablecommand.button.forward}" + Emoji.RIGHT_ARROW.getEmoji());
            forwardButton.setCallbackData(CALLBACK_SELECT_PAGE_COMMAND_LIST + (page + 1));

            pagesRow.add(forwardButton);
        }

        commandPropertyRows.add(pagesRow);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(addingMainRows(commandPropertyRows));

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.enableHtml(true);
        editMessageText.setText("${setter.disablecommand.selectcommand}");
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
        deleteButton.setText(Emoji.CHECK_MARK.getEmoji() + "${setter.disablecommand.button.enable}");
        deleteButton.setCallbackData(CALLBACK_ENABLE_COMMAND);
        deleteButtonRow.add(deleteButton);

        List<InlineKeyboardButton> selectButtonRow = new ArrayList<>();
        InlineKeyboardButton selectButton = new InlineKeyboardButton();
        selectButton.setText(Emoji.NO_ENTRY_SIGN.getEmoji() + "${setter.disablecommand.button.disable}");
        selectButton.setCallbackData(CALLBACK_DISABLE_COMMAND);
        selectButtonRow.add(selectButton);

        List<InlineKeyboardButton> updateButtonRow = new ArrayList<>();
        InlineKeyboardButton updateButton = new InlineKeyboardButton();
        updateButton.setText(Emoji.UPDATE.getEmoji() + "${setter.disablecommand.button.update}");
        updateButton.setCallbackData(CALLBACK_COMMAND + UPDATE_COMMAND_LIST);
        updateButtonRow.add(updateButton);

        List<InlineKeyboardButton> backButtonRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(Emoji.BACK.getEmoji() + "${setter.disablecommand.button.settings}");
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
        sendMessage.enableHtml(true);
        sendMessage.setText(text);

        return sendMessage;
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
