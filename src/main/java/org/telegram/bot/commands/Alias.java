package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.ResponseSettings;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.*;
import org.telegram.bot.utils.ObjectCopier;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class Alias implements Command, MessageAnalyzer {

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
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        Long chatId = message.getChatId();

        bot.sendTyping(chatId);

        Chat chat = message.getChat();
        User user = message.getUser();

        String commandArgument = message.getCommandArgument();
        String caption;
        List<org.telegram.bot.domain.entities.Alias> aliasList;
        if (commandArgument != null) {
            log.debug("Request to get info about aliases by name {} for chat {}", commandArgument, chat);
            caption = "${command.alias.foundaliases}:\n";

            aliasList = deduplicate(aliasService.get(chat, commandArgument));
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

        return returnResponse(new TextResponse(message)
                .setText(responseText)
                .setResponseSettings(new ResponseSettings()
                        .setWebPagePreview(false)
                        .setFormattingStyle(FormattingStyle.MARKDOWN)));
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
    public List<BotResponse> analyze(BotRequest request) {
        Message message = request.getMessage();
        String textMessage = message.getText();
        if (textMessage == null) {
            return returnResponse();
        }

        Chat chat = request.getMessage().getChat();
        User user = request.getMessage().getUser();

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
                List<BotResponse> resultList = new ArrayList<>(MAX_COMMANDS_IN_ALIAS);

                for (String aliasValue : aliasValueList) {
                    if (argument == null) {
                        resultList.addAll(processUpdate(request, chat, user, aliasValue));
                    } else {
                        resultList.addAll(processUpdate(request, chat, user, aliasValue + argument));
                    }
                }

                return resultList;
            } else if (argument == null) {
                return processUpdate(request, chat, user, alias.getValue());
            }
        }

        return returnResponse();
    }

    private List<String> getAliasValueList(String aliasValue) {
        if (aliasValue.startsWith("{")) {
            return Arrays.stream(aliasValue.substring(1, aliasValue.length() - 1).split(";"))
                    .filter(value -> !value.isEmpty())
                    .limit(MAX_COMMANDS_IN_ALIAS)
                    .toList();
        }

        return List.of(aliasValue);
    }

    private List<BotResponse> processUpdate(BotRequest botRequest, Chat chat, User user, String messageText) {
        BotRequest newBotRequest = objectCopier.copyObject(botRequest, BotRequest.class);
        if (newBotRequest == null) {
            log.error("Failed to get a copy of update");
            return returnResponse();
        }

        CommandProperties commandProperties = commandPropertiesService.findCommandInText(messageText, bot.getBotUsername());

        if (commandProperties != null &&
                (userService.isUserHaveAccessForCommand(
                        userService.getCurrentAccessLevel(user.getUserId(), chat.getChatId()).getValue(),
                        commandProperties.getAccessLevel()))) {
            Message newMessage = newBotRequest.getMessage();
            newMessage.setText(messageText);
            userStatsService.incrementUserStatsCommands(chat, user);
            bot.parseAsync(newBotRequest, (Command) context.getBean(commandProperties.getClassName()));
        }

        return returnResponse();
    }

}
