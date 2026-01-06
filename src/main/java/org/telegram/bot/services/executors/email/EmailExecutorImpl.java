package org.telegram.bot.services.executors.email;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.bot.config.ConditionalOnPropertyNotEmpty;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserEmail;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.EmailResponse;
import org.telegram.bot.mapper.email.response.ResponseEmailMapper;
import org.telegram.bot.services.*;
import org.telegram.bot.services.email.EmailSender;
import org.telegram.bot.utils.EmailUtils;

import java.util.List;

@RequiredArgsConstructor
@Service
@ConditionalOnPropertyNotEmpty("mail.smtp.host")
public class EmailExecutorImpl implements EmailExecutor {

    private final ChatService chatService;
    private final UserStatsService userStatsService;
    private final UserEmailService userEmailService;
    private final LanguageResolver languageResolver;
    private final InternationalizationService internationalizationService;
    private final ResponseEmailMapper responseEmailMapper;
    private final EmailSender emailSender;

    @Override
    public void execute(List<BotResponse> botResponses, BotRequest request) {
        if (emailSender == null) {
            return;
        }

        List<User> chatUsers = userStatsService.getUsersOfChat(request.getMessage().getChat());
        List<UserEmail> usersEmails = userEmailService.getByUsers(chatUsers)
                .stream()
                .filter(EmailUtils::isShippingEnabled)
                .toList();
        if (usersEmails.isEmpty()) {
            return;
        }

        String lang = languageResolver.getChatLanguageCode(request);

        responseEmailMapper.toEmailResponse(botResponses, usersEmails)
                .stream()
                .map(emailResponse -> internationalizationService.internationalize(emailResponse, lang))
                .forEach(emailSender::sendMail);
    }

    @Override
    public void execute(BotResponse botResponse) {
        if (emailSender == null) {
            return;
        }

        Chat chat = chatService.get(botResponse.getChatId());
        List<User> chatUsers = userStatsService.getUsersOfChat(chat);
        List<UserEmail> usersEmails = userEmailService.getByUsers(chatUsers)
                .stream()
                .filter(EmailUtils::isShippingEnabled)
                .toList();
        if (usersEmails.isEmpty()) {
            return;
        }

        String lang = languageResolver.getChatLanguageCode(chat);
        EmailResponse emailResponse = responseEmailMapper.toEmailResponse(botResponse, usersEmails);
        emailResponse = internationalizationService.internationalize(emailResponse, lang);

        emailSender.sendMail(emailResponse);
    }

}
