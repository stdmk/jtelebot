package org.telegram.bot.commands.setters;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserEmail;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.enums.AccessLevel;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.enums.Emoji;
import org.telegram.bot.enums.FormattingStyle;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.InternationalizationService;
import org.telegram.bot.services.SpeechService;
import org.telegram.bot.services.UserEmailService;
import org.telegram.bot.services.email.EmailSender;
import org.telegram.bot.utils.MathUtils;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static org.telegram.bot.utils.TextUtils.containsStartWith;
import static org.telegram.bot.utils.TextUtils.getStartsWith;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailSetter implements Setter<BotResponse> {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final String CALLBACK_COMMAND = "${setter.command} ";
    private static final String EMPTY_EMAIL_COMMAND = "email";
    private static final String UPDATE_EMAIL_COMMAND = EMPTY_EMAIL_COMMAND + " ${setter.email.update}";
    private static final String SET_EMAIL = EMPTY_EMAIL_COMMAND + " ${setter.email.setcommand}";
    private static final String CALLBACK_SET_EMAIL = CALLBACK_COMMAND + SET_EMAIL;
    private static final String REMOVE_EMAIL = EMPTY_EMAIL_COMMAND + " ${setter.email.removecommand}";
    private static final String CALLBACK_REMOVE_EMAIL = CALLBACK_COMMAND + REMOVE_EMAIL;

    private final Map<Long, Integer> verificationCodeMap = new ConcurrentHashMap<>();
    private final Set<String> emptyEmailCommands = new HashSet<>();
    private final Set<String> setEmailCommands = new HashSet<>();
    private final Set<String> removeEmailCommands = new HashSet<>();

    private final SpeechService speechService;
    private final UserEmailService userEmailService;
    private final InternationalizationService internationalizationService;
    private final CommandWaitingService commandWaitingService;
    @Lazy
    private final EmailSender emailSender;

    @PostConstruct
    private void postConstruct() {
        emptyEmailCommands.addAll(internationalizationService.getAllTranslations("setter.email.emptycommand"));
        setEmailCommands.addAll(internationalizationService.internationalize(SET_EMAIL));
        removeEmailCommands.addAll(internationalizationService.internationalize(REMOVE_EMAIL));
    }

    @Override
    public boolean canProcessed(String command) {
        return emptyEmailCommands.stream().anyMatch(command::startsWith);
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.TRUSTED;
    }

    @Override
    public BotResponse set(BotRequest request, String commandText) {
        Message message = request.getMessage();
        Chat chat = message.getChat();
        User user = message.getUser();
        commandWaitingService.remove(chat, user);

        String lowerCaseCommandText = commandText.toLowerCase(Locale.ROOT);
        if (message.isCallback()) {
            if (containsStartWith(setEmailCommands, lowerCaseCommandText)) {
                return setEmailByCallback(message, chat, user);
            } else if (containsStartWith(removeEmailCommands, lowerCaseCommandText)) {
                return removeEmailByCallback(message, user);
            }

            return getSetterWithKeyboard(message, user, false);
        }

        if (containsStartWith(setEmailCommands, lowerCaseCommandText)) {
            return setEmail(message, chat, user, commandText);
        } else if (containsStartWith(removeEmailCommands, lowerCaseCommandText)) {
            return removeEmail(message, user);
        }

        return getSetterWithKeyboard(message, user, true);
    }

    private EditResponse setEmailByCallback(Message message, Chat chat, User user) {
        commandWaitingService.add(chat, user, org.telegram.bot.commands.Set.class, CALLBACK_SET_EMAIL);

        return new EditResponse(message)
                .setText("${setter.email.setemailhelp}")
                .setResponseSettings(FormattingStyle.HTML);
    }

    private BotResponse setEmail(Message message, Chat chat, User user, String command) {
        String argument = getLocalizedCommand(command, SET_EMAIL);
        String value = command.substring(command.indexOf(argument) + argument.length() + 1);

        Long userId = user.getUserId();
        Integer expectedCode = verificationCodeMap.get(userId);
        int actualCode;
        if (expectedCode != null) {
            try {
                actualCode = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            if (!expectedCode.equals(actualCode)) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            UserEmail userEmail = userEmailService.get(user);
            userEmailService.save(userEmail.setVerified(true));

            verificationCodeMap.remove(userId);

            return getSetterWithKeyboard(message, user, true);
        } else {
            if (isInvalidEmail(value)) {
                throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
            }

            UserEmail userEmail = userEmailService.get(user);
            if (userEmail == null) {
                userEmail = new UserEmail().setUser(user).setVerified(false).setShippingEnabled(false);
            }

            userEmailService.save(userEmail.setEmail(value));
            commandWaitingService.add(chat, user, org.telegram.bot.commands.Set.class, CALLBACK_SET_EMAIL);

            Integer code = MathUtils.getRandomInRange(100000, 1000000);
            verificationCodeMap.put(userId, code);
            sendVerificationCode(value, code);

            return new TextResponse(message)
                    .setText("${setter.email.setverificationhelp}")
                    .setResponseSettings(FormattingStyle.HTML);
        }
    }

    private boolean isInvalidEmail(String email) {
        return !EMAIL_PATTERN.matcher(email).matches();
    }

    private void sendVerificationCode(String email, Integer code) {
        emailSender.sendMail(new EmailResponse()
                .setEmailAddresses(Set.of(email))
                .setSubject("Verification code")
                .setText(code.toString())
                .setAttachments(List.of()));
    }

    private BotResponse removeEmailByCallback(Message message, User user) {
        UserEmail userEmail = userEmailService.get(user);
        if (userEmail == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        userEmailService.remove(userEmail);

        return getSetterWithKeyboard(message, user, false);
    }

    private BotResponse removeEmail(Message message, User user) {
        UserEmail userEmail = userEmailService.get(user);
        if (userEmail == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT));
        }

        userEmailService.remove(userEmail);

        return getSetterWithKeyboard(message, user, true);
    }

    private BotResponse getSetterWithKeyboard(Message message, User user, boolean newMessage) {
        UserEmail userEmail = userEmailService.get(user);

        String currentEmail;
        String verified;
        if (userEmail == null) {
            currentEmail = "${setter.email.notset}";
            verified = "";
        } else {
            currentEmail = userEmail.getEmail();
            verified = Boolean.TRUE.equals(userEmail.getVerified()) ? Emoji.CHECK_MARK_BUTTON.getSymbol() : Emoji.DELETE.getSymbol();
        }

        String responseText = "${setter.email.currentemail}: <b>" + currentEmail + "</b>\n"
                + "${setter.email.verified}: " + verified;

        if (newMessage) {
            return new TextResponse(message)
                    .setText(responseText)
                    .setKeyboard(prepareKeyboard())
                    .setResponseSettings(FormattingStyle.HTML);
        }

        return new EditResponse(message)
                .setText(responseText)
                .setKeyboard(prepareKeyboard())
                .setResponseSettings(FormattingStyle.HTML);
    }

    private Keyboard prepareKeyboard() {
        return new Keyboard(List.of(
                List.of(new KeyboardButton()
                        .setName(Emoji.NEW.getSymbol() + "${setter.email.button.set}")
                        .setCallback(CALLBACK_SET_EMAIL)),
                List.of(new KeyboardButton()
                        .setName(Emoji.DELETE.getSymbol() + "${setter.email.button.delete}")
                        .setCallback(CALLBACK_REMOVE_EMAIL)),
                List.of(new KeyboardButton()
                        .setName(Emoji.UPDATE.getSymbol() + "${setter.email.button.update}")
                        .setCallback(CALLBACK_COMMAND + UPDATE_EMAIL_COMMAND)),
                List.of(new KeyboardButton()
                        .setName(Emoji.BACK.getSymbol() + "${setter.email.button.settings}")
                        .setCallback(CALLBACK_COMMAND + "back"))
        ));
    }

    private String getLocalizedCommand(String text, String command) {
        String localizedCommand = getStartsWith(
                internationalizationService.internationalize(command),
                text.toLowerCase(Locale.ROOT));

        if (localizedCommand == null) {
            throw new BotException(speechService.getRandomMessageByTag(BotSpeechTag.INTERNAL_ERROR));
        }

        return localizedCommand;
    }

}
