package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.ResponseSettings;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.AliasService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.ObjectCopier;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class Alias implements Command, MessageAnalyzer {

    public static final int MAX_COMMANDS_IN_ALIAS = 5;

    private final Bot bot;
    private final ObjectCopier objectCopier;
    private final AliasService aliasService;
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
        if (!message.hasText()) {
            return returnResponse();
        }

        String textMessage = message.getText();
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
            if (alias.getValue().contains("{")) {
                List<String> aliasValueList = getAliasValueList(alias.getValue());
                List<BotResponse> resultList = new ArrayList<>(MAX_COMMANDS_IN_ALIAS);
                for (String aliasValue : aliasValueList) {
                    resultList.addAll(processRequest(request, getMessageText(aliasValue, argument)));
                }
                return resultList;
            } else {
                return processRequest(request, getMessageText(alias.getValue(), argument));
            }
        }

        return returnResponse();
    }

    private String getMessageText(String aliasValue, String argument) {
        if (argument == null) {
            return aliasValue;
        }

        return aliasValue + argument;
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

    private List<BotResponse> processRequest(BotRequest botRequest, String messageText) {
        BotRequest newBotRequest = objectCopier.copyObject(botRequest, BotRequest.class);
        if (newBotRequest == null) {
            log.error("Failed to get a copy of request");
            return returnResponse();
        }
        newBotRequest.getMessage().setText(messageText);

        bot.processRequestWithoutAnalyze(newBotRequest);

        return returnResponse();
    }

}
