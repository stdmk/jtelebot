package org.telegram.bot.commands.setters;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.telegram.bot.commands.Set;
import org.telegram.bot.domain.BotStats;
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
public class NewsSetter implements Setter<BotResponse> {

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

    public BotResponse set(BotRequest request, String commandText) {
        Message message = request.getMessage();
        Chat chat = message.getChat();
        User user = message.getUser();
        String lowerCaseCommandText = commandText.toLowerCase();

        if (message.isCallback()) {
            if (emptyNewsCommands.contains(lowerCaseCommandText) || updateNewsCommands.contains(lowerCaseCommandText)) {
                return getNewsSourcesListForChatWithKeyboard(message, chat);
            } else if (containsStartWith(deleteNewsCommands, lowerCaseCommandText)) {
                return deleteNewsSourceForChatByCallback(message, chat, commandText);
            } else if (containsStartWith(deleteNewsSourceCommands, lowerCaseCommandText)) {
                return getKeyboardWithDeletingNewsSource(message, user, commandText);
            } else if (containsStartWith(addNewsSourceCommands, lowerCaseCommandText)) {
                return addNewsSourceForChatByCallback(message, chat, user);
            } else if (containsStartWith(selectNewsCommands, lowerCaseCommandText)) {
                return selectNewsCallback(message, chat, commandText);
            }
        }

        if (emptyNewsCommands.contains(lowerCaseCommandText)) {
            return getNewsSourcesListForChat(message, chat);
        } else if (containsStartWith(deleteNewsCommands, lowerCaseCommandText)) {
            return deleteNewsSourceForChat(message, chat, commandText);
        } else if (containsStartWith(addNewsSourceCommands, lowerCaseCommandText)) {
            return addNewsSourceForChat(message, chat, user, commandText);
        } else {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }

    private BotResponse selectNewsCallback(Message message, Chat chat, String command) {
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
            return new TextResponse(message)
                    .setText(speechService.getRandomMessageByTag(BotSpeechTag.DUPLICATE_ENTRY))
                    .setResponseSettings(FormattingStyle.HTML);
        }

        newsService.save(new org.telegram.bot.domain.entities.News()
                .setNewsSource(newsSource)
                .setChat(chat));

        return getNewsSourcesListForChatWithKeyboard(message, chat);
    }

    private EditResponse getKeyboardWithSelectingNews(Message message, int page) {
        Page<NewsSource> newsSourceList = newsSourceService.getAll(page);

        List<List<KeyboardButton>> newsSourcesRows = newsSourceList
                .stream()
                .map(newsSource -> List.of(
                        new KeyboardButton()
                                .setName(newsSource.getName())
                                .setCallback(CALLBACK_SELECT_NEWS_SOURCE_COMMAND + " " + newsSource.getId())))
                .collect(Collectors.toList());

        List<KeyboardButton> pagesRow = new ArrayList<>(2);
        if (page > 0) {
            KeyboardButton backButton = new KeyboardButton();
            backButton.setName(Emoji.LEFT_ARROW.getSymbol() + "${setter.news.button.back}");
            backButton.setCallback(CALLBACK_SELECT_PAGE_NEWS_LIST + (page - 1));

            pagesRow.add(backButton);
        }

        if (page + 1 < newsSourceList.getTotalPages()) {
            KeyboardButton forwardButton = new KeyboardButton();
            forwardButton.setName("${setter.news.button.forward}" + Emoji.RIGHT_ARROW.getSymbol());
            forwardButton.setCallback(CALLBACK_SELECT_PAGE_NEWS_LIST + (page + 1));

            pagesRow.add(forwardButton);
        }

        newsSourcesRows.add(pagesRow);

        return new EditResponse(message)
                .setText("${setter.news.selecthelp}")
                .setKeyboard(new Keyboard(addingMainRows(newsSourcesRows)));
    }

    private EditResponse getKeyboardWithDeletingNewsSource(Message message, User user, String commandText) {
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

        List<List<KeyboardButton>> newsSourcesRows = newsSourceList
                .stream()
                .map(newsSource -> {
                    List<KeyboardButton> newsSourceRow = new ArrayList<>();

                    KeyboardButton newsSourceButton = new KeyboardButton();
                    newsSourceButton.setName(Emoji.DELETE.getSymbol() + newsSource.getName());
                    newsSourceButton.setCallback(CALLBACK_DELETE_NEWS_SOURCE_COMMAND + " " + newsSource.getId());

                    newsSourceRow.add(newsSourceButton);

                    return newsSourceRow;
                }).collect(Collectors.toList());

        List<KeyboardButton> pagesRow = new ArrayList<>();
        if (page > 0) {
            KeyboardButton backButton = new KeyboardButton();
            backButton.setName(Emoji.LEFT_ARROW.getSymbol() + "${setter.news.button.back}");
            backButton.setCallback(CALLBACK_DELETE_PAGE_NEWS_LIST + (page - 1));

            pagesRow.add(backButton);
        }

        if (page + 1 < newsSourceList.getTotalPages()) {
            KeyboardButton forwardButton = new KeyboardButton();
            forwardButton.setName("${setter.news.button.forward}" + Emoji.RIGHT_ARROW.getSymbol());
            forwardButton.setCallback(CALLBACK_DELETE_PAGE_NEWS_LIST + (page + 1));

            pagesRow.add(forwardButton);
        }

        newsSourcesRows.add(pagesRow);

        return new EditResponse(message)
                .setText("${setter.news.removingcaption}")
                .setKeyboard(new Keyboard(addingMainRows(newsSourcesRows)));
    }

    private TextResponse deleteNewsSource(Message message, User user, String command) throws BotException {
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

        return new TextResponse(message)
                .setText(speechService.getRandomMessageByTag(BotSpeechTag.SAVED))
                .setResponseSettings(FormattingStyle.HTML);
    }

    private BotResponse addNewsSourceForChat(Message message, Chat chat, User user, String command) {
        checkUserLevel(user);

        String addNewsCommand = getLocalizedCommand(command, ADD_NEWS_SOURCE_COMMAND);
        log.debug("Request to add new news resource");

        if (command.equals(addNewsCommand)) {
            List<News> allNewsInChat = newsService.getAll(chat);
            return new TextResponse(message)
                    .setText(prepareTextOfListNewsSources(allNewsInChat) + ADDING_HELP_TEXT)
                    .setKeyboard(prepareKeyboardWithNews(allNewsInChat));
        }

        String params = command.substring(addNewsCommand.length() + 1);

        int i = params.indexOf(" ");
        if (i < 0) {
            commandWaitingService.remove(commandWaitingService.get(chat, user));
            return new TextResponse(message)
                    .setText(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT))
                    .setResponseSettings(FormattingStyle.HTML);
        }

        String name;
        URL url;
        if (!params.startsWith("http") && !params.startsWith("http", i + 1)) {
            commandWaitingService.remove(commandWaitingService.get(chat, user));
            return new TextResponse(message)
                    .setText(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT))
                    .setResponseSettings(FormattingStyle.HTML);
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
                return new TextResponse(message)
                        .setText("${setter.news.wrongurl}")
                        .setResponseSettings(FormattingStyle.HTML);
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

        return new TextResponse(message)
                .setText(speechService.getRandomMessageByTag(BotSpeechTag.SAVED))
                .setResponseSettings(FormattingStyle.HTML);
    }

    private EditResponse addNewsSourceForChatByCallback(Message message, Chat chat, User user) {
        commandWaitingService.add(chat, user, Set.class, CALLBACK_ADD_NEWS_SOURCE_COMMAND);
        List<News> allNewsInChat = newsService.getAll(chat);
        return new EditResponse(message)
                .setText(prepareTextOfListNewsSources(allNewsInChat) + ADDING_HELP_TEXT)
                .setKeyboard(prepareKeyboardWithNews(allNewsInChat))
                .setResponseSettings(FormattingStyle.HTML);

    }

    private TextResponse deleteNewsSourceForChat(Message message, Chat chat, String command) throws BotException {
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

        return new TextResponse(message)
                .setText(responseText)
                .setResponseSettings(FormattingStyle.HTML);
    }

    private EditResponse deleteNewsSourceForChatByCallback(Message message, Chat chat, String command) {
        String deleteNewsCommand = getLocalizedCommand(command, DELETE_NEWS_COMMAND);
        log.debug("Request to delete news resource");

        try {
            newsService.remove(Long.valueOf(command.substring(deleteNewsCommand.length() + 1)));
        } catch (Exception ignored) {
            // maybe a non-administrator is trying to delete
        }

        return getNewsSourcesListForChatWithKeyboard(message, chat);
    }

    private TextResponse getNewsSourcesListForChat(Message message, Chat chat) {
        log.debug("Request to list all news sources for chat {}", chat.getChatId());
        List<News> allNewsInChat = newsService.getAll(chat);
        return new TextResponse(message)
                .setText(prepareTextOfListNewsSources(allNewsInChat))
                .setKeyboard(prepareKeyboardWithNews(allNewsInChat))
                .setResponseSettings(FormattingStyle.HTML);
    }

    private EditResponse getNewsSourcesListForChatWithKeyboard(Message message, Chat chat) {
        log.debug("Request to list all news sources for chat {}", chat.getChatId());
        List<News> allNewsInChat = newsService.getAll(chat);
        return new EditResponse(message)
                .setText(prepareTextOfListNewsSources(allNewsInChat))
                .setKeyboard(prepareKeyboardWithNews(allNewsInChat))
                .setResponseSettings(FormattingStyle.HTML);
    }

    private String prepareTextOfListNewsSources(List<News> allNewsInChat) {
        StringBuilder buf = new StringBuilder();
        buf.append("<b>${setter.news.caption}:</b>\n");

        allNewsInChat.forEach(news -> buf
                .append("<a href=\"").append(news.getNewsSource().getUrl()).append("\">")
                .append(news.getNewsSource().getName()).append("</a>\n"));

        return buf.toString();
    }

    private Keyboard prepareKeyboardWithNews(List<News> allNewsInChat) {
        List<List<KeyboardButton>> rows = allNewsInChat.stream().map(news -> List.of(
                new KeyboardButton()
                        .setName(Emoji.DELETE.getSymbol() + news.getNewsSource().getName())
                        .setCallback(CALLBACK_DELETE_NEWS_COMMAND + " " + news.getId()))).collect(Collectors.toList());

        addingMainRows(rows);

        return new Keyboard(rows);
    }

    private List<List<KeyboardButton>> addingMainRows(List<List<KeyboardButton>> rows) {
        rows.add(new ArrayList<>(List.of(
                new KeyboardButton()
                        .setName(Emoji.RIGHT_ARROW_CURVING_UP.getSymbol() + "${setter.news.button.select}")
                        .setCallback(CALLBACK_SELECT_NEWS_SOURCE_COMMAND))));
        rows.add(List.of(
                new KeyboardButton()
                        .setName(Emoji.DELETE.getSymbol() + "${setter.news.button.remove}")
                        .setCallback(CALLBACK_DELETE_NEWS_SOURCE_COMMAND)));
        rows.add(List.of(
                new KeyboardButton()
                        .setName(Emoji.NEW.getSymbol() + "${setter.news.button.add}")
                        .setCallback(CALLBACK_ADD_NEWS_SOURCE_COMMAND)));
        rows.add(List.of(
                new KeyboardButton()
                        .setName(Emoji.UPDATE.getSymbol() + "${setter.news.button.update}")
                        .setCallback(CALLBACK_COMMAND + UPDATE_NEWS_COMMAND)));
        rows.add(List.of(
                new KeyboardButton()
                        .setName(Emoji.BACK.getSymbol() + "${setter.news.button.settings}")
                        .setCallback(CALLBACK_COMMAND + "back")));

        return rows;
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
