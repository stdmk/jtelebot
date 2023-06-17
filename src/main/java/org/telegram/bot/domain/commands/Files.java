package org.telegram.bot.domain.commands;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.CommandParent;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandWaiting;
import org.telegram.bot.domain.entities.File;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.enums.BotSpeechTag;
import org.telegram.bot.domain.enums.Emoji;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.FileService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.TextUtils;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.DateUtils.formatDate;
import static org.telegram.bot.utils.TextUtils.formatFileSize;
import static org.telegram.bot.utils.TextUtils.getLinkToUser;

@Component
@RequiredArgsConstructor
public class Files implements CommandParent<PartialBotApiMethod<?>> {

    private final FileService fileService;
    private final CommandWaitingService commandWaitingService;
    private final SpeechService speechService;

    private final static String CALLBACK_COMMAND = "files ";
    private final static String SELECT_FILE_COMMAND = "s";
    private final static String SELECT_PAGE = "p";
    private final static String CALLBACK_SELECT_FILE_COMMAND = CALLBACK_COMMAND + SELECT_FILE_COMMAND;
    private final static String DELETE_FILE_COMMAND = "d";
    private final static String CALLBACK_DELETE_FILE_COMMAND = CALLBACK_COMMAND + DELETE_FILE_COMMAND;
    private final static String ADD_FILE_COMMAND = "a";
    private final static String CALLBACK_ADD_FILE_COMMAND = CALLBACK_COMMAND + ADD_FILE_COMMAND;
    private final static String OPEN_FILE_COMMAND = "o";
    private final static String CALLBACK_OPEN_FILE_COMMAND = CALLBACK_COMMAND + OPEN_FILE_COMMAND;
    private final static String MAKE_DIR_COMMAND = "m";
    private final static String CALLBACK_MAKE_DIR_COMMAND = CALLBACK_COMMAND + MAKE_DIR_COMMAND;

    private final Long ROOT_DIR_ID = 0L;

    @Override
    public PartialBotApiMethod<?> parse(Update update) {
        Message message = getMessageFromUpdate(update);
        Chat chat = new Chat().setChatId(message.getChatId());
        String textMessage;
        boolean callback = false;
        String EMPTY_COMMAND = "files";

        CommandWaiting commandWaiting = commandWaitingService.get(chat, new User().setUserId(message.getFrom().getId()));

        if (commandWaiting != null) {
            String text = message.getText();
            if (text == null) {
                text = "";
            }
            textMessage = cutCommandInText(commandWaiting.getTextMessage()) + text;
        } else {
            if (update.hasCallbackQuery()) {
                commandWaiting = commandWaitingService.get(chat, new User().setUserId(update.getCallbackQuery().getFrom().getId()));
                CallbackQuery callbackQuery = update.getCallbackQuery();
                textMessage = cutCommandInText(callbackQuery.getData());
                callback = true;
            } else {
                textMessage = cutCommandInText(message.getText());
            }
        }

        if (callback) {
            User user = new User().setUserId(update.getCallbackQuery().getFrom().getId());

            if (textMessage.equals(EMPTY_COMMAND)) {
                return selectDirectory(message, chat, false, 0, null);
            } else if (textMessage.startsWith(SELECT_FILE_COMMAND)) {
                commandWaitingService.remove(commandWaiting);
                return selectFileByCallback(message, chat, textMessage);
            } else if (textMessage.startsWith(DELETE_FILE_COMMAND)) {
                return deleteFileByCallback(message, chat, user, textMessage);
            } else if (textMessage.startsWith(ADD_FILE_COMMAND)) {
                return addFileByCallback(message, chat, user, textMessage);
            } else if (textMessage.startsWith(OPEN_FILE_COMMAND)) {
                return sendFile(chat, textMessage);
            } else if (textMessage.startsWith(MAKE_DIR_COMMAND)) {
                return makeDirByCallback(message, chat, user, textMessage);
            }
        }

        User user = new User().setUserId(message.getFrom().getId());
        if (textMessage == null || textMessage.equals(EMPTY_COMMAND)) {
            return selectDirectory(message,  chat, true, 0, null);
        } else if (textMessage.startsWith(ADD_FILE_COMMAND)) {
            return addFiles(message, chat, user, textMessage, commandWaiting);
        } else if (textMessage.startsWith(MAKE_DIR_COMMAND)) {
            commandWaitingService.remove(commandWaiting);
            return makeDir(message, chat, user, textMessage);
        } else {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }
    }

    private SendDocument sendFile(Chat chat, String textCommand) throws BotException {
        long fileId;
        try {
            fileId = Long.parseLong(textCommand.substring(OPEN_FILE_COMMAND.length()));
        } catch (NumberFormatException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        File file = fileService.get(fileId);
        if (file == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        if (!chat.getChatId().equals(file.getChat().getChatId())) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NOT_OWNER));
        }

        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(chat.getChatId().toString());
        sendDocument.setDocument(new InputFile(file.getFileId()));

        return sendDocument;
    }

    private PartialBotApiMethod<?> addFiles(Message message, Chat chat, User user, String textCommand, CommandWaiting commandWaiting) throws BotException {
        boolean audio;

        if (message.hasDocument()) {
            audio = false;
        } else if (message.hasAudio()) {
            audio = true;
        } else {
            commandWaitingService.remove(commandWaiting);
            return null;
        }

        File parent;
        try {
            parent = fileService.get(Long.parseLong(textCommand.substring(ADD_FILE_COMMAND.length()).trim()));
        } catch (NumberFormatException e) {
            commandWaitingService.remove(commandWaiting);
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        if (parent == null) {
            commandWaitingService.remove(commandWaiting);
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        File file;
        if (!audio) {
            file = mapDocumentToFile(message.getDocument(), chat, user, parent);
        } else {
            file = mapAudioToFile(message.getAudio(), chat, user, parent);
        }

        fileService.save(file);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText(speechService.getRandomMessageByTag(BotSpeechTag.SAVED));
        sendMessage.setReplyMarkup(buildCancelAddingFilesKeyboard(parent));

        return sendMessage;
    }

    private PartialBotApiMethod<?> makeDir(Message message, Chat chat, User user, String textCommand) throws BotException {
        textCommand = textCommand.trim();

        File parent;
        String dirName;
        try {
            int i = textCommand.indexOf(" ");
            parent = fileService.get(Long.parseLong(textCommand.substring(ADD_FILE_COMMAND.length(), i)));
            dirName = textCommand.substring(i + 1);
        } catch (NumberFormatException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        if (parent == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        File file = new File();

        file.setName(dirName);
        file.setChat(chat);
        file.setUser(user);
        file.setDate(LocalDateTime.now());
        file.setParentId(parent.getId());

        fileService.save(file);

        return selectDirectory(message, chat, true, 0, parent);
    }

    private EditMessageText makeDirByCallback(Message message, Chat chat, User user, String textCommand) {
        commandWaitingService.add(chat, user, Files.class, CALLBACK_COMMAND + textCommand);

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.setText("\nТеперь напиши мне имя папки");

        return editMessageText;
    }

    private SendMessage addFileByCallback(Message message, Chat chat, User user, String textCommand) {
        commandWaitingService.add(chat, user, Files.class, CALLBACK_COMMAND + textCommand);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setText("\nТеперь пришли мне необходимые файлы");

        return sendMessage;
    }

    private EditMessageText deleteFileByCallback(Message message, Chat chat, User user, String textCommand) throws BotException {
        long fileId;
        try {
            fileId = Long.parseLong(textCommand.substring(DELETE_FILE_COMMAND.length()));
        } catch (NumberFormatException e) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        File file = fileService.get(fileId);
        if (file == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        if (!user.getUserId().equals(file.getUser().getUserId())) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NOT_OWNER));
        }

        fileService.remove(chat, file);

        File dir = fileService.get(file.getParentId());
        if (dir == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        return (EditMessageText) selectDirectory(message, chat, false, 0, dir);
    }

    private EditMessageText selectFileByCallback(Message message, Chat chat, String textCommand) throws BotException {
        int page = 0;
        long fileId;
        textCommand = textCommand.trim();
        try {
            fileId = Long.parseLong(textCommand.substring(SELECT_FILE_COMMAND.length()));
        } catch (NumberFormatException e) {
            try {
                fileId = Long.parseLong(textCommand.substring(textCommand.indexOf(SELECT_FILE_COMMAND) + SELECT_FILE_COMMAND.length(), textCommand.indexOf(" ")));
                page = Integer.parseInt(textCommand.substring(textCommand.indexOf(SELECT_PAGE) + SELECT_PAGE.length()));
            } catch (NumberFormatException en) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
            }
        }

        File file = fileService.get(fileId);
        if (file == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        } else if (file.getType() == null) {
            return (EditMessageText) selectDirectory(message, chat, false, page, file);
        }

        String fileInfo = "<b>" + file.getName() + "</b>\n" +
                            "Автор: " + getLinkToUser(file.getUser(), true) + "\n" +
                            "Создан: " + formatDate(file.getDate()) + "\n" +
                            "Тип: " + file.getType() + "\n" +
                            "Размер: " + formatFileSize(file.getSize()) + "\n";

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.setText(fileInfo);
        editMessageText.enableHtml(true);
        editMessageText.setReplyMarkup(getFileManagingKeyboard(file));

        return editMessageText;
    }

    private InlineKeyboardMarkup getFileManagingKeyboard(File file) {
        List<List<InlineKeyboardButton>> fileManagingRows = new ArrayList<>();
        List<InlineKeyboardButton> managingRow = new ArrayList<>();

        InlineKeyboardButton downloadButton = new InlineKeyboardButton();
        downloadButton.setText(Emoji.DOWN_ARROW.getEmoji());
        downloadButton.setCallbackData(CALLBACK_OPEN_FILE_COMMAND + file.getId());

        InlineKeyboardButton deleteButton = new InlineKeyboardButton();
        deleteButton.setText(Emoji.DELETE.getEmoji());
        deleteButton.setCallbackData(CALLBACK_DELETE_FILE_COMMAND + file.getId());

        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText(Emoji.BACK.getEmoji());
        backButton.setCallbackData(CALLBACK_SELECT_FILE_COMMAND + file.getParentId());

        managingRow.add(downloadButton);
        managingRow.add(deleteButton);
        managingRow.add(backButton);

        fileManagingRows.add(managingRow);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(fileManagingRows);

        return inlineKeyboardMarkup;
    }

    private PartialBotApiMethod<?> selectDirectory(Message message, Chat chat, boolean newMessage, int page, File directory) throws BotException {
        if (directory == null) {
            directory = fileService.get(ROOT_DIR_ID);
            if (directory == null) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
            }
        }

        Page<File> fileList = fileService.get(chat, directory, page);
        List<List<InlineKeyboardButton>> dirContent = new ArrayList<>();

        if (fileList.isEmpty() && !directory.getId().equals(ROOT_DIR_ID)) {
            List<InlineKeyboardButton> fileRow = new ArrayList<>();

            InlineKeyboardButton deleteEmptyDirButton = new InlineKeyboardButton();
            deleteEmptyDirButton.setText(Emoji.DELETE.getEmoji() + "Удалить папку");
            deleteEmptyDirButton.setCallbackData(CALLBACK_DELETE_FILE_COMMAND + directory.getId());

            fileRow.add(deleteEmptyDirButton);
            dirContent.add(fileRow);
        } else {
            dirContent = fileList.stream().map(file -> {
                List<InlineKeyboardButton> fileRow = new ArrayList<>();

                InlineKeyboardButton fileButton = new InlineKeyboardButton();

                String fileName = EmojiMimeType.getEmojiByType(file.getType()) + file.getName();
                fileButton.setText(TextUtils.cutIfLongerThan(fileName, 30));
                fileButton.setCallbackData(CALLBACK_SELECT_FILE_COMMAND + file.getId());

                fileRow.add(fileButton);

                return fileRow;
            }).collect(Collectors.toList());

            List<InlineKeyboardButton> pagesRow = new ArrayList<>();
            if (page > 0) {
                InlineKeyboardButton backButton = new InlineKeyboardButton();
                backButton.setText(Emoji.LEFT_ARROW.getEmoji());
                backButton.setCallbackData(CALLBACK_COMMAND + SELECT_FILE_COMMAND + directory.getId() + " " + SELECT_PAGE + (page - 1));

                pagesRow.add(backButton);
            }

            int totalPages = fileList.getTotalPages();
            if (page + 1 < totalPages && totalPages > 1) {
                InlineKeyboardButton forwardButton = new InlineKeyboardButton();
                forwardButton.setText(Emoji.RIGHT_ARROW.getEmoji());
                forwardButton.setCallbackData(CALLBACK_COMMAND + SELECT_FILE_COMMAND + directory.getId() + " " + SELECT_PAGE + (page + 1));

                pagesRow.add(forwardButton);
            }

            dirContent.add(pagesRow);
        }

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(addingMainRows(dirContent, directory));

        if (newMessage) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId().toString());
            sendMessage.setReplyToMessageId(message.getMessageId());
            sendMessage.setText("Папка: <b>" + directory.getName() + "</b>\n");
            sendMessage.enableHtml(true);
            sendMessage.setReplyMarkup(inlineKeyboardMarkup);

            return sendMessage;
        }

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(message.getChatId().toString());
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.setText("Папка: <b>" + directory.getName() + "</b>\n");
        editMessageText.enableHtml(true);
        editMessageText.setReplyMarkup(inlineKeyboardMarkup);

        return editMessageText;
    }

    private List<List<InlineKeyboardButton>> addingMainRows(List<List<InlineKeyboardButton>> rows, File parent) {
        List<InlineKeyboardButton> addButtonRow = new ArrayList<>();

        InlineKeyboardButton addButton = new InlineKeyboardButton();
        addButton.setText(Emoji.NEW.getEmoji() + "Файл");
        addButton.setCallbackData(CALLBACK_ADD_FILE_COMMAND + parent.getId());

        InlineKeyboardButton newDirButton = new InlineKeyboardButton();
        newDirButton.setText(Emoji.NEW.getEmoji() + "Папка");
        newDirButton.setCallbackData(CALLBACK_MAKE_DIR_COMMAND + parent.getId());

        addButtonRow.add(addButton);
        addButtonRow.add(newDirButton);

        List<InlineKeyboardButton> managingRow = new ArrayList<>();

        InlineKeyboardButton updateButton = new InlineKeyboardButton();
        updateButton.setText(Emoji.UPDATE.getEmoji() + "Обновить");
        updateButton.setCallbackData(CALLBACK_SELECT_FILE_COMMAND + parent.getId());

        if (!parent.getId().equals(ROOT_DIR_ID)) {
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText(Emoji.BACK.getEmoji() + "Вверх");
            backButton.setCallbackData(CALLBACK_SELECT_FILE_COMMAND + parent.getParentId());
            managingRow.add(backButton);
        }

        rows.add(addButtonRow);
        rows.add(managingRow);

        return rows;
    }

    private InlineKeyboardMarkup buildCancelAddingFilesKeyboard(File file) {
        Long parentId = file.getParentId();
        if (parentId == null) {
            parentId = ROOT_DIR_ID;
        }

        List<List<InlineKeyboardButton>> cancelRows = new ArrayList<>();
        List<InlineKeyboardButton> cancelRow = new ArrayList<>();

        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText(Emoji.CHECK_MARK.getEmoji() + "Готово");
        cancelButton.setCallbackData(CALLBACK_SELECT_FILE_COMMAND + parentId);

        cancelRow.add(cancelButton);
        cancelRows.add(cancelRow);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(cancelRows);

        return inlineKeyboardMarkup;
    }

    private File mapDocumentToFile(Document document, Chat chat, User user, File parent) {
        File file = new File();

        file.setFileId(document.getFileId());
        file.setFileUniqueId(document.getFileUniqueId());
        file.setName(document.getFileName());
        file.setType(document.getMimeType());
        file.setSize(document.getFileSize());
        file.setChat(chat);
        file.setUser(user);
        file.setDate(LocalDateTime.now());
        file.setParentId(parent.getId());

        return file;
    }

    private File mapAudioToFile(Audio audio, Chat chat, User user, File parent) {
        File file = new File();

        file.setFileId(audio.getFileId());
        file.setFileUniqueId(audio.getFileUniqueId());
        file.setName(audio.getFileName());
        file.setType(audio.getMimeType());
        file.setSize(audio.getFileSize());
        file.setChat(chat);
        file.setUser(user);
        file.setDate(LocalDateTime.now());
        file.setParentId(parent.getId());

        return file;
    }

    @RequiredArgsConstructor
    @Getter
    public enum EmojiMimeType {
        HEADPHONE("audio", Emoji.HEADPHONE),
        PICTURE("image", Emoji.PICTURE),
        CLIPBOARD("text", Emoji.CLIPBOARD),
        MOVIE_CAMERA("video", Emoji.MOVIE_CAMERA),
        UNKNOWN("", Emoji.MEMO),
        ;

        private final String type;
        private final Emoji emoji;

        public static String getEmojiByType(String mimeType) {
            if (mimeType == null) {
                return Emoji.FOLDER.getEmoji();
            }

            return Arrays.stream(EmojiMimeType.values())
                    .filter(emojiMimeType -> mimeType.startsWith(emojiMimeType.getType()))
                    .findFirst()
                    .orElse(UNKNOWN)
                    .getEmoji()
                    .getEmoji();
        }
    }
}
