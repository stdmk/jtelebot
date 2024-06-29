package org.telegram.bot.commands.setters;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.*;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class TvSetter implements Setter<BotResponse> {

    private static final String CALLBACK_COMMAND = "${setter.command} ";
    private static final String EMPTY_CITY_COMMAND = "${setter.tv.emptycommand}";
    private static final String UPDATE_TV_COMMAND = EMPTY_CITY_COMMAND + " ${setter.tv.update}";
    private static final String SELECT_TV_COMMAND = EMPTY_CITY_COMMAND + " ${setter.tv.select}";
    private static final String CALLBACK_SELECT_TV_COMMAND = CALLBACK_COMMAND + SELECT_TV_COMMAND;
    private static final String SELECT_PAGE_TV_LIST = SELECT_TV_COMMAND + " page";
    private static final String CALLBACK_SELECT_PAGE_TV_LIST = CALLBACK_COMMAND + SELECT_PAGE_TV_LIST;
    private static final String DELETE_TV_COMMAND = EMPTY_CITY_COMMAND + " ${setter.tv.delete}";
    private static final String CALLBACK_DELETE_TV_COMMAND = CALLBACK_COMMAND + DELETE_TV_COMMAND;

    private final java.util.Set<String> emptyTvCommands = new HashSet<>();
    private final java.util.Set<String> updateTvCommands = new HashSet<>();
    private final java.util.Set<String> selectTvCommands = new HashSet<>();
    private final java.util.Set<String> deleteTvCommands = new HashSet<>();

    private final UserTvService userTvService;
    private final SpeechService speechService;
    private final TvChannelService tvChannelService;
    private final CommandWaitingService commandWaitingService;
    private final InternationalizationService internationalizationService;

    @PostConstruct
    private void postConstruct() {
        emptyTvCommands.addAll(internationalizationService.getAllTranslations("setter.tv.emptycommand"));
        updateTvCommands.addAll(internationalizationService.internationalize(UPDATE_TV_COMMAND));
        selectTvCommands.addAll(internationalizationService.internationalize(SELECT_TV_COMMAND));
        deleteTvCommands.addAll(internationalizationService.internationalize(DELETE_TV_COMMAND));
    }

    @Override
    public boolean canProcessed(String command) {
        return emptyTvCommands.stream().anyMatch(command::startsWith);
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
        String lowerCaseCommandText = commandText.toLowerCase();

        if (message.isCallback()) {
            if (emptyTvCommands.contains(lowerCaseCommandText) || updateTvCommands.contains(lowerCaseCommandText)) {
                return getUserTvListWithKeyboard(message, chat, user, false);
            } else if (containsStartWith(selectTvCommands, lowerCaseCommandText)) {
                return selectTvByCallback(message, chat, user, commandText);
            } else if (containsStartWith(deleteTvCommands, lowerCaseCommandText)) {
                return getKeyboardWithDeletingTvChannels(message, chat, user, commandText);
            }
        }

        if (emptyTvCommands.contains(lowerCaseCommandText)) {
            return getUserTvListWithKeyboard(message,  chat, user, true);
        } else if (containsStartWith(deleteTvCommands, lowerCaseCommandText)) {
            return deleteUserTv(message, chat, user, commandText);
        } else if (containsStartWith(selectTvCommands, lowerCaseCommandText)) {
            return selectUserTv(message, chat, user, commandText);
        } else {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }

    private EditResponse selectTvByCallback(Message message, Chat chat, User user, String command) {
        String selectTvCommand = getLocalizedCommand(command, SELECT_TV_COMMAND);
        if (command.equals(selectTvCommand)) {
            return getKeyboardWithSelectingTvChannels(message, 0);
        }

        Set<String> selectPageTvListCommandSet = internationalizationService.internationalize(SELECT_PAGE_TV_LIST);
        if (selectPageTvListCommandSet != null) {
            String selectPageTvListCommand = getStartsWith(selectPageTvListCommandSet, command);
            if (selectPageTvListCommand != null) {
                return getKeyboardWithSelectingTvChannels(
                        message,
                        Integer.parseInt(command.substring(selectPageTvListCommand.length())));
            }
        }

        int tvChannelId;
        try {
            tvChannelId = Integer.parseInt(command.substring(selectTvCommand.length() + 1));
        } catch (Exception e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        TvChannel tvChannel = tvChannelService.get(tvChannelId);
        if (tvChannel == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        UserTv userTv = userTvService.get(chat, user, tvChannel);
        if (userTv == null) {
            userTv = new UserTv();
            userTv.setChat(chat);
            userTv.setUser(user);
            userTv.setTvChannel(tvChannel);
        }

        userTvService.save(userTv);

        return (EditResponse) getUserTvListWithKeyboard(message, chat, user, false);
    }

    private EditResponse getKeyboardWithSelectingTvChannels(Message message, int page) {
        Page<TvChannel> tvChannelList = tvChannelService.getAll(page);

        List<List<KeyboardButton>> tvChannelRows = tvChannelList.stream().map(tvChannel -> List.of(
                new KeyboardButton()
                        .setName(tvChannel.getName())
                        .setCallback(CALLBACK_SELECT_TV_COMMAND + " " + tvChannel.getId()))).collect(Collectors.toList());

        List<KeyboardButton> pagesRow = new ArrayList<>();
        if (page > 0) {
            pagesRow.add(new KeyboardButton()
                    .setName(Emoji.LEFT_ARROW.getSymbol() + "${setter.tv.button.back}")
                    .setCallback(CALLBACK_SELECT_PAGE_TV_LIST + (page - 1)));
        }

        if (page < tvChannelList.getTotalPages()) {
            pagesRow.add(new KeyboardButton()
                    .setName("${setter.tv.button.forward}" + Emoji.RIGHT_ARROW.getSymbol())
                    .setCallback(CALLBACK_SELECT_PAGE_TV_LIST + (page + 1)));
        }

        tvChannelRows.add(pagesRow);

        Keyboard keyboard = new Keyboard(addingMainRows(tvChannelRows));

        return new EditResponse(message)
                .setText("${setter.tv.selecthelp}")
                .setKeyboard(keyboard)
                .setResponseSettings(FormattingStyle.MARKDOWN);
    }

    private EditResponse getKeyboardWithDeletingTvChannels(Message message, Chat chat, User user, String commandText) throws BotException {
        String deleteTvCommand = getLocalizedCommand(commandText, DELETE_TV_COMMAND);
        if (!deleteTvCommand.equals(commandText)) {
            deleteUserTv(message, chat, user, commandText);
        }

        List<UserTv> userTvList = userTvService.get(chat, user);

        List<List<KeyboardButton>> tvChannelRows = userTvList.stream().map(userTv -> List.of(
                new KeyboardButton()
                        .setName(Emoji.DELETE.getSymbol() + userTv.getTvChannel().getName())
                        .setCallback(CALLBACK_DELETE_TV_COMMAND + " " + userTv.getId()))).toList();

        Keyboard keyboard = new Keyboard(addingMainRows(tvChannelRows));

        return new EditResponse(message)
                .setText("${setter.tv.selectedchannels}")
                .setKeyboard(keyboard)
                .setResponseSettings(FormattingStyle.MARKDOWN);
    }

    private BotResponse getUserTvListWithKeyboard(Message message, Chat chat, User user, boolean newMessage) {
        log.debug("Request to list all user tv for chat {} and user {}", chat.getChatId(), user.getUserId());
        List<UserTv> userTvList = userTvService.get(chat, user);

        if (newMessage) {
            return new TextResponse(message)
                    .setText(prepareTextOfListUserTvList(userTvList))
                    .setKeyboard(prepareKeyboardWithUserTvList())
                    .setResponseSettings(FormattingStyle.MARKDOWN);
        }

        return new EditResponse(message)
                .setText(prepareTextOfListUserTvList(userTvList))
                .setKeyboard(prepareKeyboardWithUserTvList())
                .setResponseSettings(FormattingStyle.MARKDOWN);
    }

    private TextResponse deleteUserTv(Message message, Chat chat, User user, String command) throws BotException {
        String deleteTvCommand = getLocalizedCommand(command, DELETE_TV_COMMAND);
        log.debug("Request to delete userTv");

        String params;
        try {
            params = command.substring(deleteTvCommand.length() + 1);
        } catch (Exception e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        UserTv userTv;
        try {
            userTv = userTvService.get(Long.parseLong(params));
        } catch (Exception e) {
            String tvChannelName = params.toLowerCase(Locale.ROOT);
            userTv = userTvService.get(chat, user)
                    .stream()
                    .filter(userTv1 -> userTv1.getTvChannel().getName().toLowerCase(Locale.ROOT).equals(tvChannelName))
                    .findFirst()
                    .orElse(null);

            if (userTv == null) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }
        }

        if (!userTv.getUser().getUserId().equals(user.getUserId())) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NOT_OWNER));
        }

        userTvService.remove(userTv);

        return new TextResponse(message)
                .setText(speechService.getRandomMessageByTag(BotSpeechTag.SAVED))
                .setResponseSettings(FormattingStyle.MARKDOWN);
    }

    private BotResponse selectUserTv(Message message, Chat chat, User user, String command) {
        String selectTvCommand = getLocalizedCommand(command, SELECT_TV_COMMAND);
        log.debug("Request to select userTv");

        if (command.equals(selectTvCommand)) {
            return selectTvByCallback(message,  chat, user, SELECT_TV_COMMAND);
        }

        String param = command.substring(selectTvCommand.length() + 1);

        String tvChannelName = param.toLowerCase(Locale.ROOT);
        TvChannel tvChannel = tvChannelService.get(tvChannelName)
                .stream()
                .filter(channel -> channel.getName().toLowerCase(Locale.ROOT).equals(tvChannelName))
                .findFirst()
                .orElse(null);
        if (tvChannel == null) {
            try {
                tvChannel = tvChannelService.get(Integer.parseInt(param));
            } catch (NumberFormatException e) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }
            if (tvChannel == null) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }
        }

        UserTv userTv = userTvService.get(chat, user, tvChannel);
        if (userTv != null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        userTv = new UserTv();
        userTv.setChat(chat);
        userTv.setUser(user);
        userTv.setTvChannel(tvChannel);
        userTvService.save(userTv);

        CommandWaiting commandWaiting = commandWaitingService.get(chat, user);
        if (commandWaiting != null && commandWaiting.getCommandName().equals("set")) {
            commandWaitingService.remove(commandWaiting);
        }

        return new TextResponse(message)
                .setText(speechService.getRandomMessageByTag(BotSpeechTag.SAVED))
                .setResponseSettings(FormattingStyle.MARKDOWN);
    }

    private List<List<KeyboardButton>> addingMainRows(List<List<KeyboardButton>> rows) {
        rows.add(List.of(new KeyboardButton()
                .setName(Emoji.RIGHT_ARROW_CURVING_UP.getSymbol() + "${setter.tv.button.select}")
                .setCallback(CALLBACK_SELECT_TV_COMMAND)));
        rows.add(List.of(new KeyboardButton()
                .setName(Emoji.DELETE.getSymbol() + "${setter.tv.button.remove}")
                .setCallback(CALLBACK_DELETE_TV_COMMAND)));
        rows.add(List.of(new KeyboardButton()
                .setName(Emoji.UPDATE.getSymbol() + "${setter.tv.button.update}")
                .setCallback(CALLBACK_COMMAND + UPDATE_TV_COMMAND)));
        rows.add(List.of(new KeyboardButton()
                .setName(Emoji.BACK.getSymbol() + "${setter.tv.button.settings}")
                .setCallback(CALLBACK_COMMAND + "back")));

        return rows;
    }

    private String prepareTextOfListUserTvList(List<UserTv> userTvList) {
        final StringBuilder buf = new StringBuilder();
        buf.append("<b>${setter.tv.selectedchannels}:</b>\n");

        userTvList.forEach(userTv -> buf
                .append(userTv.getId()).append(". ")
                .append(userTv.getTvChannel().getName()).append("\n"));

        return buf.toString();
    }

    private Keyboard prepareKeyboardWithUserTvList() {
        return new Keyboard(
                new KeyboardButton()
                        .setName(Emoji.DELETE.getSymbol() + "${setter.tv.button.remove}")
                        .setCallback(CALLBACK_DELETE_TV_COMMAND),
                new KeyboardButton()
                        .setName(Emoji.NEW.getSymbol() + "${setter.tv.button.select}")
                        .setCallback(CALLBACK_SELECT_TV_COMMAND),
                new KeyboardButton()
                        .setName(Emoji.UPDATE.getSymbol() + "${setter.tv.button.update}")
                        .setCallback(CALLBACK_COMMAND + UPDATE_TV_COMMAND),
                new KeyboardButton()
                        .setName(Emoji.BACK.getSymbol() + "${setter.tv.button.settings}")
                        .setCallback(CALLBACK_COMMAND + "back")
        );
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
