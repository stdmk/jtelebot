package org.telegram.bot.commands;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandWaiting;
import org.telegram.bot.domain.entities.File;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.request.Attachment;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.request.MessageContentType;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.Emoji;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.FileService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.TextUtils;

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
@Slf4j
public class Files implements Command {

    private final Bot bot;
    private final BotStats botStats;
    private final FileService fileService;
    private final CommandWaitingService commandWaitingService;
    private final SpeechService speechService;

    private static final String EMPTY_COMMAND = "files";
    private static final String CALLBACK_COMMAND = "files ";
    private static final String SELECT_FILE_COMMAND = "s";
    private static final String SELECT_PAGE = "p";
    private static final String CALLBACK_SELECT_FILE_COMMAND = CALLBACK_COMMAND + SELECT_FILE_COMMAND;
    private static final String DELETE_FILE_COMMAND = "d";
    private static final String CALLBACK_DELETE_FILE_COMMAND = CALLBACK_COMMAND + DELETE_FILE_COMMAND;
    private static final String ADD_FILE_COMMAND = "a";
    private static final String CALLBACK_ADD_FILE_COMMAND = CALLBACK_COMMAND + ADD_FILE_COMMAND;
    private static final String OPEN_FILE_COMMAND = "o";
    private static final String CALLBACK_OPEN_FILE_COMMAND = CALLBACK_COMMAND + OPEN_FILE_COMMAND;
    private static final String MAKE_DIR_COMMAND = "m";
    private static final String CALLBACK_MAKE_DIR_COMMAND = CALLBACK_COMMAND + MAKE_DIR_COMMAND;

    private static final Long ROOT_DIR_ID = 0L;

    @Override
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        Chat chat = message.getChat();
        User user = message.getUser();

        CommandWaiting commandWaiting = commandWaitingService.get(chat, user);

        String commandArgument;
        if (commandWaiting != null) {
            commandArgument = TextUtils.cutCommandInText(commandWaiting.getTextMessage());
            if (message.hasText()) {
                commandArgument = commandArgument + message.getText();
            }
        } else {
            commandArgument = message.getCommandArgument();
        }

        if (message.isCallback()) {
            return returnResponse(getResponseForCallback(request, commandWaiting, commandArgument));
        }

        return returnResponse(getResponse(message, commandWaiting, commandArgument));
    }

    private BotResponse getResponseForCallback(BotRequest request, CommandWaiting commandWaiting, String textMessage) {
        Message message = request.getMessage();
        Chat chat = message.getChat();
        User user = message.getUser();

        if (textMessage.equals(EMPTY_COMMAND)) {
            bot.sendTyping(message.getChatId());
            return selectDirectory(message, chat, false, 0, null);
        } else if (textMessage.startsWith(SELECT_FILE_COMMAND)) {
            bot.sendTyping(message.getChatId());
            commandWaitingService.remove(commandWaiting);
            return selectFileByCallback(message, chat, textMessage);
        } else if (textMessage.startsWith(DELETE_FILE_COMMAND)) {
            bot.sendTyping(message.getChatId());
            return deleteFileByCallback(message, chat, user, textMessage);
        } else if (textMessage.startsWith(ADD_FILE_COMMAND)) {
            bot.sendTyping(message.getChatId());
            return addFileByCallback(message, chat, user, textMessage);
        } else if (textMessage.startsWith(OPEN_FILE_COMMAND)) {
            bot.sendUploadDocument(message.getChatId());
            return sendFile(message, textMessage);
        } else if (textMessage.startsWith(MAKE_DIR_COMMAND)) {
            bot.sendTyping(message.getChatId());
            return makeDirByCallback(message, chat, user, textMessage);
        }

        log.error("Unexpected callback request {}", textMessage);
        botStats.incrementErrors(request, "Unexpected callback request " + textMessage);
        throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
    }

    private BotResponse getResponse(Message message, CommandWaiting commandWaiting, String textMessage) {
        bot.sendTyping(message.getChatId());

        Chat chat = message.getChat();
        User user = message.getUser();
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

    private FileResponse sendFile(Message message, String textCommand) throws BotException {
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

        Long chatId = message.getChatId();
        if (!chatId.equals(file.getChat().getChatId())) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.NOT_OWNER));
        }

        return new FileResponse(message)
                .addFile(new org.telegram.bot.domain.model.response.File(file.getFileId()));
    }

    private TextResponse addFiles(Message message, Chat chat, User user, String textCommand, CommandWaiting commandWaiting) throws BotException {
        if (!MessageContentType.FILE.equals(message.getMessageContentType()) && !MessageContentType.AUDIO.equals(message.getMessageContentType())) {
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

        fileService.save(mapToFile(message.getAttachments().get(0), chat, user, parent));

        return new TextResponse(message)
                .setText(speechService.getRandomMessageByTag(BotSpeechTag.SAVED))
                .setKeyboard(buildCancelAddingFilesKeyboard(parent));
    }

    private BotResponse makeDir(Message message, Chat chat, User user, String textCommand) throws BotException {
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

    private EditResponse makeDirByCallback(Message message, Chat chat, User user, String textCommand) {
        commandWaitingService.add(chat, user, Files.class, CALLBACK_COMMAND + textCommand);
        return new EditResponse(message)
                .setText("${command.files.commandwaitingfolderstart}");
    }

    private TextResponse addFileByCallback(Message message, Chat chat, User user, String textCommand) {
        commandWaitingService.add(chat, user, Files.class, CALLBACK_COMMAND + textCommand);
        return new TextResponse(message)
                .setText("${command.files.commandwaitingfilesstart}");
    }

    private EditResponse deleteFileByCallback(Message message, Chat chat, User user, String textCommand) throws BotException {
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

        return (EditResponse) selectDirectory(message, chat, false, 0, dir);
    }

    private EditResponse selectFileByCallback(Message message, Chat chat, String textCommand) throws BotException {
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
            return (EditResponse) selectDirectory(message, chat, false, page, file);
        }

        String fileInfo = "<b>" + file.getName() + "</b>\n" +
                            "${command.files.fileinfo.author}: " + getLinkToUser(file.getUser(), true) + "\n" +
                            "${command.files.fileinfo.created}: " + formatDate(file.getDate()) + "\n" +
                            "${command.files.fileinfo.type}: " + file.getType() + "\n" +
                            "${command.files.fileinfo.size}: " + formatFileSize(file.getSize()) + "\n";

        return new EditResponse(message)
                .setText(fileInfo)
                .setKeyboard(getFileManagingKeyboard(file))
                .setResponseSettings(FormattingStyle.HTML);
    }

    private Keyboard getFileManagingKeyboard(File file) {
        return new Keyboard().setKeyboardButtonsList(List.of(
                new KeyboardButton()
                        .setName(Emoji.DOWN_ARROW.getSymbol())
                        .setCallback(CALLBACK_OPEN_FILE_COMMAND + file.getId()),
                new KeyboardButton()
                        .setName(Emoji.DELETE.getSymbol())
                        .setCallback(CALLBACK_DELETE_FILE_COMMAND + file.getId()),
                new KeyboardButton()
                        .setName(Emoji.BACK.getSymbol())
                        .setCallback(CALLBACK_SELECT_FILE_COMMAND + file.getParentId())));
    }

    private BotResponse selectDirectory(Message message, Chat chat, boolean newMessage, int page, File directory) throws BotException {
        if (directory == null) {
            directory = fileService.get(ROOT_DIR_ID);
            if (directory == null) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
            }
        }

        Page<File> fileList = fileService.get(chat, directory, page);
        List<List<KeyboardButton>> dirContent = new ArrayList<>();

        if (fileList.isEmpty() && !directory.getId().equals(ROOT_DIR_ID)) {
            List<KeyboardButton> fileRow = List.of(new KeyboardButton()
                    .setName(Emoji.DELETE.getSymbol() + "${command.files.folder.remove}")
                    .setCallback(CALLBACK_DELETE_FILE_COMMAND + directory.getId()));
            dirContent.add(fileRow);
        } else {
            dirContent = fileList
                    .stream()
                    .map(file -> List.of(new KeyboardButton()
                            .setName(TextUtils.cutIfLongerThan(EmojiMimeType.getEmojiByType(file.getType()) + file.getName(), 30))
                            .setCallback(CALLBACK_SELECT_FILE_COMMAND + file.getId())))
                    .collect(Collectors.toList());
            List<KeyboardButton> pagesRow = new ArrayList<>(2);
            if (page > 0) {
                pagesRow.add(new KeyboardButton()
                        .setName(Emoji.LEFT_ARROW.getSymbol())
                        .setCallback(CALLBACK_COMMAND + SELECT_FILE_COMMAND + directory.getId() + " " + SELECT_PAGE + (page - 1)));
            }

            int totalPages = fileList.getTotalPages();
            if (page + 1 < totalPages && totalPages > 1) {
                pagesRow.add(new KeyboardButton()
                        .setName(Emoji.RIGHT_ARROW.getSymbol())
                        .setCallback(CALLBACK_COMMAND + SELECT_FILE_COMMAND + directory.getId() + " " + SELECT_PAGE + (page + 1)));
            }

            dirContent.add(pagesRow);
        }

        Keyboard keyboard = new Keyboard(addingMainRows(dirContent, directory));

        if (newMessage) {
            return new TextResponse(message)
                    .setText("${command.files.folder.caption}: <b>" + directory.getName() + "</b>\n")
                    .setKeyboard(keyboard)
                    .setResponseSettings(FormattingStyle.HTML);
        }

        return new EditResponse(message)
                .setText("${command.files.folder.caption}: <b>" + directory.getName() + "</b>\n")
                .setKeyboard(keyboard)
                .setResponseSettings(FormattingStyle.HTML);
    }

    private List<List<KeyboardButton>> addingMainRows(List<List<KeyboardButton>> rows, File parent) {
        List<KeyboardButton> addButtonRow = List.of(
                new KeyboardButton()
                        .setName(Emoji.NEW.getSymbol() + "${command.files.file.caption}")
                        .setCallback(CALLBACK_ADD_FILE_COMMAND + parent.getId()),
                new KeyboardButton()
                        .setName(Emoji.NEW.getSymbol() + "${command.files.folder.caption}")
                        .setCallback(CALLBACK_MAKE_DIR_COMMAND + parent.getId()));

        List<KeyboardButton> managingRow = new ArrayList<>();

        managingRow.add(new KeyboardButton()
                .setName(Emoji.UPDATE.getSymbol() + "${command.files.button.reload}")
                .setCallback(CALLBACK_SELECT_FILE_COMMAND + parent.getId()));

        if (!parent.getId().equals(ROOT_DIR_ID)) {
            managingRow.add(new KeyboardButton()
                    .setName(Emoji.BACK.getSymbol() + "${command.files.button.up}")
                    .setCallback(CALLBACK_SELECT_FILE_COMMAND + parent.getParentId()));
        }

        rows.add(addButtonRow);
        rows.add(managingRow);

        return rows;
    }

    private Keyboard buildCancelAddingFilesKeyboard(File file) {
        Long parentId = file.getParentId();
        if (parentId == null) {
            parentId = ROOT_DIR_ID;
        }

        return new Keyboard(new KeyboardButton()
                .setName(Emoji.CHECK_MARK.getSymbol() + "${command.files.button.done}")
                .setCallback(CALLBACK_SELECT_FILE_COMMAND + parentId));
    }

    private File mapToFile(Attachment attachment, Chat chat, User user, File parent) {
        return new File()
                .setFileId(attachment.getFileId())
                .setFileUniqueId(attachment.getFileUniqueId())
                .setName(attachment.getName())
                .setType(attachment.getMimeType())
                .setSize(attachment.getSize())
                .setChat(chat)
                .setUser(user)
                .setDate(LocalDateTime.now())
                .setParentId(parent.getId());
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
                return Emoji.FOLDER.getSymbol();
            }

            return Arrays.stream(EmojiMimeType.values())
                    .filter(emojiMimeType -> mimeType.startsWith(emojiMimeType.getType()))
                    .findFirst()
                    .orElse(UNKNOWN)
                    .getEmoji()
                    .getSymbol();
        }
    }
}
