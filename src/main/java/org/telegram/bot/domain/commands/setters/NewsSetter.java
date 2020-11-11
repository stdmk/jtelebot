package org.telegram.bot.domain.commands.setters;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandWaiting;
import org.telegram.bot.domain.entities.News;
import org.telegram.bot.domain.entities.NewsSource;
import org.telegram.bot.domain.enums.Emoji;
import org.telegram.bot.domain.enums.ParseModes;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class NewsSetter implements CommandParent<PartialBotApiMethod<?>> {

    private final Logger log = LoggerFactory.getLogger(NewsSetter.class);

    private final ChatService chatService;
    private final NewsService newsService;
    private final NewsSourceService newsSourceService;
    private final SpeechService speechService;
    private final CommandWaitingService commandWaitingService;

    @Override
    public PartialBotApiMethod<?> parse(Update update, String commandText) throws Exception {
        Message message = getMessageFromUpdate(update);

        if (update.hasCallbackQuery()) {
            if (commandText.length() == 18 || commandText.startsWith("установить новости обновить")) {
                return getNewsSourcesListForChatWithKeyboard(message);
            } else if (commandText.startsWith("установить новости удалить")) {
                return deleteNewsSourceForChatByCallback(message, commandText);
            } else if (commandText.startsWith("установить новости добавить")) {
                return addNewsSourceForChatByCallback(message, update.getCallbackQuery().getFrom().getId());
            }
        }

        if (commandText.length() == 18) {
            return getNewsSourcesListForChat(message);
        } else if (commandText.startsWith("установить новости удалить")) {
            return deleteNewsSourceForChat(message, commandText);
        } else if (commandText.startsWith("установить новости добавить")) {
            return addNewsSourceForChat(message, commandText);
        } else {
            throw new BotException(speechService.getRandomMessageByTag("wrongInput"));
        }
    }

    private PartialBotApiMethod<?> addNewsSourceForChat(Message message, String command) {
        log.debug("Request to add new news resource");
        if (command.length() == 27) {
            List<News> allNewsInChat = newsService.getAll(chatService.get(message.getChatId()));

            return new SendMessage().setChatId(message.getChatId())
                    .setParseMode(ParseModes.HTML.getValue())
                    .setReplyMarkup(prepareKeyboardWithNews(allNewsInChat))
                    .setText(prepareTextOfListNewsSources(allNewsInChat) +
                            "\nНапиши мне имя нового источника новостей и ссылку на рсс-поток через пробел" +
                            "\nНапример: Лента https://lenta.ru/rss/last24");
        }

        String params = command.substring(28);

        int i = params.indexOf(" ");
        if (i < 0) {
            return buildSendMessageWithText(message, speechService.getRandomMessageByTag("wrongInput"));
        }

        String name;
        URL url;
        if (!params.startsWith("http") && !params.substring(i + 1).startsWith("http")) {
            return buildSendMessageWithText(message, speechService.getRandomMessageByTag("wrongInput"));
        }

        try {
            url = new URL(params.substring(0, i));
            name = params.substring(i + 1);
        } catch (MalformedURLException e) {
            try {
                url = new URL(params.substring(i + 1));
                name = params.substring(0, i);
            } catch (MalformedURLException malformedURLException) {
                return buildSendMessageWithText(message, "Ошибочный адрес url источника");
            }
        }

        NewsSource newsSource = newsSourceService.get(url.toString());
        if (newsSource == null) {
            newsSource = new NewsSource();
            newsSource.setUrl(url.toString());
        }

        Chat chat = chatService.get(message.getChatId());
        org.telegram.bot.domain.entities.News news = newsService.get(chat, name, newsSource);
        if (news != null) {
            return buildSendMessageWithText(message, "Такой источник уже существует: " + news.getName() + " - " + news.getNewsSource().getUrl());
        }

        if (newsSource.getId() == null) {
            newsSource = newsSourceService.save(newsSource);
        }

        news = new org.telegram.bot.domain.entities.News();
        news.setName(name);
        news.setNewsSource(newsSource);
        news.setChat(chat);
        newsService.save(news);
        return buildSendMessageWithText(message, speechService.getRandomMessageByTag("saved"));
    }

    private EditMessageText addNewsSourceForChatByCallback(Message message, Integer userId) {
            log.debug("Empty params. Waiting to continue...");
            CommandWaiting commandWaiting = commandWaitingService.get(message.getChatId(), userId);
            if (commandWaiting == null) {
                commandWaiting = new CommandWaiting();
                commandWaiting.setChatId(message.getChatId());
                commandWaiting.setUserId(userId);
            }
            commandWaiting.setCommandName("set");
            commandWaiting.setIsFinished(false);
            commandWaiting.setTextMessage("установить новости добавить ");
            commandWaitingService.save(commandWaiting);

        List<News> allNewsInChat = newsService.getAll(chatService.get(message.getChatId()));

        return new EditMessageText()
                .setChatId(message.getChatId())
                .setMessageId(message.getMessageId())
                .setParseMode(ParseModes.HTML.getValue())
                .setReplyMarkup(prepareKeyboardWithNews(allNewsInChat))
                .setText(prepareTextOfListNewsSources(allNewsInChat) +
                        "\nНапиши мне имя нового источника новостей и ссылку на рсс-поток через пробел" +
                        "\nНапример: Лента https://lenta.ru/rss/last24");

    }

    private SendMessage deleteNewsSourceForChat(Message message, String command) throws BotException {
        log.debug("Request to delete news resource");

        String params;
        try {
            params = command.substring(27);
        } catch (Exception e) {
            throw new BotException(speechService.getRandomMessageByTag("wrongInput"));
        }

        Chat chat = chatService.get(message.getChatId());
        String responseText;

        try {
            Long newsSourceId = Long.parseLong(params);
            if (newsService.remove(chat, newsSourceId)) {
                responseText = speechService.getRandomMessageByTag("saved");
            } else {
                responseText = speechService.getRandomMessageByTag("wrongInput");
            }
        } catch (Exception e) {
            if (newsService.remove(chat, params)) {
                responseText = speechService.getRandomMessageByTag("saved");
            } else {
                responseText = speechService.getRandomMessageByTag("wrongInput");
            }
        }

        return buildSendMessageWithText(message, responseText);
    }

    private EditMessageText deleteNewsSourceForChatByCallback(Message message, String command) {
        log.debug("Request to delete news resource");
        try {
            newsService.remove(Long.valueOf(command.substring(27)));
        } catch (Exception ignored) {}

        return getNewsSourcesListForChatWithKeyboard(message);
    }

    private SendMessage getNewsSourcesListForChat(Message message) {
        Chat chat = chatService.get(message.getChatId());
        log.debug("Request to list all news sources for chat {}", chat.getChatId());

        List<News> allNewsInChat = newsService.getAll(chat);

        return new SendMessage()
                .setChatId(message.getChatId())
                .setReplyToMessageId(message.getMessageId())
                .setParseMode(ParseModes.HTML.getValue())
                .setText(prepareTextOfListNewsSources(allNewsInChat))
                .setReplyMarkup(prepareKeyboardWithNews(allNewsInChat));
    }

    private EditMessageText getNewsSourcesListForChatWithKeyboard(Message message) {
        Chat chat = chatService.get(message.getChatId());
        log.debug("Request to list all news sources for chat {}", chat.getChatId());
        List<News> allNewsInChat = newsService.getAll(chat);

        return new EditMessageText()
                .setChatId(message.getChatId())
                .setMessageId(message.getMessageId())
                .setParseMode(ParseModes.HTML.getValue())
                .setText(prepareTextOfListNewsSources(allNewsInChat))
                .setReplyMarkup(prepareKeyboardWithNews(allNewsInChat));
    }

    private String prepareTextOfListNewsSources(List<News> allNewsInChat) {
        final StringBuilder buf = new StringBuilder();
        buf.append("<b>Список новостных источников:</b>\n");

        allNewsInChat.forEach(news -> buf
                .append("<a href=\"").append(news.getNewsSource().getUrl()).append("\">")
                .append(news.getName()).append("</a>\n"));

        return buf.toString();
    }

    private InlineKeyboardMarkup prepareKeyboardWithNews(List<News> allNewsInChat) {
        List<List<InlineKeyboardButton>> rows = allNewsInChat.stream().map(news -> {
            List<InlineKeyboardButton> newsRow = new ArrayList<>();

            InlineKeyboardButton newsButton = new InlineKeyboardButton();
            newsButton.setText(Emoji.DELETE.getEmoji() + news.getName());
            newsButton.setCallbackData("Установить новости удалить " + news.getId());

            newsRow.add(newsButton);

            return newsRow;
        }).collect(Collectors.toList());

        List<InlineKeyboardButton> addButtonRow = new ArrayList<>();
        InlineKeyboardButton addButton = new InlineKeyboardButton();
        addButton.setText(Emoji.NEW.getEmoji() + "Добавить");
        addButton.setCallbackData("Установить новости добавить");
        addButtonRow.add(addButton);

        List<InlineKeyboardButton> updateButtonRow = new ArrayList<>();
        InlineKeyboardButton updateButton = new InlineKeyboardButton();
        updateButton.setText(Emoji.UPDATE.getEmoji() + "Обновить");
        updateButton.setCallbackData("Установить новости обновить");
        updateButtonRow.add(updateButton);

        List<InlineKeyboardButton> backButtonRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(Emoji.BACK.getEmoji() + "Установки");
        backButton.setCallbackData("Установить back");
        backButtonRow.add(backButton);

        rows.add(addButtonRow);
        rows.add(updateButtonRow);
        rows.add(backButtonRow);

        return new InlineKeyboardMarkup().setKeyboard(rows);
    }

    private SendMessage buildSendMessageWithText(Message message, String text) {
        return new SendMessage()
                .setChatId(message.getChatId())
                .setReplyToMessageId(message.getMessageId())
                .setParseMode(ParseModes.HTML.getValue())
                .setText(text);
    }
}
