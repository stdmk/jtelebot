package org.telegram.bot.commands.setters;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.entities.CommandWaiting;
import org.telegram.bot.domain.entities.DisableCommand;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.AccessLevel;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.Emoji;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.TextUtils.containsStartWith;
import static org.telegram.bot.utils.TextUtils.getStartsWith;

@Component
@RequiredArgsConstructor
@Slf4j
public class DisableCommandSetter implements Setter<BotResponse> {

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
    public BotResponse set(BotRequest request, String commandText) {
        Message message = request.getMessage();
        Chat chat = message.getChat();
        String lowerCaseCommandText = commandText.toLowerCase();

        if (message.isCallback()) {
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

    private BotResponse getDisabledCommandListWithKeyboard(Message message, Chat chat, boolean newMessage) {
        log.debug("Request to list of disabled commands for chat {}", chat);
        List<DisableCommand> disableCommandList = disableCommandService.getByChat(chat);

        if (newMessage) {
            return new TextResponse(message)
                    .setText(prepareTextOfDisableCommandList(disableCommandList))
                    .setKeyboard(prepareKeyboardWithMainButtons())
                    .setResponseSettings(FormattingStyle.HTML);
        }

        return new EditResponse(message)
                .setText(prepareTextOfDisableCommandList(disableCommandList))
                .setKeyboard(prepareKeyboardWithMainButtons())
                .setResponseSettings(FormattingStyle.HTML);
    }

    private EditResponse disableCommandByCallback(Message message, Chat chat, String command) throws BotException {
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

        return (EditResponse) getDisabledCommandListWithKeyboard(message, chat, false);
    }

    private EditResponse getKeyboardWithEnablingCommands(Message message, Chat chat, String commandText) throws BotException {
        String enableCommand = getLocalizedCommand(commandText, ENABLE_COMMAND);

        if (!enableCommand.equals(commandText)) {
            enableCommand(message, chat, commandText);
        }

        List<DisableCommand> disableCommandList = disableCommandService.getByChat(chat);

        List<List<KeyboardButton>> commandPropertyRows = disableCommandList
                .stream()
                .map(disableCommand -> List.of(new KeyboardButton()
                        .setName(Emoji.CHECK_MARK.getSymbol() + disableCommand.getCommandProperties().getRussifiedName())
                        .setCallback(CALLBACK_ENABLE_COMMAND + " " + disableCommand.getId())))
                .collect(Collectors.toList());

        return new EditResponse(message)
                .setText("<b>${setter.disablecommand.caption}</b>\n")
                .setKeyboard(new Keyboard(commandPropertyRows));
    }

    private TextResponse enableCommand(Message message, Chat chat, String command) throws BotException {
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

        return new TextResponse(message)
                .setText(speechService.getRandomMessageByTag(BotSpeechTag.SAVED))
                .setResponseSettings(FormattingStyle.HTML);
    }

    private BotResponse disableCommand(Message message, Chat chat, String command) throws BotException {
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

        CommandWaiting commandWaiting = commandWaitingService.get(chat, message.getUser());
        if (commandWaiting != null && commandWaiting.getCommandName().equals("set")) {
            commandWaitingService.remove(commandWaiting);
        }

        return new TextResponse(message)
                .setText(speechService.getRandomMessageByTag(BotSpeechTag.SAVED))
                .setResponseSettings(FormattingStyle.HTML);
    }

    private String prepareTextOfDisableCommandList(List<DisableCommand> disableCommandList) {
        final StringBuilder buf = new StringBuilder();
        buf.append("<b>${setter.disablecommand.caption}:</b>\n");

        disableCommandList.forEach(disableCommand -> buf
                .append(disableCommand.getId()).append(". ")
                .append(disableCommand.getCommandProperties().getRussifiedName()).append("\n"));

        return buf.toString();
    }

    private EditResponse getKeyboardWithDisablingCommands(Message message, int page) {
        Page<CommandProperties> commandPropertiesList = commandPropertiesService.getAll(page);

        List<List<KeyboardButton>> commandPropertyRows = commandPropertiesList
                .stream()
                .filter(commandProperties -> !SET_COMMAND_NAME.equals(commandProperties.getCommandName()))
                .map(commandProperties -> List.of(
                        new KeyboardButton()
                                .setName(commandProperties.getRussifiedName())
                                .setCallback(CALLBACK_DISABLE_COMMAND + " " + commandProperties.getId())))
                .collect(Collectors.toList());

        List<KeyboardButton> pagesRow = new ArrayList<>(2);
        if (page > 0) {
            KeyboardButton backButton = new KeyboardButton();
            backButton.setName(Emoji.LEFT_ARROW.getSymbol() + "${setter.disablecommand.button.back}");
            backButton.setCallback(CALLBACK_SELECT_PAGE_COMMAND_LIST + (page - 1));

            pagesRow.add(backButton);
        }

        if (page < commandPropertiesList.getTotalPages()) {
            KeyboardButton forwardButton = new KeyboardButton();
            forwardButton.setName("${setter.disablecommand.button.forward}" + Emoji.RIGHT_ARROW.getSymbol());
            forwardButton.setCallback(CALLBACK_SELECT_PAGE_COMMAND_LIST + (page + 1));

            pagesRow.add(forwardButton);
        }

        commandPropertyRows.add(pagesRow);

        return new EditResponse(message)
                .setText("${setter.disablecommand.selectcommand}")
                .setKeyboard(new Keyboard(commandPropertyRows))
                .setResponseSettings(FormattingStyle.HTML);
    }

    private Keyboard prepareKeyboardWithMainButtons() {
        return new Keyboard(
                new KeyboardButton()
                        .setName(Emoji.CHECK_MARK.getSymbol() + "${setter.disablecommand.button.enable}")
                        .setCallback(CALLBACK_ENABLE_COMMAND),
                new KeyboardButton()
                        .setName(Emoji.NO_ENTRY_SIGN.getSymbol() + "${setter.disablecommand.button.disable}")
                        .setCallback(CALLBACK_DISABLE_COMMAND),
                new KeyboardButton()
                        .setName(Emoji.UPDATE.getSymbol() + "${setter.disablecommand.button.update}")
                        .setCallback(CALLBACK_COMMAND + UPDATE_COMMAND_LIST),
                new KeyboardButton()
                        .setName(Emoji.BACK.getSymbol() + "${setter.disablecommand.button.settings}")
                        .setCallback(CALLBACK_COMMAND + "back"));
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
