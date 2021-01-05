package org.telegram.bot.domain.commands;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserStats;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.domain.enums.Emoji;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.ChatService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserService;
import org.telegram.bot.services.UserStatsService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import javax.transaction.Transactional;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class Top implements CommandParent<SendMessage> {

    private final Logger log = LoggerFactory.getLogger(Top.class);

    private final UserStatsService userStatsService;
    private final UserService userService;
    private final ChatService chatService;
    private final SpeechService speechService;

    private static final List<String> PARAMS = Arrays.asList("месяц", "все", "всё");

    @Override
    public SendMessage parse(Update update) throws BotException {
        Message message = getMessageFromUpdate(update);
        String textMessage = cutCommandInText(message.getText());
        String responseText;
        Chat chat = chatService.get(message.getChatId());

        //TODO переделать на айди, если пользователь без юзернейма
        if (textMessage == null) {
            responseText = getTopOfUsername(chat, message.getFrom().getUserName());
        } else {
            if (PARAMS.contains(textMessage)) {
                responseText = getTopListOfUsers(chat, textMessage);
            } else {
                responseText = getTopOfUsername(chat, textMessage);
            }
        }

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.enableMarkdown(true);
        sendMessage.setText(responseText);

        return sendMessage;
    }

    public SendMessage getTopByChat(Chat chat) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chat.getChatId().toString());
        sendMessage.enableMarkdown(true);
        sendMessage.setText(getTopListOfUsers(chat, PARAMS.get(0)) + "\nСтатистика за месяц сброшена");

        return sendMessage;
    }

    @Transactional
    private String getTopOfUsername(Chat chat, String username) throws BotException {
        log.debug("Request to get top of user by username {}", username);
        User user = userService.get(username);
        if (user == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.USER_NOT_FOUND));
        }

        HashMap<String, String> fieldsOfStats = new HashMap<>();
        StringBuilder buf = new StringBuilder();
        String valueForSkip = "0";
        UserStats userStats = userStatsService.get(chat, user);
        if (userStats == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.USER_NOT_FOUND));
        }

        fieldsOfStats.put(Emoji.EMAIL.getEmoji() + "Сообщений", userStats.getNumberOfMessages().toString());
        fieldsOfStats.put(Emoji.PICTURE.getEmoji() + "Стикеров", userStats.getNumberOfStickers().toString());
        fieldsOfStats.put(Emoji.CAMERA.getEmoji() + "Изображений", userStats.getNumberOfPhotos().toString());
        fieldsOfStats.put(Emoji.FILM_FRAMES.getEmoji() + "Анимаций", userStats.getNumberOfAnimations().toString());
        fieldsOfStats.put(Emoji.MUSIC.getEmoji() + "Музыки", userStats.getNumberOfAudio().toString());
        fieldsOfStats.put(Emoji.DOCUMENT.getEmoji() + "Документов", userStats.getNumberOfDocuments().toString());
        fieldsOfStats.put(Emoji.MOVIE_CAMERA.getEmoji() + "Видео", userStats.getNumberOfVideos().toString());
        fieldsOfStats.put(Emoji.VHS.getEmoji() + "Видеосообщений", userStats.getNumberOfVideoNotes().toString());
        fieldsOfStats.put(Emoji.PLAY_BUTTON.getEmoji() + "Голосовых", userStats.getNumberOfVoices().toString());
        fieldsOfStats.put(Emoji.ROBOT.getEmoji() + "Команд", userStats.getNumberOfCommands().toString());

        buf.append("*").append(userStats.getUser().getUsername()).append("\n").append("За месяц:*\n");
        fieldsOfStats.forEach((key, value) -> {
            if (!value.equals(valueForSkip)) {
                buf.append(key).append(": ").append(value).append("\n");
            }
        });
        buf.append("-----------------------------\n");

        fieldsOfStats = new HashMap<>();
        buf.append("*Всего:*\n");

        fieldsOfStats.put(Emoji.EMAIL.getEmoji() + "Сообщений", userStats.getNumberOfAllMessages().toString());
        fieldsOfStats.put(Emoji.PICTURE.getEmoji() + "Стикеров", userStats.getNumberOfAllStickers().toString());
        fieldsOfStats.put(Emoji.CAMERA.getEmoji() + "Изображений", userStats.getNumberOfAllPhotos().toString());
        fieldsOfStats.put(Emoji.FILM_FRAMES.getEmoji() + "Анимаций", userStats.getNumberOfAllAnimations().toString());
        fieldsOfStats.put(Emoji.MUSIC.getEmoji() + "Музыки", userStats.getNumberOfAllAudio().toString());
        fieldsOfStats.put(Emoji.DOCUMENT.getEmoji() + "Документов", userStats.getNumberOfAllDocuments().toString());
        fieldsOfStats.put(Emoji.MOVIE_CAMERA.getEmoji() + "Видео", userStats.getNumberOfAllVideos().toString());
        fieldsOfStats.put(Emoji.VHS.getEmoji() + "Видеосообщений", userStats.getNumberOfAllVideoNotes().toString());
        fieldsOfStats.put(Emoji.PLAY_BUTTON.getEmoji() + "Голосовых", userStats.getNumberOfAllVoices().toString());
        fieldsOfStats.put(Emoji.ROBOT.getEmoji() + "Команд", userStats.getNumberOfAllCommands().toString());

        fieldsOfStats.forEach((key, value) -> {
            if (!value.equals(valueForSkip)) {
                buf.append(key).append(": ").append(value).append("\n");
            }
        });

        return buf.toString();
    }

    private String getTopListOfUsers(Chat chat, String param) {
        log.debug("Request to top by {} for chat {}", param, chat);

        StringBuilder responseText = new StringBuilder();
        List<UserStats> userList = userStatsService.getUsersByChat(chat);

        int spacesAfterSerialNumberCount = String.valueOf(userList.size()).length() + 2;
        int spacesAfterNuberOfMessageCount = userList.stream()
                .map(UserStats::getNumberOfMessages)
                .max(Integer::compareTo)
                .orElse(6)
                .toString()
                .length() + 1;

        if (param.equals(PARAMS.get(0))) {
            userList = userList.stream()
                    .filter(userStats -> userStats.getNumberOfMessages() != 0)
                    .sorted(Comparator.comparing(UserStats::getNumberOfMessages).reversed())
                    .collect(Collectors.toList());
        }
        else if (param.equals(PARAMS.get(1)) || param.equals(PARAMS.get(2))) {
            userList = userList.stream()
                    .filter(userStats -> userStats.getNumberOfAllMessages() != 0)
                    .sorted(Comparator.comparing(UserStats::getNumberOfAllMessages).reversed())
                    .collect(Collectors.toList());
        }

        AtomicInteger counter = new AtomicInteger(1);
        responseText.append("*Топ по общению за ").append(param).append(":*\n```\n");
        userList.forEach(userStats -> responseText
                .append(String.format("%-" + spacesAfterSerialNumberCount + "s", counter.getAndIncrement() + ")"))
                .append(String.format("%-" + spacesAfterNuberOfMessageCount + "s", userStats.getNumberOfMessages().toString()))
                .append(userStats.getUser().getUsername()).append("\n"));

        return responseText.append("```").toString();
    }
}
