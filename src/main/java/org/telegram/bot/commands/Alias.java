package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.Command;
import org.telegram.bot.domain.TextAnalyzer;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class Alias implements Command<SendMessage>, TextAnalyzer {

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
        Chat chat = new Chat().setChatId(message.getChatId());
        User user = new User().setUserId(message.getFrom().getId());
        log.debug("Initialization of alias search for user {} and chat {}", user, chat);

        org.telegram.bot.domain.entities.Alias alias = aliasService.get(chat, user, message.getText());

        if (alias != null) {
            Update newUpdate = objectCopier.copyObject(update, Update.class);
            if (newUpdate == null) {
                log.error("Failed to get a copy of update");
                return;
            }

            String aliasValue = alias.getValue();
            Message newMessage = getMessageFromUpdate(newUpdate);
            newMessage.setText(aliasValue);
            CommandProperties commandProperties = commandPropertiesService.findCommandInText(aliasValue, bot.getBotUsername());

            if (commandProperties != null &&
                    (userService.isUserHaveAccessForCommand(
                            userService.getCurrentAccessLevel(user.getUserId(), chat.getChatId()).getValue(),
                            commandProperties.getAccessLevel()))) {
                    userStatsService.incrementUserStatsCommands(chat, user);
                    bot.parseAsync(newUpdate, (Command<?>) context.getBean(commandProperties.getClassName()));

            }
            log.debug("The alias found is not a command");
        }
        log.debug("No aliases found");
    }
}
