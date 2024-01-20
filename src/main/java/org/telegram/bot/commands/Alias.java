package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.Command;
import org.telegram.bot.domain.MessageAnalyzer;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;
import org.telegram.bot.utils.ObjectCopier;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class Alias implements Command<SendMessage>, MessageAnalyzer {

    public static final int MAX_COMMANDS_IN_ALIAS = 5;

    private final ApplicationContext context;
    private final Bot bot;
    private final ObjectCopier objectCopier;

    private final AliasService aliasService;
    private final UserService userService;
    private final UserStatsService userStatsService;
    private final CommandPropertiesService commandPropertiesService;
    private final SpeechService speechService;

    @Override
    public SendMessage parse(Update update) {
        Message message = getMessageFromUpdate(update);
        Long chatId = message.getChatId();

        bot.sendTyping(chatId);

        Chat chat = new Chat().setChatId(chatId);
        User user = new User().setUserId(message.getFrom().getId());

        String textMessage = cutCommandInText(message.getText());
        String caption;
        List<org.telegram.bot.domain.entities.Alias> aliasList;
        if (textMessage != null) {
            log.debug("Request to get info about aliases by name {} for chat {}", textMessage, chat);
            caption = "${command.alias.foundaliases}:\n";

            aliasList = deduplicate(aliasService.get(chat, textMessage));
            if (aliasList.isEmpty()) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING));
            }
        } else {
            log.debug("Request to get list of user {} and chat {}", user, chat);
            caption = "*${command.alias.aliaslist}:*\n";
            aliasList = aliasService.getByChatAndUser(chat, user);
        }

        String responseText = caption + aliasList
                .stream()
                .map(this::buildAliasInfoString)
                .collect(Collectors.joining("\n"));

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.enableMarkdown(true);
        sendMessage.disableWebPagePreview();
        sendMessage.setText(responseText);

        return sendMessage;
    }

    /**
     * Deduplicate alias list by name and value of alias.
     *
     * @param aliasList list of aliases to deduplication.
     * @return reduplicated list.
     */
    private List<org.telegram.bot.domain.entities.Alias> deduplicate(List<org.telegram.bot.domain.entities.Alias> aliasList) {
        List<org.telegram.bot.domain.entities.Alias> resultList = new ArrayList<>();
        Map<String, String> aliasNameAliasValueMap = new HashMap<>();

        aliasList.forEach(alias -> {
            String aliasValue = aliasNameAliasValueMap.get(alias.getName());
            if (!alias.getValue().equals(aliasValue)) {
                aliasNameAliasValueMap.put(alias.getName(), alias.getValue());
                resultList.add(alias);
            }
        });

        return resultList;
    }

    private String buildAliasInfoString(org.telegram.bot.domain.entities.Alias alias) {
        return alias.getName() + " — `" + alias.getValue() + "`";
    }

    @Override
    public void analyze(Update update) {
        Message message = getMessageFromUpdate(update);
        String textMessage = message.getText();
        Chat chat = new Chat().setChatId(message.getChatId());
        User user = new User().setUserId(message.getFrom().getId());

        String aliasName;
        String argument = null;
        int firstSpaceIndex = textMessage.indexOf(" ");
        if (firstSpaceIndex < 1) {
            aliasName = textMessage;
        } else {
            aliasName = textMessage.substring(0, firstSpaceIndex);
            argument = textMessage.substring(firstSpaceIndex);
        }

        org.telegram.bot.domain.entities.Alias alias = aliasService.get(chat, user, aliasName);
        if (alias != null) {
            List<String> aliasValueList = getAliasValueList(alias.getValue());
            if (aliasValueList.size() > 1) {
                for (String aliasValue : aliasValueList) {
                    if (argument == null) {
                        processUpdate(update, chat, user, aliasValue);
                    } else {
                        processUpdate(update, chat, user, aliasValue + argument);
                    }
                }
            } else {
                processUpdate(update, chat, user, alias.getValue());
            }
        }
    }

    private List<String> getAliasValueList(String aliasValue) {
        if (aliasValue.startsWith("{")) {
            return Arrays.stream(aliasValue.substring(1, aliasValue.length() - 1).split(";"))
                    .filter(value -> !value.isEmpty())
                    .limit(MAX_COMMANDS_IN_ALIAS)
                    .collect(Collectors.toList());
        }

        return List.of(aliasValue);
    }

    private void processUpdate(Update update, Chat chat, User user, String messageText) {
        Update newUpdate = objectCopier.copyObject(update, Update.class);
        if (newUpdate == null) {
            log.error("Failed to get a copy of update");
            return;
        }

        CommandProperties commandProperties = commandPropertiesService.findCommandInText(messageText, bot.getBotUsername());

        if (commandProperties != null &&
                (userService.isUserHaveAccessForCommand(
                        userService.getCurrentAccessLevel(user.getUserId(), chat.getChatId()).getValue(),
                        commandProperties.getAccessLevel()))) {
            Message newMessage = getMessageFromUpdate(newUpdate);
            newMessage.setText(messageText);
            userStatsService.incrementUserStatsCommands(chat, user);
            bot.parseAsync(newUpdate, (Command<?>) context.getBean(commandProperties.getClassName()));
        }
    }

}
