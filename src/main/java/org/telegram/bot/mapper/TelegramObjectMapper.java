package org.telegram.bot.mapper;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.request.*;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.games.Animation;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.stickers.Sticker;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TelegramObjectMapper {

    private final BotStats botStats;

    public BotRequest toBotRequest(Update update) {
        return new BotRequest()
                .setMessage(getMessage(update));
    }

    private Message getMessage(Update update) {
        String messageText;
        org.telegram.telegrambots.meta.api.objects.User telegramUser;
        MessageKind messageKind;
        org.telegram.telegrambots.meta.api.objects.Message telegramMessage = update.getMessage();
        if (telegramMessage == null) {
            telegramMessage = update.getEditedMessage();
            if (telegramMessage == null) {
                CallbackQuery callbackQuery = update.getCallbackQuery();
                if (callbackQuery == null) {
                    botStats.incrementErrors(update, "Unknown type of receiving update");
                    return null;
                } else {
                    telegramMessage = callbackQuery.getMessage();
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
                .setDateTime(Instant.ofEpochSecond(telegramMessage.getDate()).atZone(ZoneId.systemDefault()).toLocalDateTime())
                .setMessageKind(messageKind)
                .setMessageContentType(messageContent.getKey())
                .setAttachments(messageContent.getValue());
    }

    private Message toMessage(org.telegram.telegrambots.meta.api.objects.Message telegramMessage) {
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
                .setDateTime(Instant.ofEpochSecond(telegramMessage.getDate()).atZone(ZoneId.systemDefault()).toLocalDateTime())
                .setMessageKind(messageKind)
                .setMessageContentType(messageContent.getKey())
                .setAttachments(messageContent.getValue());
    }

    private Pair<MessageContentType, List<Attachment>> toAttachments(org.telegram.telegrambots.meta.api.objects.Message message) {
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
        return photoList.stream().map(this::toAttachment).collect(Collectors.toList());
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

    private Chat toChat(org.telegram.telegrambots.meta.api.objects.Chat chat) {
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
        if (response instanceof TextResponse) {
            return toSendMessage((TextResponse) response);
        } else if (response instanceof EditResponse) {
            return toEditMessageText((EditResponse) response);
        } else if (response instanceof DeleteResponse) {
            return toDeleteMessage((DeleteResponse) response);
        } else if (response instanceof FileResponse) {
            return toMediaMethod((FileResponse) response);
        } else if (response instanceof LocationResponse) {
            return toSendLocation((LocationResponse) response);
        } else {
            botStats.incrementErrors(response, "Unknown type of BotResponse: " + response.getClass());
            throw new IllegalArgumentException("Unknown type of BotResponse: " + response.getClass());
        }

    }

    public SendMessage toSendMessage(TextResponse textResponse) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(textResponse.getChatId());
        sendMessage.setReplyToMessageId(textResponse.getReplyToMessageId());
        sendMessage.setText(textResponse.getText());
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
        EditMessageText editMessageText = new EditMessageText();

        editMessageText.setChatId(editResponse.getChatId());
        editMessageText.setMessageId(editResponse.getEditableMessageId());
        editMessageText.setText(editResponse.getText());
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
                .map(keyboardButtons -> keyboardButtons.stream().map(this::toInlineKeyboardButton).collect(Collectors.toList()))
                .collect(Collectors.toList()));
    }

    private InlineKeyboardButton toInlineKeyboardButton(KeyboardButton keyboardButton) {
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();

        inlineKeyboardButton.setText(keyboardButton.getName());
        inlineKeyboardButton.setCallbackData(keyboardButton.getCallback());

        return inlineKeyboardButton;
    }

    private DeleteMessage toDeleteMessage(DeleteResponse deleteResponse) {
        DeleteMessage deleteMessage = new DeleteMessage();

        deleteMessage.setChatId(deleteResponse.getChatId());
        deleteMessage.setMessageId(deleteResponse.getMessageId());

        return deleteMessage;
    }

    private SendLocation toSendLocation(LocationResponse locationResponse) {
        SendLocation sendLocation = new SendLocation();

        sendLocation.setChatId(locationResponse.getChatId());
        sendLocation.setReplyToMessageId(locationResponse.getReplyToMessageId());
        sendLocation.setLatitude(locationResponse.getLatitude());
        sendLocation.setLongitude(locationResponse.getLongitude());

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
        SendPhoto sendPhoto = new SendPhoto();
        InputFile inputFile = toInputFile(file);

        sendPhoto.setChatId(fileResponse.getChatId());
        sendPhoto.setReplyToMessageId(fileResponse.getReplyToMessageId());
        sendPhoto.setPhoto(inputFile);
        sendPhoto.setCaption(fileResponse.getText());

        ResponseSettings responseSettings = fileResponse.getResponseSettings();
        if (responseSettings != null) {
            if (!responseSettings.isNotification()) {
                sendPhoto.disableNotification();
            }
            if (file.getFileSettings().isSpoiler()) {
                sendPhoto.setHasSpoiler(true);
            }

            sendPhoto.setParseMode(toParseMode(responseSettings.getFormattingStyle()));
        }

        return sendPhoto;
    }

    private SendVideo toSendVideo(FileResponse fileResponse) {
        org.telegram.bot.domain.model.response.File file = fileResponse.getFiles().get(0);
        InputFile inputFile = toInputFile(file);

        SendVideo sendVideo = new SendVideo();
        sendVideo.setChatId(fileResponse.getChatId());
        sendVideo.setReplyToMessageId(fileResponse.getReplyToMessageId());
        sendVideo.setVideo(inputFile);

        if (file.getFileSettings().isSpoiler()) {
            sendVideo.setHasSpoiler(true);
        }

        return sendVideo;
    }

    private SendVoice toSendVoice(FileResponse fileResponse) {
        InputFile inputFile = toInputFile(fileResponse.getFiles().get(0));

        SendVoice sendVoice = new SendVoice();
        sendVoice.setChatId(fileResponse.getChatId());
        sendVoice.setReplyToMessageId(fileResponse.getReplyToMessageId());
        sendVoice.setVoice(inputFile);

        return sendVoice;
    }

    public SendDocument toSendDocument(FileResponse fileResponse) {
        InputFile inputFile = toInputFile(fileResponse.getFiles().get(0));

        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(fileResponse.getChatId());
        sendDocument.setReplyToMessageId(fileResponse.getReplyToMessageId());
        sendDocument.setCaption(fileResponse.getText());
        sendDocument.setDocument(inputFile);

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
            InputMediaPhoto inputMediaPhoto = new InputMediaPhoto();
            inputMediaPhoto.setMedia(file.getUrl());
            inputMediaPhoto.setCaption(file.getName());
            images.add(inputMediaPhoto);
        });

        SendMediaGroup sendMediaGroup = new SendMediaGroup();
        sendMediaGroup.setMedias(images);
        sendMediaGroup.setReplyToMessageId(fileResponse.getReplyToMessageId());
        sendMediaGroup.setChatId(fileResponse.getChatId());

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
