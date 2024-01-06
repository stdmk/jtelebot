package org.telegram.bot.commands.setters;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.commands.Set;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandWaiting;
import org.telegram.bot.domain.entities.News;
import org.telegram.bot.domain.entities.NewsSource;
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
    private static final String ADD_NEWS_COMMAND = EMPTY_NEWS_COMMAND + " ${setter.news.add}";
    private static final String CALLBACK_ADD_NEWS_COMMAND = CALLBACK_COMMAND + ADD_NEWS_COMMAND;
    private static final String ADDING_HELP_TEXT = "${setter.news.addhelp}\n${setter.news.addexample}\n";

    private final java.util.Set<String> emptyNewsCommands = new HashSet<>();
    private final java.util.Set<String> updateNewsCommands = new HashSet<>();
    private final java.util.Set<String> deleteNewsCommands = new HashSet<>();
    private final java.util.Set<String> addNewsCommands = new HashSet<>();

    private final NewsService newsService;
    private final NewsSourceService newsSourceService;
    private final SpeechService speechService;
    private final CommandWaitingService commandWaitingService;
    private final InternationalizationService internationalizationService;

    @PostConstruct
    private void postConstruct() {
        emptyNewsCommands.addAll(internationalizationService.getAllTranslations("setter.news.emptycommand"));
        updateNewsCommands.addAll(internationalizationService.internationalize(UPDATE_NEWS_COMMAND));
        deleteNewsCommands.addAll(internationalizationService.internationalize(DELETE_NEWS_COMMAND));
        addNewsCommands.addAll(internationalizationService.internationalize(ADD_NEWS_COMMAND));
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
            if (emptyNewsCommands.contains(lowerCaseCommandText) || updateNewsCommands.contains(lowerCaseCommandText)) {
                return getNewsSourcesListForChatWithKeyboard(message, chat);
            } else if (containsStartWith(deleteNewsCommands, lowerCaseCommandText)) {
                return deleteNewsSourceForChatByCallback(message, chat, commandText);
            } else if (containsStartWith(addNewsCommands, lowerCaseCommandText)) {
                return addNewsSourceForChatByCallback(message, chat, new User().setUserId(update.getCallbackQuery().getFrom().getId()));
            }
        }

        if (emptyNewsCommands.contains(lowerCaseCommandText)) {
            return getNewsSourcesListForChat(message, chat);
        } else if (containsStartWith(deleteNewsCommands, lowerCaseCommandText)) {
            return deleteNewsSourceForChat(message, chat, commandText);
        } else if (containsStartWith(addNewsCommands, lowerCaseCommandText)) {
            return addNewsSourceForChat(message, chat, new User().setUserId(message.getFrom().getId()), commandText);
        } else {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }

    private PartialBotApiMethod<?> addNewsSourceForChat(Message message, Chat chat, User user, String command) {
        String addNewsCommand = getLocalizedCommand(command, ADD_NEWS_COMMAND);
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
        if (newsSource == null) {
            newsSource = new NewsSource();
            newsSource.setUrl(url.toString());
        }

        org.telegram.bot.domain.entities.News news = newsService.get(chat, name, newsSource);
        if (news != null) {
            return buildSendMessageWithText(message, speechService.getRandomMessageByTag(BotSpeechTag.DUPLICATE_ENTRY));
        }

        if (newsSource.getId() == null) {
            newsSource = newsSourceService.save(newsSource);
        }

        news = new org.telegram.bot.domain.entities.News();
        news.setName(name);
        news.setNewsSource(newsSource);
        news.setChat(chat);
        newsService.save(news);

        CommandWaiting commandWaiting = commandWaitingService.get(chat, user);
        if (commandWaiting != null && commandWaiting.getCommandName().equals("set")) {
            commandWaitingService.remove(commandWaiting);
        }

        return buildSendMessageWithText(message, speechService.getRandomMessageByTag(BotSpeechTag.SAVED));
    }

    private EditMessageText addNewsSourceForChatByCallback(Message message, Chat chat, User user) {
        commandWaitingService.add(chat, user, Set.class, CALLBACK_ADD_NEWS_COMMAND);

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
        final StringBuilder buf = new StringBuilder();
        buf.append("<b>${setter.news.caption}:</b>\n");

        allNewsInChat.forEach(news -> buf
                .append("<a href=\"").append(news.getNewsSource().getUrl()).append("\">")
                .append(news.getName()).append("</a>\n"));

        return buf.toString();
    }

    private InlineKeyboardMarkup prepareKeyboardWithNews(List<News> allNewsInChat) {
        List<List<InlineKeyboardButton>> rows = allNewsInChat.stream().map(news -> {
            List<InlineKeyboardButton> newsRow = new ArrayList<>();

            InlineKeyboardButton newsButton = new InlineKeyboardButton();
            newsButton.setText(Emoji.DELETE.getSymbol() + news.getName());
            newsButton.setCallbackData(CALLBACK_DELETE_NEWS_COMMAND + " " + news.getId());

            newsRow.add(newsButton);

            return newsRow;
        }).collect(Collectors.toList());

        List<InlineKeyboardButton> addButtonRow = new ArrayList<>();
        InlineKeyboardButton addButton = new InlineKeyboardButton();
        addButton.setText(Emoji.NEW.getSymbol() + "${setter.news.button.add}");
        addButton.setCallbackData(CALLBACK_ADD_NEWS_COMMAND);
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

        rows.add(addButtonRow);
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
