package org.telegram.bot.commands.setters;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.telegram.bot.commands.Set;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.entities.*;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.TextUtils.containsStartWith;
import static org.telegram.bot.utils.TextUtils.getStartsWith;

@Component
@RequiredArgsConstructor
@Slf4j
public class NewsSetter implements Setter<PartialBotApiMethod<?>> {

    private static final String CALLBACK_COMMAND = "${setter.command} ";
    private static final String EMPTY_NEWS_COMMAND = "${setter.news.emptycommand}";
    private static final String UPDATE_NEWS_COMMAND = EMPTY_NEWS_COMMAND + " ${setter.news.update}";
    private static final String DELETE_NEWS_COMMAND = EMPTY_NEWS_COMMAND + " ${setter.news.remove}";
    private static final String CALLBACK_DELETE_NEWS_COMMAND = CALLBACK_COMMAND + DELETE_NEWS_COMMAND;
    private static final String DELETE_NEWS_SOURCE_COMMAND = EMPTY_NEWS_COMMAND + " ${setter.news.sourceremove}";
    private static final String CALLBACK_DELETE_NEWS_SOURCE_COMMAND = CALLBACK_COMMAND + DELETE_NEWS_SOURCE_COMMAND;
    private static final String ADD_NEWS_SOURCE_COMMAND = EMPTY_NEWS_COMMAND + " ${setter.news.sourceadd}";
    private static final String CALLBACK_ADD_NEWS_SOURCE_COMMAND = CALLBACK_COMMAND + ADD_NEWS_SOURCE_COMMAND;
    private static final String SELECT_NEWS_SOURCE_COMMAND = EMPTY_NEWS_COMMAND + " ${setter.news.sourcesel}";
    private static final String CALLBACK_SELECT_NEWS_SOURCE_COMMAND = CALLBACK_COMMAND + SELECT_NEWS_SOURCE_COMMAND;
    private static final String SELECT_PAGE_NEWS_SOURCE_LIST = SELECT_NEWS_SOURCE_COMMAND + " page";
    private static final String CALLBACK_SELECT_PAGE_NEWS_LIST = CALLBACK_COMMAND + SELECT_PAGE_NEWS_SOURCE_LIST;
    private static final String DELETE_PAGE_NEWS_SOURCE_LIST = DELETE_NEWS_SOURCE_COMMAND + " page";
    private static final String CALLBACK_DELETE_PAGE_NEWS_LIST = CALLBACK_COMMAND + DELETE_PAGE_NEWS_SOURCE_LIST;
    private static final String ADDING_HELP_TEXT = "${setter.news.addhelp}\n${setter.news.addexample}\n";

    private final java.util.Set<String> emptyNewsCommands = new HashSet<>();
    private final java.util.Set<String> updateNewsCommands = new HashSet<>();
    private final java.util.Set<String> deleteNewsCommands = new HashSet<>();
    private final java.util.Set<String> addNewsSourceCommands = new HashSet<>();
    private final java.util.Set<String> deleteNewsSourceCommands = new HashSet<>();
    private final java.util.Set<String> selectNewsCommands = new HashSet<>();

    private final NewsService newsService;
    private final NewsSourceService newsSourceService;
    private final SpeechService speechService;
    private final CommandWaitingService commandWaitingService;
    private final UserService userService;
    private final InternationalizationService internationalizationService;
    private final BotStats botStats;

    @PostConstruct
    private void postConstruct() {
        emptyNewsCommands.addAll(internationalizationService.getAllTranslations("setter.news.emptycommand"));
        updateNewsCommands.addAll(internationalizationService.internationalize(UPDATE_NEWS_COMMAND));
        deleteNewsCommands.addAll(internationalizationService.internationalize(DELETE_NEWS_COMMAND));
        addNewsSourceCommands.addAll(internationalizationService.internationalize(ADD_NEWS_SOURCE_COMMAND));
        selectNewsCommands.addAll(internationalizationService.internationalize(SELECT_NEWS_SOURCE_COMMAND));
        deleteNewsSourceCommands.addAll(internationalizationService.internationalize(DELETE_NEWS_SOURCE_COMMAND));
    }

    @Override
    public boolean canProcessed(String command) {
        return emptyNewsCommands.stream().anyMatch(command::startsWith);
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.MODERATOR;
    }

    public PartialBotApiMethod<?> set(Update update, String commandText) {
        Message message = getMessageFromUpdate(update);
        Chat chat = new Chat().setChatId(message.getChatId());
        String lowerCaseCommandText = commandText.toLowerCase();

        if (update.hasCallbackQuery()) {
            User user = new User().setUserId(update.getCallbackQuery().getFrom().getId());

            if (emptyNewsCommands.contains(lowerCaseCommandText) || updateNewsCommands.contains(lowerCaseCommandText)) {
                return getNewsSourcesListForChatWithKeyboard(message, chat);
            } else if (containsStartWith(deleteNewsCommands, lowerCaseCommandText)) {
                return deleteNewsSourceForChatByCallback(message, chat, commandText);
            } else if (containsStartWith(deleteNewsSourceCommands, lowerCaseCommandText)) {
                return getKeyboardWithDeletingNewsSource(message, user, commandText);
            } else if (containsStartWith(addNewsSourceCommands, lowerCaseCommandText)) {
                return addNewsSourceForChatByCallback(message, chat, new User().setUserId(update.getCallbackQuery().getFrom().getId()));
            } else if (containsStartWith(selectNewsCommands, lowerCaseCommandText)) {
                return selectNewsCallback(message, chat, commandText);
            }
        }

        if (emptyNewsCommands.contains(lowerCaseCommandText)) {
            return getNewsSourcesListForChat(message, chat);
        } else if (containsStartWith(deleteNewsCommands, lowerCaseCommandText)) {
            return deleteNewsSourceForChat(message, chat, commandText);
        } else if (containsStartWith(addNewsSourceCommands, lowerCaseCommandText)) {
            return addNewsSourceForChat(message, chat, new User().setUserId(message.getFrom().getId()), commandText);
        } else {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }

    private PartialBotApiMethod<?> selectNewsCallback(Message message, Chat chat, String command) {
        String selectNewsCommand = getLocalizedCommand(command, SELECT_NEWS_SOURCE_COMMAND);
        if (command.equals(selectNewsCommand)) {
            return getKeyboardWithSelectingNews(message, 0);
        }

        java.util.Set<String> selectPageNewsListCommandSet = internationalizationService.internationalize(SELECT_PAGE_NEWS_SOURCE_LIST);
        if (selectPageNewsListCommandSet != null) {
            String selectPageNewsListCommand = getStartsWith(selectPageNewsListCommandSet, command);
            if (selectPageNewsListCommand != null) {
                return getKeyboardWithSelectingNews(
                        message,
                        Integer.parseInt(command.substring(selectPageNewsListCommand.length())));
            }
        }

        long newsSourceId;
        try {
            newsSourceId = Long.parseLong(command.substring(selectNewsCommand.length() + 1));
        } catch (Exception e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        NewsSource newsSource = newsSourceService.get(newsSourceId);
        if (newsSource == null) {
            log.error("Wrong NewsSource id from callback: {}", newsSourceId);
            botStats.incrementErrors(newsSourceId, "Wrong NewsSource id from callback");
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        org.telegram.bot.domain.entities.News news = newsService.get(chat, newsSource);
        if (news != null) {
            return buildSendMessageWithText(message, speechService.getRandomMessageByTag(BotSpeechTag.DUPLICATE_ENTRY));
        }

        newsService.save(new org.telegram.bot.domain.entities.News()
                .setNewsSource(newsSource)
                .setChat(chat));

        return getNewsSourcesListForChatWithKeyboard(message, chat);
    }

    private EditMessageText getKeyboardWithSelectingNews(Message message, int page) {
        Page<NewsSource> newsSourceList = newsSourceService.getAll(page);

        List<List<InlineKeyboardButton>> newsSourcesRows = newsSourceList
                .stream()
                .map(newsSource -> {
                    List<InlineKeyboardButton> newsSourceRow = new ArrayList<>();

                    InlineKeyboardButton newsSourceButton = new InlineKeyboardButton();
                    newsSourceButton.setText(newsSource.getName());
                    newsSourceButton.setCallbackData(CALLBACK_SELECT_NEWS_SOURCE_COMMAND + " " + newsSource.getId());

                    newsSourceRow.add(newsSourceButton);

                    return newsSourceRow;
                }).collect(Collectors.toList());

        List<InlineKeyboardButton> pagesRow = new ArrayList<>();
        if (page > 0) {
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText(Emoji.LEFT_ARROW.getSymbol() + "${setter.news.button.back}");
            backButton.setCallbackData(CALLBACK_SELECT_PAGE_NEWS_LIST + (page - 1));

            pagesRow.add(backButton);
        }

        if (page + 1 < newsSourceList.getTotalPages()) {
            InlineKeyboardButton forwardButton = new InlineKeyboardButton();
            forwardButton.setText("${setter.news.button.forward}" + Emoji.RIGHT_ARROW.getSymbol());
            forwardButton.setCallbackData(CALLBACK_SELECT_PAGE_NEWS_LIST + (page + 1));

            pagesRow.add(forwardButton);
        }

        newsSourcesRows.add(pagesRow);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(addingMainRows(newsSourcesRows));

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.enableMarkdown(true);
        editMessageText.setText("${setter.news.selecthelp}");
        editMessageText.setReplyMarkup(inlineKeyboardMarkup);

        return editMessageText;
    }

    private EditMessageText getKeyboardWithDeletingNewsSource(Message message, User user, String commandText) {
        String deleteNewsSourceCommand = getLocalizedCommand(commandText, DELETE_NEWS_SOURCE_COMMAND);
        java.util.Set<String> deletePageNewsListCommandSet = internationalizationService.internationalize(DELETE_PAGE_NEWS_SOURCE_LIST);
        if (!deleteNewsSourceCommand.equals(commandText) && (!containsStartWith(deletePageNewsListCommandSet, commandText))) {
            deleteNewsSource(message, user, commandText);
        }

        int page = 0;

        if (deletePageNewsListCommandSet != null) {
            String deletePageNewsListCommand = getStartsWith(deletePageNewsListCommandSet, commandText);
            if (deletePageNewsListCommand != null) {
                page = Integer.parseInt(commandText.substring(deletePageNewsListCommand.length()));
            }
        }

        Page<NewsSource> newsSourceList = newsSourceService.getAll(page);

        List<List<InlineKeyboardButton>> newsSourcesRows = newsSourceList
                .stream()
                .map(newsSource -> {
                    List<InlineKeyboardButton> newsSourceRow = new ArrayList<>();

                    InlineKeyboardButton newsSourceButton = new InlineKeyboardButton();
                    newsSourceButton.setText(Emoji.DELETE.getSymbol() + newsSource.getName());
                    newsSourceButton.setCallbackData(CALLBACK_DELETE_NEWS_SOURCE_COMMAND + " " + newsSource.getId());

                    newsSourceRow.add(newsSourceButton);

                    return newsSourceRow;
                }).collect(Collectors.toList());

        List<InlineKeyboardButton> pagesRow = new ArrayList<>();
        if (page > 0) {
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText(Emoji.LEFT_ARROW.getSymbol() + "${setter.news.button.back}");
            backButton.setCallbackData(CALLBACK_DELETE_PAGE_NEWS_LIST + (page - 1));

            pagesRow.add(backButton);
        }

        if (page + 1 < newsSourceList.getTotalPages()) {
            InlineKeyboardButton forwardButton = new InlineKeyboardButton();
            forwardButton.setText("${setter.news.button.forward}" + Emoji.RIGHT_ARROW.getSymbol());
            forwardButton.setCallbackData(CALLBACK_DELETE_PAGE_NEWS_LIST + (page + 1));

            pagesRow.add(forwardButton);
        }

        newsSourcesRows.add(pagesRow);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(addingMainRows(newsSourcesRows));

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.enableMarkdown(true);
        editMessageText.setText("${setter.news.removingcaption}");
        editMessageText.setReplyMarkup(inlineKeyboardMarkup);

        return editMessageText;
    }

    private SendMessage deleteNewsSource(Message message, User user, String command) throws BotException {
        checkUserLevel(user);

        String deleteNewsSourceCommand = getLocalizedCommand(command, DELETE_NEWS_SOURCE_COMMAND);
        log.debug("Request to delete newsSource");

        String params;
        try {
            params = command.substring(deleteNewsSourceCommand.length() + 1);
        } catch (Exception e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        NewsSource newsSource;
        try {
            newsSource = newsSourceService.get(Long.parseLong(params));
        } catch (Exception e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        newsSourceService.remove(newsSource);

        return buildSendMessageWithText(message, speechService.getRandomMessageByTag(BotSpeechTag.SAVED));
    }

    private PartialBotApiMethod<?> addNewsSourceForChat(Message message, Chat chat, User user, String command) {
        checkUserLevel(user);

        String addNewsCommand = getLocalizedCommand(command, ADD_NEWS_SOURCE_COMMAND);
        log.debug("Request to add new news resource");

        if (command.equals(addNewsCommand)) {
            List<News> allNewsInChat = newsService.getAll(chat);

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId().toString());
            sendMessage.enableHtml(true);
            sendMessage.setReplyMarkup(prepareKeyboardWithNews(allNewsInChat));
            sendMessage.setText(prepareTextOfListNewsSources(allNewsInChat) + ADDING_HELP_TEXT);

            return sendMessage;
        }

        String params = command.substring(addNewsCommand.length() + 1);

        int i = params.indexOf(" ");
        if (i < 0) {
            commandWaitingService.remove(commandWaitingService.get(chat, user));
            return buildSendMessageWithText(message, speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        String name;
        URL url;
        if (!params.startsWith("http") && !params.startsWith("http", i + 1)) {
            commandWaitingService.remove(commandWaitingService.get(chat, user));
            return buildSendMessageWithText(message, speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        try {
            url = new URL(params.substring(0, i));
            name = params.substring(i + 1);
        } catch (MalformedURLException e) {
            try {
                url = new URL(params.substring(i + 1));
                name = params.substring(0, i);
            } catch (MalformedURLException malformedURLException) {
                commandWaitingService.remove(commandWaitingService.get(chat, user));
                return buildSendMessageWithText(message, "${setter.news.wrongurl}");
            }
        }

        NewsSource newsSource = newsSourceService.get(url.toString());
        if (newsSource != null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.DUPLICATE_ENTRY));
        }

        newsSourceService.save(new NewsSource()
                .setUrl(url.toString())
                .setName(name));

        CommandWaiting commandWaiting = commandWaitingService.get(chat, user);
        if (commandWaiting != null && commandWaiting.getCommandName().equals("set")) {
            commandWaitingService.remove(commandWaiting);
        }

        return buildSendMessageWithText(message, speechService.getRandomMessageByTag(BotSpeechTag.SAVED));
    }

    private EditMessageText addNewsSourceForChatByCallback(Message message, Chat chat, User user) {
        commandWaitingService.add(chat, user, Set.class, CALLBACK_ADD_NEWS_SOURCE_COMMAND);

        List<News> allNewsInChat = newsService.getAll(chat);

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.enableHtml(true);
        editMessageText.setReplyMarkup(prepareKeyboardWithNews(allNewsInChat));
        editMessageText.setText(prepareTextOfListNewsSources(allNewsInChat) + ADDING_HELP_TEXT);

        return editMessageText;

    }

    private SendMessage deleteNewsSourceForChat(Message message, Chat chat, String command) throws BotException {
        String deleteNewsCommand = getLocalizedCommand(command, DELETE_NEWS_COMMAND);
        log.debug("Request to delete news resource");

        String params;
        try {
            params = command.substring(deleteNewsCommand.length() + 1);
        } catch (Exception e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        String responseText;

        try {
            Long newsSourceId = Long.parseLong(params);
            if (newsService.remove(chat, newsSourceId)) {
                responseText = speechService.getRandomMessageByTag(BotSpeechTag.SAVED);
            } else {
                responseText = speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
            }
        } catch (Exception e) {
            if (newsService.remove(chat, params)) {
                responseText = speechService.getRandomMessageByTag(BotSpeechTag.SAVED);
            } else {
                responseText = speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
            }
        }

        return buildSendMessageWithText(message, responseText);
    }

    private EditMessageText deleteNewsSourceForChatByCallback(Message message, Chat chat, String command) {
        String deleteNewsCommand = getLocalizedCommand(command, DELETE_NEWS_COMMAND);
        log.debug("Request to delete news resource");

        try {
            newsService.remove(Long.valueOf(command.substring(deleteNewsCommand.length() + 1)));
        } catch (Exception ignored) {
            // maybe a non-administrator is trying to delete
        }

        return getNewsSourcesListForChatWithKeyboard(message, chat);
    }

    private SendMessage getNewsSourcesListForChat(Message message, Chat chat) {
        log.debug("Request to list all news sources for chat {}", chat.getChatId());

        List<News> allNewsInChat = newsService.getAll(chat);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.enableHtml(true);
        sendMessage.setText(prepareTextOfListNewsSources(allNewsInChat));
        sendMessage.setReplyMarkup(prepareKeyboardWithNews(allNewsInChat));

        return sendMessage;
    }

    private EditMessageText getNewsSourcesListForChatWithKeyboard(Message message, Chat chat) {
        log.debug("Request to list all news sources for chat {}", chat.getChatId());
        List<News> allNewsInChat = newsService.getAll(chat);

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.enableHtml(true);
        editMessageText.setText(prepareTextOfListNewsSources(allNewsInChat));
        editMessageText.setReplyMarkup(prepareKeyboardWithNews(allNewsInChat));

        return editMessageText;
    }

    private String prepareTextOfListNewsSources(List<News> allNewsInChat) {
        StringBuilder buf = new StringBuilder();
        buf.append("<b>${setter.news.caption}:</b>\n");

        allNewsInChat.forEach(news -> buf
                .append("<a href=\"").append(news.getNewsSource().getUrl()).append("\">")
                .append(news.getNewsSource().getName()).append("</a>\n"));

        return buf.toString();
    }

    private InlineKeyboardMarkup prepareKeyboardWithNews(List<News> allNewsInChat) {
        List<List<InlineKeyboardButton>> rows = allNewsInChat.stream().map(news -> {
            List<InlineKeyboardButton> newsRow = new ArrayList<>();

            InlineKeyboardButton newsButton = new InlineKeyboardButton();
            newsButton.setText(Emoji.DELETE.getSymbol() + news.getNewsSource().getName());
            newsButton.setCallbackData(CALLBACK_DELETE_NEWS_COMMAND + " " + news.getId());

            newsRow.add(newsButton);

            return newsRow;
        }).collect(Collectors.toList());

        addingMainRows(rows);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }

    private List<List<InlineKeyboardButton>> addingMainRows(List<List<InlineKeyboardButton>> rows) {
        List<InlineKeyboardButton> selectButtonRow = new ArrayList<>();
        InlineKeyboardButton selectButton = new InlineKeyboardButton();
        selectButton.setText(Emoji.RIGHT_ARROW_CURVING_UP.getSymbol() + "${setter.news.button.select}");
        selectButton.setCallbackData(CALLBACK_SELECT_NEWS_SOURCE_COMMAND);
        selectButtonRow.add(selectButton);

        List<InlineKeyboardButton> deleteButtonRow = new ArrayList<>();
        InlineKeyboardButton deleteButton = new InlineKeyboardButton();
        deleteButton.setText(Emoji.DELETE.getSymbol() + "${setter.news.button.remove}");
        deleteButton.setCallbackData(CALLBACK_DELETE_NEWS_SOURCE_COMMAND);
        deleteButtonRow.add(deleteButton);

        List<InlineKeyboardButton> addButtonRow = new ArrayList<>();
        InlineKeyboardButton addButton = new InlineKeyboardButton();
        addButton.setText(Emoji.NEW.getSymbol() + "${setter.news.button.add}");
        addButton.setCallbackData(CALLBACK_ADD_NEWS_SOURCE_COMMAND);
        addButtonRow.add(addButton);

        List<InlineKeyboardButton> updateButtonRow = new ArrayList<>();
        InlineKeyboardButton updateButton = new InlineKeyboardButton();
        updateButton.setText(Emoji.UPDATE.getSymbol() + "${setter.news.button.update}");
        updateButton.setCallbackData(CALLBACK_COMMAND + UPDATE_NEWS_COMMAND);
        updateButtonRow.add(updateButton);

        List<InlineKeyboardButton> backButtonRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(Emoji.BACK.getSymbol() + "${setter.news.button.settings}");
        backButton.setCallbackData(CALLBACK_COMMAND + "back");
        backButtonRow.add(backButton);

        rows.add(selectButtonRow);
        rows.add(deleteButtonRow);
        rows.add(addButtonRow);
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

    private void checkUserLevel(User user) {
        user = userService.get(user.getUserId());
        if (!userService.isUserHaveAccessForCommand(user.getAccessLevel(), AccessLevel.ADMIN.getValue())) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NO_ACCESS));
        }
    }

}
