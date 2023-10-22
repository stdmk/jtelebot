package org.telegram.bot.commands.setters;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandWaiting;
import org.telegram.bot.domain.entities.TvChannel;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserTv;
import org.telegram.bot.domain.enums.AccessLevel;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.domain.enums.Emoji;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.TvChannelService;
import org.telegram.bot.services.UserTvService;
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
public class TvSetter implements Setter<PartialBotApiMethod<?>> {

    private final UserTvService userTvService;
    private final SpeechService speechService;
    private final TvChannelService tvChannelService;
    private final CommandWaitingService commandWaitingService;

    private final String CALLBACK_COMMAND = "установить ";
    private final String UPDATE_TV_COMMAND = "тв обновить";
    private final String SELECT_TV_COMMAND = "тв выбрать";
    private final String CALLBACK_SELECT_TV_COMMAND = CALLBACK_COMMAND + SELECT_TV_COMMAND;
    private final String SELECT_PAGE_TV_LIST = SELECT_TV_COMMAND + " page";
    private final String DELETE_TV_COMMAND = "тв удалить";
    private final String CALLBACK_DELETE_TV_COMMAND = CALLBACK_COMMAND + DELETE_TV_COMMAND;

    @Override
    public boolean canProcessed(String command) {
        return command.startsWith("тв");
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

        String EMPTY_CITY_COMMAND = "тв";
        if (update.hasCallbackQuery()) {
            User user = new User().setUserId(update.getCallbackQuery().getFrom().getId());

            if (lowerCaseCommandText.equals(EMPTY_CITY_COMMAND) || lowerCaseCommandText.equals(UPDATE_TV_COMMAND)) {
                return getUserTvListWithKeyboard(message, chat, user, false);
            } else if (lowerCaseCommandText.startsWith(SELECT_TV_COMMAND)) {
                return selectTvByCallback(message, chat, user, commandText);
            } else if (lowerCaseCommandText.startsWith(DELETE_TV_COMMAND)) {
                return getKeyboardWithDeletingTvChannels(message, chat, user, commandText);
            }
        }

        User user = new User().setUserId(message.getFrom().getId());
        if (lowerCaseCommandText.equals(EMPTY_CITY_COMMAND)) {
            return getUserTvListWithKeyboard(message,  chat, user, true);
        } else if (lowerCaseCommandText.startsWith(DELETE_TV_COMMAND)) {
            return deleteUserTv(message, chat, user, commandText);
        } else if (lowerCaseCommandText.startsWith(SELECT_TV_COMMAND)) {
            return selectUserTv(message, chat, user, commandText);
        } else {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }

    private EditMessageText selectTvByCallback(Message message, Chat chat, User user, String command) throws BotException {
        command = command.replace(CALLBACK_COMMAND, "");

        if (command.equals(SELECT_TV_COMMAND)) {
            return getKeyboardWithSelectingTvChannels(message, chat, user, 0);
        } else if (command.startsWith(SELECT_PAGE_TV_LIST)) {
            return getKeyboardWithSelectingTvChannels(
                    message,
                    chat,
                    user,
                    Integer.parseInt(command.substring(SELECT_PAGE_TV_LIST.length())));
        }

        int tvChannelId;
        try {
            tvChannelId = Integer.parseInt(command.substring(SELECT_TV_COMMAND.length() + 1));
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

        return (EditMessageText) getUserTvListWithKeyboard(message, chat, user, false);
    }

    private EditMessageText getKeyboardWithSelectingTvChannels(Message message, Chat chat, User user, int page) {
        Page<TvChannel> tvChannelList = tvChannelService.getAll(page);

        List<List<InlineKeyboardButton>> tvChannelRows = tvChannelList.stream().map(tvChannel -> {
            List<InlineKeyboardButton> tvChannelRow = new ArrayList<>();

            InlineKeyboardButton tvChannelButton = new InlineKeyboardButton();
            tvChannelButton.setText(tvChannel.getName());
            tvChannelButton.setCallbackData(CALLBACK_SELECT_TV_COMMAND + " " + tvChannel.getId());

            tvChannelRow.add(tvChannelButton);

            return tvChannelRow;
        }).collect(Collectors.toList());

        List<InlineKeyboardButton> pagesRow = new ArrayList<>();
        String CALLBACK_SELECT_PAGE_TV_LIST = CALLBACK_COMMAND + SELECT_PAGE_TV_LIST;
        if (page > 0) {
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText(Emoji.LEFT_ARROW.getEmoji() + "Назад");
            backButton.setCallbackData(CALLBACK_SELECT_PAGE_TV_LIST + (page - 1));

            pagesRow.add(backButton);
        }

        if (page < tvChannelList.getTotalPages()) {
            InlineKeyboardButton forwardButton = new InlineKeyboardButton();
            forwardButton.setText("Вперёд" + Emoji.RIGHT_ARROW.getEmoji());
            forwardButton.setCallbackData(CALLBACK_SELECT_PAGE_TV_LIST + (page + 1));

            pagesRow.add(forwardButton);
        }

        tvChannelRows.add(pagesRow);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(addingMainRows(tvChannelRows));

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.enableMarkdown(true);
        editMessageText.setText("Выбери телеканал из списка");
        editMessageText.setReplyMarkup(inlineKeyboardMarkup);

        return editMessageText;
    }

    private EditMessageText getKeyboardWithDeletingTvChannels(Message message, Chat chat, User user, String commandText) throws BotException {
        if (!DELETE_TV_COMMAND.equals(commandText)) {
            deleteUserTv(message, chat, user, commandText);
        }

        List<UserTv> userTvList = userTvService.get(chat, user);

        List<List<InlineKeyboardButton>> tvChannelRows = userTvList.stream().map(userTv -> {
            List<InlineKeyboardButton> tvChannelRow = new ArrayList<>();

            InlineKeyboardButton tvChannelButton = new InlineKeyboardButton();
            tvChannelButton.setText(Emoji.DELETE.getEmoji() + userTv.getTvChannel().getName());
            tvChannelButton.setCallbackData(CALLBACK_DELETE_TV_COMMAND + " " + userTv.getId());

            tvChannelRow.add(tvChannelButton);

            return tvChannelRow;
        }).collect(Collectors.toList());

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(addingMainRows(tvChannelRows));

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.enableMarkdown(true);
        editMessageText.setText("Список выбранных телеканалов");
        editMessageText.setReplyMarkup(inlineKeyboardMarkup);

        return editMessageText;
    }

    private PartialBotApiMethod<?> getUserTvListWithKeyboard(Message message, Chat chat, User user, Boolean newMessage) {
        log.debug("Request to list all user tv for chat {} and user {}", chat.getChatId(), user.getUserId());
        List<UserTv> userTvList = userTvService.get(chat, user);

        if (newMessage) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId().toString());
            sendMessage.enableHtml(true);
            sendMessage.setText(prepareTextOfListUserTvList(userTvList));
            sendMessage.setReplyMarkup(prepareKeyboardWithUserTvList());

            return sendMessage;
        }

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.enableHtml(true);
        editMessageText.setText(prepareTextOfListUserTvList(userTvList));
        editMessageText.setReplyMarkup(prepareKeyboardWithUserTvList());

        return editMessageText;
    }

    private SendMessage deleteUserTv(Message message, Chat chat, User user, String command) throws BotException {
        log.debug("Request to delete userTv");

        String params;
        try {
            params = command.substring(DELETE_TV_COMMAND.length() + 1);
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

        return buildSendMessageWithText(message, speechService.getRandomMessageByTag(BotSpeechTag.SAVED));
    }

    private PartialBotApiMethod<?> selectUserTv(Message message, Chat chat, User user, String command) throws BotException {
        log.debug("Request to select userTv");
        if (command.equals(SELECT_TV_COMMAND)) {
            return selectTvByCallback(message,  chat, user, SELECT_TV_COMMAND);
        }

        String param = command.substring(SELECT_TV_COMMAND.length() + 1);

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

        return buildSendMessageWithText(message, speechService.getRandomMessageByTag(BotSpeechTag.SAVED));
    }

    private List<List<InlineKeyboardButton>> addingMainRows(List<List<InlineKeyboardButton>> rows) {

        List<InlineKeyboardButton> selectButtonRow = new ArrayList<>();
        InlineKeyboardButton selectButton = new InlineKeyboardButton();
        selectButton.setText(Emoji.RIGHT_ARROW_CURVING_UP.getEmoji() + "Выбрать");
        selectButton.setCallbackData(CALLBACK_SELECT_TV_COMMAND);
        selectButtonRow.add(selectButton);

        List<InlineKeyboardButton> deleteButtonRow = new ArrayList<>();
        InlineKeyboardButton deleteButton = new InlineKeyboardButton();
        deleteButton.setText(Emoji.DELETE.getEmoji() + "Удалить");
        deleteButton.setCallbackData(CALLBACK_DELETE_TV_COMMAND);
        deleteButtonRow.add(deleteButton);

        List<InlineKeyboardButton> updateButtonRow = new ArrayList<>();
        InlineKeyboardButton updateButton = new InlineKeyboardButton();
        updateButton.setText(Emoji.UPDATE.getEmoji() + "Обновить");
        updateButton.setCallbackData(CALLBACK_COMMAND + UPDATE_TV_COMMAND);
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

    private String prepareTextOfListUserTvList(List<UserTv> userTvList) {
        final StringBuilder buf = new StringBuilder();
        buf.append("<b>Список выбранных тв-каналов:</b>\n");

        userTvList.forEach(userTv -> buf
                .append(userTv.getId()).append(". ")
                .append(userTv.getTvChannel().getName()).append("\n"));

        return buf.toString();
    }

    private InlineKeyboardMarkup prepareKeyboardWithUserTvList() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> deleteButtonRow = new ArrayList<>();
        InlineKeyboardButton deleteButton = new InlineKeyboardButton();
        deleteButton.setText(Emoji.DELETE.getEmoji() + "Удалить");
        deleteButton.setCallbackData(CALLBACK_DELETE_TV_COMMAND);
        deleteButtonRow.add(deleteButton);

        List<InlineKeyboardButton> selectButtonRow = new ArrayList<>();
        InlineKeyboardButton selectButton = new InlineKeyboardButton();
        selectButton.setText(Emoji.NEW.getEmoji() + "Выбрать");
        selectButton.setCallbackData(CALLBACK_SELECT_TV_COMMAND);
        selectButtonRow.add(selectButton);

        List<InlineKeyboardButton> updateButtonRow = new ArrayList<>();
        InlineKeyboardButton updateButton = new InlineKeyboardButton();
        updateButton.setText(Emoji.UPDATE.getEmoji() + "Обновить");
        updateButton.setCallbackData(CALLBACK_COMMAND + UPDATE_TV_COMMAND);
        updateButtonRow.add(updateButton);

        List<InlineKeyboardButton> backButtonRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(Emoji.BACK.getEmoji() + "Установки");
        backButton.setCallbackData(CALLBACK_COMMAND + "back");
        backButtonRow.add(backButton);

        rows.add(deleteButtonRow);
        rows.add(selectButtonRow);
        rows.add(updateButtonRow);
        rows.add(backButtonRow);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }

    private SendMessage buildSendMessageWithText(Message message, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.enableMarkdown(true);
        sendMessage.setText(text);

        return sendMessage;
    }

    private EditMessageText buildEditMessageWithText(Message message, String text) {
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.enableMarkdown(true);
        editMessageText.setText(text);

        return editMessageText;
    }
}
