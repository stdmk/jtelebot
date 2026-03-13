package org.telegram.bot.controllers;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.telegram.bot.commands.Command;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.entities.Token;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.enums.AccessLevel;
import org.telegram.bot.enums.RequestSource;
import org.telegram.bot.services.ChatService;
import org.telegram.bot.services.CommandPropertiesService;
import org.telegram.bot.services.UserService;
import org.telegram.bot.services.WebTokenService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/web/commands")
@RequiredArgsConstructor
public class WebCommandController {

    private static final String TOKEN_PREFIX = "web-user-";

    private final WebTokenService webTokenService;
    private final UserService userService;
    private final ChatService chatService;
    private final CommandPropertiesService commandPropertiesService;
    private final ApplicationContext context;

    @GetMapping
    public ResponseEntity<?> getCommands(@RequestHeader("X-Web-Token") String tokenValue) {
        User user = getUserByToken(tokenValue);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Invalid token"));
        }

        List<WebCommandInfoResponse> commands = commandPropertiesService.getAvailableCommandsForLevel(user.getAccessLevel())
                .stream()
                .map(command -> new WebCommandInfoResponse(command.getCommandName(), command.getRussifiedName(), command.getAccessLevel()))
                .toList();

        return ResponseEntity.ok(commands);
    }

    @PostMapping("/execute")
    public ResponseEntity<?> executeCommand(@RequestHeader("X-Web-Token") String tokenValue,
                                            @RequestBody ExecuteWebCommandRequest executeWebCommandRequest) {
        User user = getUserByToken(tokenValue);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Invalid token"));
        }

        if (StringUtils.isBlank(executeWebCommandRequest.command())) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Command is required"));
        }

        CommandProperties commandProperties = commandPropertiesService.getCommand(executeWebCommandRequest.command());
        if (commandProperties == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Command not found"));
        }

        if (!userService.isUserHaveAccessForCommand(user, commandProperties.getAccessLevel())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse("No access for this command"));
        }

        Command command = (Command) context.getBean(commandProperties.getClassName());
        BotRequest request = buildRequest(user, executeWebCommandRequest.command(), executeWebCommandRequest.argument());
        List<String> responses = command.parse(request)
                .stream()
                .map(this::extractText)
                .toList();

        return ResponseEntity.ok(new ExecuteWebCommandResponse(commandProperties.getCommandName(), responses));
    }

    private String extractText(BotResponse response) {
        if (response instanceof TextResponse textResponse) {
            return textResponse.getText();
        }

        return response == null ? "" : response.getClass().getSimpleName();
    }

    private BotRequest buildRequest(User user, String commandName, String argument) {
        Chat chat = chatService.get(user.getUserId());
        if (chat == null) {
            chat = new Chat().setChatId(user.getUserId()).setAccessLevel(user.getAccessLevel()).setName(user.getUsername());
        }

        String text = commandName + (StringUtils.isBlank(argument) ? "" : " " + argument);

        Message message = new Message()
                .setChat(chat)
                .setUser(user)
                .setText(text)
                .setDateTime(LocalDateTime.now())
                .setMessageId(1);

        return new BotRequest().setSource(RequestSource.TELEGRAM).setMessage(message);
    }

    private User getUserByToken(String tokenValue) {
        if (StringUtils.isBlank(tokenValue)) {
            return null;
        }

        Token token = webTokenService.getByTokenValue(tokenValue);
        if (token == null || token.getName() == null || !token.getName().startsWith(TOKEN_PREFIX)) {
            return null;
        }

        try {
            Long userId = Long.parseLong(token.getName().substring(TOKEN_PREFIX.length()));
            User user = userService.get(userId);
            if (user == null) {
                return null;
            }
            if (user.getAccessLevel() == null) {
                user.setAccessLevel(AccessLevel.NEWCOMER.getValue());
            }
            return user;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private record ExecuteWebCommandRequest(String command, String argument) {
    }

    private record WebCommandInfoResponse(String command, String description, Integer accessLevel) {
    }

    private record ExecuteWebCommandResponse(String command, List<String> responses) {
    }

    private record ErrorResponse(String error) {
    }
}
