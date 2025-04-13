package org.telegram.bot.commands.setters;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.telegram.bot.commands.Set;
import org.telegram.bot.domain.entities.Alias;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.AccessLevel;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.Emoji;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.AliasService;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.InternationalizationService;
import org.telegram.bot.services.SpeechService;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.TextUtils.containsStartWith;
import static org.telegram.bot.utils.TextUtils.getStartsWith;

@Service
@RequiredArgsConstructor
@Slf4j
public class AliasSetter implements Setter<BotResponse> {

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
    public BotResponse set(BotRequest request, String commandText) {
        Message message = request.getMessage();
        Chat chat = message.getChat();
        User user = message.getUser();
        String lowerCaseCommandText = commandText.toLowerCase(Locale.ROOT);

        if (message.isCallback()) {
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

    private EditResponse getAliasListByCallbackWithKeyboard(Message message, Chat chat, User user, int page) {
        log.debug("Request to list all aliases for chat {} and user {}, page: {}", chat.getChatId(), user.getUserId(), page);
        Page<Alias> aliasList = aliasService.getByChatAndUser(chat, user, page);
        return new EditResponse(message)
                .setText(prepareTextOfListAliases(aliasList))
                .setKeyboard(prepareKeyboardWithAliasesForDelete(aliasList, page))
                .setResponseSettings(FormattingStyle.HTML);
    }

    private TextResponse getAliasListWithKeyboard(Message message, Chat chat, User user) {
        log.debug("Request to list all aliases for chat {} and user {}", chat.getChatId(), user.getUserId());
        Page<Alias> aliasList = aliasService.getByChatAndUser(chat, user, FIRST_PAGE);
        return new TextResponse(message)
                .setText(prepareTextOfListAliases(aliasList))
                .setKeyboard(prepareKeyboardWithAliasesForDelete(aliasList, FIRST_PAGE))
                .setResponseSettings(FormattingStyle.HTML);
    }

    private String prepareTextOfListAliases(Page<Alias> aliasList) {
        final StringBuilder buf = new StringBuilder();
        buf.append("<b>${setter.alias.listcaption}:</b>\n");

        aliasList.forEach(alias -> buf
                .append(alias.getId()).append(". ")
                .append(alias.getName()).append("\n"));

        return buf.toString();
    }

    private Keyboard prepareKeyboardWithAliasesForDelete(Page<Alias> aliasList, int page) {
        List<List<KeyboardButton>> rows = aliasList.stream().map(alias -> List.of(
                new KeyboardButton()
                        .setName(Emoji.DELETE.getSymbol() + alias.getName() + " — " + alias.getValue())
                        .setCallback(CALLBACK_DELETE_ALIAS_COMMAND + " " + alias.getId()))
        ).collect(Collectors.toList());

        addingMainRows(rows, CALLBACK_DELETE_ALIAS_COMMAND, page, aliasList.getTotalPages());

        return new Keyboard(rows);
    }

    private EditResponse deleteAliasByCallback(Message message, Chat chat, User user, String command) {
        String selectPageCommand = getStartsWith(
                internationalizationService.internationalize(DELETE_ALIAS_COMMAND + SELECT_PAGE),
                command.toLowerCase(Locale.ROOT));

        if (selectPageCommand != null && command.startsWith(selectPageCommand)) {
            int page = Integer.parseInt(command.substring(selectPageCommand.length()));
            return getAliasListByCallbackWithKeyboard(message, chat, user, page);
        }

        log.debug("Request to delete alias");

        String deleteAliasCommand = getStartsWith(deleteAliasCommands, command.toLowerCase(Locale.ROOT));
        if (deleteAliasCommand == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        aliasService.remove(chat, user, Long.valueOf(command.substring(deleteAliasCommand.length() + 1)));

        return getAliasListByCallbackWithKeyboard(message, chat, user, FIRST_PAGE);
    }

    private EditResponse addAliasByCallback(Message message, Chat chat, User user) {
        commandWaitingService.add(chat, user, Set.class, CALLBACK_ADD_ALIAS_COMMAND);
        Page<Alias> aliasList = aliasService.getByChatAndUser(chat, user, FIRST_PAGE);
        return new EditResponse(message)
                .setText(prepareTextOfListAliases(aliasList) + "\n" + ADDING_ALIAS_TEXT)
                .setKeyboard(prepareKeyboardWithAliasesForDelete(aliasList, FIRST_PAGE))
                .setResponseSettings(FormattingStyle.HTML);
    }

    private TextResponse deleteAlias(Message message, Chat chat, User user, String command) throws BotException {
        log.debug("Request to delete alias");

        String deleteAliasCommand = getStartsWith(deleteAliasCommands, command.toLowerCase(Locale.ROOT));
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

        return new TextResponse(message)
                .setText(responseText)
                .setResponseSettings(FormattingStyle.HTML);
    }

    private BotResponse selectAliasByCallback(Message message, Chat chat, User user, String command) {
        String selectPageCommand = getStartsWith(
                internationalizationService.internationalize(SELECT_ALIAS_COMMAND + SELECT_PAGE),
                command.toLowerCase(Locale.ROOT));

        if (selectPageCommand != null && command.startsWith(selectPageCommand)) {
            int page = Integer.parseInt(command.substring((selectPageCommand).length()));
            Page<Alias> chatAliasList = aliasService.getByChat(chat, page);
            List<Alias> userAliasList = aliasService.getByChatAndUser(chat, user);
            return new EditResponse(message)
                    .setText("<b>${setter.alias.chatlistcaption}:</b>")
                    .setKeyboard(prepareKeyboardWithAliasesForSelect(chatAliasList, userAliasList, page))
                    .setResponseSettings(FormattingStyle.HTML);
        } else {
            String selectAliasCommand = getStartsWith(selectAliasCommands, command.toLowerCase(Locale.ROOT));
            if (selectAliasCommand == null) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
            }

            Long aliasId = Long.parseLong(command.substring(selectAliasCommand.length() + 1));

            Alias alias = aliasService.get(aliasId);

            if (aliasService.get(chat, user, alias.getName()) != null) {
                return new TextResponse(message)
                        .setText(speechService.getRandomMessageByTag(BotSpeechTag.DUPLICATE_ENTRY))
                        .setResponseSettings(FormattingStyle.HTML);
            }

            aliasService.save(new Alias()
                    .setChat(chat)
                    .setUser(user)
                    .setName(alias.getName())
                    .setValue(alias.getValue()));

            return getAliasListByCallbackWithKeyboard(message, chat, user, FIRST_PAGE);
        }
    }

    private BotResponse addAlias(Message message, Chat chat, User user, String command) {
        log.debug("Request to add new alias");

        String addAliasCommand = getStartsWith(addAliasCommands, command.toLowerCase(Locale.ROOT));
        if (addAliasCommand == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        if (addAliasCommand.contains(command.toLowerCase(Locale.ROOT))) {
            Page<Alias> allNewsInChat = aliasService.getByChatAndUser(chat, user, FIRST_PAGE);
            return new TextResponse(message)
                    .setText(prepareTextOfListAliases(allNewsInChat) + "\n" + ADDING_ALIAS_TEXT)
                    .setKeyboard(prepareKeyboardWithAliasesForDelete(allNewsInChat, FIRST_PAGE))
                    .setResponseSettings(FormattingStyle.HTML);
        }

        String params = command.substring(addAliasCommand.length() + 1);

        int i = params.indexOf(" ");
        if (i < 0) {
            commandWaitingService.remove(chat, user);
            return new TextResponse(message)
                    .setText(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT))
                    .setResponseSettings(FormattingStyle.HTML);
        }
        String aliasName = params.substring(0, i).toLowerCase(Locale.ROOT);
        String aliasValue = params.substring(i + 1);

        validateAliasValue(aliasValue);

        Alias alias = aliasService.get(chat, user, aliasName);
        if (alias != null) {
            return new TextResponse(message)
                    .setText(speechService.getRandomMessageByTag(BotSpeechTag.DUPLICATE_ENTRY))
                    .setResponseSettings(FormattingStyle.HTML);
        }

        aliasService.save(new Alias().setChat(chat).setUser(user).setName(aliasName).setValue(aliasValue));

        commandWaitingService.remove(chat, user);

        return new TextResponse(message)
                .setText(speechService.getRandomMessageByTag(BotSpeechTag.SAVED))
                .setResponseSettings(FormattingStyle.HTML);
    }

    private void validateAliasValue(String aliasValue) {
        if (aliasValue.startsWith("{")) {
            String[] values = aliasValue.substring(1, aliasValue.length() - 1).split(";");
            if (values.length > org.telegram.bot.commands.Alias.MAX_COMMANDS_IN_ALIAS) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }
        }
    }

    private Keyboard prepareKeyboardWithAliasesForSelect(Page<Alias> chatAliasList, List<Alias> userAliasList, int page) {
        List<List<KeyboardButton>> rows = chatAliasList
                .stream()
                .map(alias -> {
                    String buttonText;

                    if (containsAliasByNameAndValue(userAliasList, alias)) {
                        buttonText = alias.getName() + " — " + alias.getValue();
                    } else {
                        buttonText = Emoji.CHECK_MARK_BUTTON.getSymbol() + alias.getName() + " — " + alias.getValue();
                    }

                    return List.of(new KeyboardButton()
                            .setName(buttonText)
                            .setCallback(CALLBACK_SELECT_ALIAS_COMMAND + " " + alias.getId()));
                })
                .collect(Collectors.toList());

        addingMainRows(rows, CALLBACK_SELECT_ALIAS_COMMAND, page, chatAliasList.getTotalPages());

        return new Keyboard(rows);
    }

    private boolean containsAliasByNameAndValue(List<Alias> aliasList, Alias desiredAlias) {
        return aliasList
                .stream()
                .anyMatch(alias -> alias.getName().equalsIgnoreCase(desiredAlias.getName()) && alias.getValue().equalsIgnoreCase(desiredAlias.getValue()));
    }

    private void addingMainRows(List<List<KeyboardButton>> rows, String callbackCommand, int page, int totalPages) {
        List<KeyboardButton> pagesRow = new ArrayList<>(2);
        String callbackSelectPageAliasList = callbackCommand + SELECT_PAGE;
        if (page > 0) {
            KeyboardButton backButton = new KeyboardButton();
            backButton.setName(Emoji.LEFT_ARROW.getSymbol() + "${setter.alias.button.back}");
            backButton.setCallback(callbackSelectPageAliasList + (page - 1));

            pagesRow.add(backButton);
        }

        if (page + 1 < totalPages) {
            KeyboardButton forwardButton = new KeyboardButton();
            forwardButton.setName("${setter.alias.button.forward}" + Emoji.RIGHT_ARROW.getSymbol());
            forwardButton.setCallback(callbackSelectPageAliasList + (page + 1));

            pagesRow.add(forwardButton);
        }

        rows.add(pagesRow);

        rows.add(List.of(new KeyboardButton()
                .setName(Emoji.NEW.getSymbol() + "${setter.alias.button.add}")
                .setCallback(CALLBACK_ADD_ALIAS_COMMAND)));
        rows.add(List.of(new KeyboardButton()
                .setName(Emoji.RIGHT_ARROW_CURVING_UP.getSymbol() + "${setter.alias.button.select}")
                .setCallback(CALLBACK_SELECT_ALIAS_COMMAND + SELECT_PAGE + FIRST_PAGE)));
        rows.add(List.of(new KeyboardButton()
                .setName(Emoji.UPDATE.getSymbol() + "${setter.alias.button.update}")
                .setCallback(CALLBACK_COMMAND + UPDATE_ALIAS_COMMAND)));
        rows.add(List.of(new KeyboardButton()
                .setName(Emoji.BACK.getSymbol() + "${setter.alias.button.settings}")
                .setCallback(CALLBACK_COMMAND + "back")));
    }

}
