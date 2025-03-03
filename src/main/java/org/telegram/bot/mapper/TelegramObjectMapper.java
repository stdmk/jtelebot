package org.telegram.bot.mapper;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.request.*;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.games.Animation;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.reactions.MessageReactionUpdated;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.stickers.Sticker;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.DateUtils.unixTimeToLocalDateTime;

@Component
@RequiredArgsConstructor
public class TelegramObjectMapper {

    private final BotStats botStats;

    public BotRequest toBotRequest(Update update) {
        Message message;

        MessageReactionUpdated messageReaction = update.getMessageReaction();
        if (messageReaction != null) {
            message = getMessage(messageReaction);
        } else {
            message = getMessage(update);
        }

        return new BotRequest().setMessage(message);
    }

    private Message getMessage(Update update) {
        String messageText;
        org.telegram.telegrambots.meta.api.objects.User telegramUser;
        MessageKind messageKind;
        org.telegram.telegrambots.meta.api.objects.message.Message telegramMessage = update.getMessage();
        if (telegramMessage == null) {
            telegramMessage = update.getEditedMessage();
            if (telegramMessage == null) {
                CallbackQuery callbackQuery = update.getCallbackQuery();
                if (callbackQuery == null) {
                    return null;
                } else {
                    telegramMessage = (org.telegram.telegrambots.meta.api.objects.message.Message) callbackQuery.getMessage();
                    telegramUser = callbackQuery.getFrom();
                    messageText = callbackQuery.getData();
                    messageKind = MessageKind.CALLBACK;
                }
            } else {
                telegramUser = telegramMessage.getFrom();
                messageText = telegramMessage.getText();
                messageKind = MessageKind.EDIT;
            }
        } else {
            telegramUser = telegramMessage.getFrom();
            messageText = telegramMessage.getText();
            messageKind = MessageKind.COMMON;
        }

        if (messageText == null) {
            messageText = telegramMessage.getCaption();
        }

        Pair<MessageContentType, List<Attachment>> messageContent = toAttachments(telegramMessage);

        return new Message()
                .setChat(toChat(telegramMessage.getChat()))
                .setUser(toUser(telegramUser))
                .setMessageId(telegramMessage.getMessageId())
                .setReplyToMessage(toMessage(telegramMessage.getReplyToMessage()))
                .setText(messageText)
                .setDateTime(telegramMessage.getDate() == null ? null : unixTimeToLocalDateTime(telegramMessage.getDate()))
                .setEditDateTime(telegramMessage.getEditDate() == null ? null : unixTimeToLocalDateTime(telegramMessage.getEditDate()))
                .setMessageKind(messageKind)
                .setMessageContentType(messageContent.getKey())
                .setAttachments(messageContent.getValue());
    }

    private Message getMessage(MessageReactionUpdated messageReactionUpdated) {
        return new Message()
                .setChat(toChat(messageReactionUpdated.getChat()))
                .setUser(toUser(messageReactionUpdated.getUser()))
                .setMessageId(messageReactionUpdated.getMessageId())
                .setDateTime(messageReactionUpdated.getDate() == null ? null : unixTimeToLocalDateTime(messageReactionUpdated.getDate()))
                .setMessageKind(MessageKind.COMMON)
                .setMessageContentType(MessageContentType.REACTION)
                .setReactionsCount(messageReactionUpdated.getNewReaction().size() - messageReactionUpdated.getOldReaction().size());
    }

    private Message toMessage(org.telegram.telegrambots.meta.api.objects.message.Message telegramMessage) {
        String messageText;
        org.telegram.telegrambots.meta.api.objects.User telegramUser;
        MessageKind messageKind;
        if (telegramMessage == null) {
            return null;
        } else {
            telegramUser = telegramMessage.getFrom();
            messageText = telegramMessage.getText();
            messageKind = MessageKind.COMMON;
        }

        if (messageText == null) {
            messageText = telegramMessage.getCaption();
        }

        Pair<MessageContentType, List<Attachment>> messageContent = toAttachments(telegramMessage);

        return new Message()
                .setChat(toChat(telegramMessage.getChat()))
                .setUser(toUser(telegramUser))
                .setMessageId(telegramMessage.getMessageId())
                .setText(messageText)
                .setDateTime(telegramMessage.getDate() == null ? null : unixTimeToLocalDateTime(telegramMessage.getDate()))
                .setEditDateTime(telegramMessage.getEditDate() == null ? null : unixTimeToLocalDateTime(telegramMessage.getEditDate()))
                .setMessageKind(messageKind)
                .setMessageContentType(messageContent.getKey())
                .setAttachments(messageContent.getValue());
    }

    private Pair<MessageContentType, List<Attachment>> toAttachments(org.telegram.telegrambots.meta.api.objects.message.Message message) {
        if (message.hasText()) {
            return Pair.of(MessageContentType.TEXT, List.of());
        } else if (message.hasSticker()) {
            return Pair.of(MessageContentType.STICKER, List.of(toAttachment(message.getSticker())));
        } else if (message.hasPhoto()) {
            return Pair.of(MessageContentType.PHOTO, toAttachments(message.getPhoto()));
        } else if (message.hasAnimation()) {
            return Pair.of(MessageContentType.ANIMATION, List.of(toAttachment(message.getAnimation())));
        } else if (message.hasAudio()) {
            return Pair.of(MessageContentType.AUDIO, List.of(toAttachment(message.getAudio())));
        } else if (message.hasDocument()) {
            return Pair.of(MessageContentType.FILE, List.of(toAttachment(message.getDocument())));
        } else if (message.hasVideo()) {
            return Pair.of(MessageContentType.VIDEO, List.of(toAttachment(message.getVideo())));
        } else if (message.hasVideoNote()) {
            return Pair.of(MessageContentType.VIDEO_NOTE, (List.of(toAttachment(message.getVideoNote()))));
        } else if (message.hasVoice()) {
            return Pair.of(MessageContentType.VOICE, List.of(toAttachment(message.getVoice())));
        } else {
            return Pair.of(MessageContentType.UNKNOWN, List.of());
        }

    }

    private Attachment toAttachment(Document document) {
        return new Attachment(document.getMimeType(), document.getFileUniqueId(), document.getFileId(), document.getFileName(), document.getFileSize(), null);
    }

    private List<Attachment> toAttachments(List<PhotoSize> photoList) {
        return photoList.stream().map(this::toAttachment).toList();
    }

    private Attachment toAttachment(PhotoSize photo) {
        return new Attachment(MimeTypeUtils.IMAGE_JPEG_VALUE, photo.getFileUniqueId(), photo.getFileId(), "photo", photo.getFileSize().longValue(), null);
    }

    private Attachment toAttachment(Video video) {
        return new Attachment(video.getMimeType(), video.getFileUniqueId(), video.getFileId(), video.getFileName(), video.getFileSize(), video.getDuration());
    }

    private Attachment toAttachment(VideoNote videoNote) {
         return new Attachment(null, videoNote.getFileUniqueId(), videoNote.getFileId(), "videonote", videoNote.getFileSize().longValue(), videoNote.getDuration());
    }

    private Attachment toAttachment(Voice voice) {
        return new Attachment(voice.getMimeType(), voice.getFileUniqueId(), voice.getFileId(), "voice", voice.getFileSize(), voice.getDuration());
    }

    private Attachment toAttachment(Audio audio) {
        return new Attachment(audio.getMimeType(), audio.getFileUniqueId(), audio.getFileId(), audio.getFileName(), audio.getFileSize(), audio.getDuration());
    }

    private Attachment toAttachment(Animation animation) {
        return new Attachment(animation.getMimetype(), animation.getFileUniqueId(), animation.getFileId(), animation.getFileName(), animation.getFileSize(), animation.getDuration());
    }

    private Attachment toAttachment(Sticker sticker) {
        return toAttachment(null, sticker.getFileUniqueId(), sticker.getFileId(), sticker.getSetName(), sticker.getFileSize().longValue(), null);
    }

    private Attachment toAttachment(String mimeType, String fileUniqueId, String fileId, String name, Long size, Integer duration) {
        return new Attachment(mimeType, fileUniqueId, fileId, name, size, duration);
    }

    private Chat toChat(org.telegram.telegrambots.meta.api.objects.chat.Chat chat) {
        Long chatId = chat.getId();

        String chatName;
        if (chatId > 0) {
            chatName = chat.getUserName();
            if (chatName == null) {
                chatName = chat.getFirstName();
            }
        } else {
            chatName = chat.getTitle();
            if (chatName == null) {
                chatName = "";
            }
        }

        return new Chat()
                .setChatId(chatId)
                .setName(chatName);
    }

    private User toUser(org.telegram.telegrambots.meta.api.objects.User user) {
        String username = user.getUserName();
        if (username == null) {
            username = user.getFirstName();
        }

        return new User()
                .setUserId(user.getId())
                .setUsername(username)
                .setLang(user.getLanguageCode());
    }

    public List<PartialBotApiMethod<?>> toTelegramMethod(List<BotResponse> responseList) {
        return responseList.stream().map(this::toTelegramMethod).collect(Collectors.toList());
    }

    public PartialBotApiMethod<?> toTelegramMethod(BotResponse response) {
        if (response instanceof TextResponse textResponse) {
            return toSendMessage(textResponse);
        } else if (response instanceof EditResponse editResponse) {
            return toEditMessageText(editResponse);
        } else if (response instanceof DeleteResponse deleteResponse) {
            return toDeleteMessage(deleteResponse);
        } else if (response instanceof FileResponse fileResponse) {
            return toMediaMethod(fileResponse);
        } else if (response instanceof LocationResponse locationResponse) {
            return toSendLocation((locationResponse));
        } else {
            botStats.incrementErrors(response, "Unknown type of BotResponse: " + response.getClass());
            throw new IllegalArgumentException("Unknown type of BotResponse: " + response.getClass());
        }

    }

    public SendMessage toSendMessage(TextResponse textResponse) {
        SendMessage sendMessage = new SendMessage(textResponse.getChatId().toString(), textResponse.getText());
        sendMessage.setReplyToMessageId(textResponse.getReplyToMessageId());
        sendMessage.setReplyMarkup(toKeyboard(textResponse.getKeyboard()));

        ResponseSettings responseSettings = textResponse.getResponseSettings();
        if (responseSettings != null) {
            if (!responseSettings.isNotification()) {
                sendMessage.disableNotification();
            }
            if (!responseSettings.isWebPagePreview()) {
                sendMessage.disableWebPagePreview();
            }

            sendMessage.setParseMode(toParseMode(responseSettings.getFormattingStyle()));
        }

        return sendMessage;
    }

    private EditMessageText toEditMessageText(EditResponse editResponse) {
        EditMessageText editMessageText = new EditMessageText(editResponse.getText());

        editMessageText.setChatId(editResponse.getChatId());
        editMessageText.setMessageId(editResponse.getEditableMessageId());
        editMessageText.setReplyMarkup(toKeyboard(editResponse.getKeyboard()));

        ResponseSettings responseSettings = editResponse.getResponseSettings();
        if (responseSettings != null) {
            if (!responseSettings.isWebPagePreview()) {
                editMessageText.disableWebPagePreview();
            }

            editMessageText.setParseMode(toParseMode(responseSettings.getFormattingStyle()));
        }

        return editMessageText;
    }

    private InlineKeyboardMarkup toKeyboard(Keyboard keyboard) {
        if (keyboard == null) {
            return null;
        }

        return new InlineKeyboardMarkup(keyboard.getKeyboardButtonsList()
                .stream()
                .map(this::toInlineKeyboardRow)
                .toList());
    }

    private InlineKeyboardRow toInlineKeyboardRow(List<KeyboardButton> keyboardButtonList) {
        InlineKeyboardRow inlineKeyboardRow = new InlineKeyboardRow(keyboardButtonList.size());
        inlineKeyboardRow.addAll(keyboardButtonList.stream().map(this::toInlineKeyboardButton).toList());
        return inlineKeyboardRow;
    }

    private InlineKeyboardButton toInlineKeyboardButton(KeyboardButton keyboardButton) {
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton(keyboardButton.getName());
        inlineKeyboardButton.setCallbackData(keyboardButton.getCallback());

        return inlineKeyboardButton;
    }

    private DeleteMessage toDeleteMessage(DeleteResponse deleteResponse) {
        return new DeleteMessage(deleteResponse.getChatId().toString(), deleteResponse.getMessageId());
    }

    private SendLocation toSendLocation(LocationResponse locationResponse) {
        SendLocation sendLocation = new SendLocation(locationResponse.getChatId().toString(), locationResponse.getLatitude(), locationResponse.getLongitude());
        sendLocation.setReplyToMessageId(locationResponse.getReplyToMessageId());

        return sendLocation;
    }

    private PartialBotApiMethod<?> toMediaMethod(FileResponse fileResponse) {
        List<org.telegram.bot.domain.model.response.File> files = fileResponse.getFiles();
        if (files.size() > 1) {
            return toSendMediaGroup(fileResponse);
        } else if (files.size() == 1) {
            org.telegram.bot.domain.model.response.File file = files.get(0);
            if (FileType.IMAGE.equals(file.getFileType())) {
                return toSendPhoto(fileResponse);
            } else if (FileType.VIDEO.equals(file.getFileType())) {
                return toSendVideo(fileResponse);
            } else if (FileType.VOICE.equals(file.getFileType())) {
                return toSendVoice(fileResponse);
            } else {
                return toSendDocument(fileResponse);
            }
        } else {
            botStats.incrementErrors(fileResponse, "file response without files!");
            throw new IllegalArgumentException("file response without files!");
        }
    }

    private SendPhoto toSendPhoto(FileResponse fileResponse) {
        org.telegram.bot.domain.model.response.File file = fileResponse.getFiles().get(0);

        SendPhoto sendPhoto = new SendPhoto(fileResponse.getChatId().toString(), toInputFile(file));
        sendPhoto.setReplyToMessageId(fileResponse.getReplyToMessageId());
        sendPhoto.setCaption(fileResponse.getText());

        ResponseSettings responseSettings = fileResponse.getResponseSettings();
        if (responseSettings != null) {
            if (!responseSettings.isNotification()) {
                sendPhoto.disableNotification();
            }

            sendPhoto.setParseMode(toParseMode(responseSettings.getFormattingStyle()));
        }
        if (file.getFileSettings().isSpoiler()) {
            sendPhoto.setHasSpoiler(true);
        }

        return sendPhoto;
    }

    private SendVideo toSendVideo(FileResponse fileResponse) {
        org.telegram.bot.domain.model.response.File file = fileResponse.getFiles().get(0);
        InputFile inputFile = toInputFile(file);

        SendVideo sendVideo = new SendVideo(fileResponse.getChatId().toString(), inputFile);
        sendVideo.setReplyToMessageId(fileResponse.getReplyToMessageId());

        if (file.getFileSettings().isSpoiler()) {
            sendVideo.setHasSpoiler(true);
        }

        return sendVideo;
    }

    private SendVoice toSendVoice(FileResponse fileResponse) {
        InputFile inputFile = toInputFile(fileResponse.getFiles().get(0));

        SendVoice sendVoice = new SendVoice(fileResponse.getChatId().toString(), inputFile);
        sendVoice.setReplyToMessageId(fileResponse.getReplyToMessageId());

        return sendVoice;
    }

    public SendDocument toSendDocument(FileResponse fileResponse) {
        InputFile inputFile = toInputFile(fileResponse.getFiles().get(0));

        SendDocument sendDocument = new SendDocument(fileResponse.getChatId().toString(), inputFile);
        sendDocument.setReplyToMessageId(fileResponse.getReplyToMessageId());
        sendDocument.setCaption(fileResponse.getText());

        ResponseSettings responseSettings = fileResponse.getResponseSettings();
        if (responseSettings != null) {
            if (!responseSettings.isNotification()) {
                sendDocument.disableNotification();
            }
            
            sendDocument.setParseMode(toParseMode(responseSettings.getFormattingStyle()));
        }

        return sendDocument;
    }

    private InputFile toInputFile(org.telegram.bot.domain.model.response.File file) {
        if (file.getFileId() != null) {
            return new InputFile(file.getFileId());
        } else if (file.getUrl() != null) {
            return new InputFile(file.getUrl());
        } else if (file.getInputStream() != null) {
            return new InputFile(file.getInputStream(), file.getName());
        } else if (file.getDiskFile() != null) {
            return new InputFile(file.getDiskFile());
        } else {
            botStats.incrementErrors(file, "unable to map file");
            throw new IllegalArgumentException("Unknown type of File: ");
        }
    }

    private SendMediaGroup toSendMediaGroup(FileResponse fileResponse) {
        List<org.telegram.bot.domain.model.response.File> files = fileResponse.getFiles();
        List<InputMedia> images = new ArrayList<>(files.size());

        files.forEach(file -> {
            InputMediaPhoto inputMediaPhoto = new InputMediaPhoto(file.getUrl());
            inputMediaPhoto.setCaption(file.getName());
            inputMediaPhoto.setHasSpoiler(file.getFileSettings().isSpoiler());
            images.add(inputMediaPhoto);
        });

        SendMediaGroup sendMediaGroup = new SendMediaGroup(fileResponse.getChatId().toString(), images);
        sendMediaGroup.setMedias(images);
        sendMediaGroup.setReplyToMessageId(fileResponse.getReplyToMessageId());

        return sendMediaGroup;
    }

    private String toParseMode(FormattingStyle formattingStyle) {
        if (FormattingStyle.HTML.equals(formattingStyle)) {
            return ParseMode.HTML;
        } else if (FormattingStyle.MARKDOWN.equals(formattingStyle)) {
            return ParseMode.MARKDOWN;
        } else if (FormattingStyle.MARKDOWN2.equals(formattingStyle)) {
            return ParseMode.MARKDOWNV2;
        } else {
            return null;
        }
    }

}
