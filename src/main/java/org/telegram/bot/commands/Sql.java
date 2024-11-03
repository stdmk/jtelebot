package org.telegram.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.bot.Bot;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.utils.TextUtils;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.telegram.bot.utils.ExceptionUtils.getInitialExceptionCauseText;

@Component
@RequiredArgsConstructor
@Slf4j
public class Sql implements Command {

    private static final List<String> SELECT_DATA_SQL_COMMANDS = List.of("select");
    private static final List<String> UPDATE_DATA_SQL_COMMANDS = List.of("update", "delete", "insert");

    @PersistenceContext
    private EntityManager entityManager;

    private final Bot bot;
    private final SpeechService speechService;

    @Override
    @Transactional
    public List<BotResponse> parse(BotRequest request) {
        Message message = request.getMessage();
        String responseText;

        String commandArgument = message.getCommandArgument();
        if (StringUtils.isEmpty(commandArgument)) {
            return returnResponse();
        }

        bot.sendTyping(message.getChatId());
        log.debug("Request to execute sql request: {}", commandArgument);
        try {
            if (isSelectDataQuery(commandArgument)) {
                responseText = "```" + executeSelectQuery(commandArgument) + "```";
            } else if (isUpdateDataQuery(commandArgument)) {
                responseText = "${command.sql.success}: `" + executeUpdateQuery(commandArgument) + "`";
            } else {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }
        } catch (BotException be) {
            throw be;
        } catch (Exception e) {
            responseText = "${command.sql.error}: `" + getInitialExceptionCauseText(e) + "`";
            bot.sendMessage(new TextResponse(message)
                    .setText(responseText)
                    .setResponseSettings(FormattingStyle.MARKDOWN));
        }

        return returnResponse(new TextResponse(message)
                .setText(responseText)
                .setResponseSettings(FormattingStyle.MARKDOWN));
    }

    private boolean isSelectDataQuery(String query) {
        String lowerCasedQuery = query.trim().toLowerCase(Locale.ROOT);
        return TextUtils.startsWithElementInList(lowerCasedQuery, SELECT_DATA_SQL_COMMANDS);
    }

    private boolean isUpdateDataQuery(String query) {
        String lowerCasedQuery = query.trim().toLowerCase(Locale.ROOT);
        return TextUtils.startsWithElementInList(lowerCasedQuery, UPDATE_DATA_SQL_COMMANDS);
    }

    private String executeSelectQuery(String query) {
        List<?> resultList = entityManager.createNativeQuery(query).getResultList();
        if (resultList.isEmpty()) {
            return "${command.sql.emptyresponse}";
        } else {
            if (resultList.stream().allMatch(Object[].class::isInstance)) {
                return resultList
                        .stream()
                        .map(results -> Arrays.stream((Object[]) results)
                                .map(Object::toString)
                                .collect(Collectors.joining(", ", "[", "]")))
                        .collect(Collectors.joining("\n"));
            } else {
                return resultList.stream().map(Object::toString).collect(Collectors.joining(", ", "[", "]"));
            }
        }
    }

    private int executeUpdateQuery(String query) {
        return entityManager.createNativeQuery(query).executeUpdate();
    }

}
