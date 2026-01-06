package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.bot.config.ConditionalOnPropertyNotEmpty;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserEmail;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.enums.RequestSource;
import org.telegram.bot.mapper.email.response.ResponseEmailMapper;
import org.telegram.bot.services.EmailNotifier;
import org.telegram.bot.services.UserEmailService;
import org.telegram.bot.services.UserStatsService;
import org.telegram.bot.services.email.EmailSender;
import org.telegram.bot.utils.EmailUtils;
import org.telegram.bot.utils.TelegramUtils;

import java.util.List;

@RequiredArgsConstructor
@Service
@ConditionalOnPropertyNotEmpty("mail.smtp.host")
public class EmailNotifierImpl implements EmailNotifier {

    private final UserStatsService userStatsService;
    private final EmailSender emailSender;
    private final UserEmailService userEmailService;
    private final ResponseEmailMapper responseEmailMapper;

    @Override
    public void notify(BotRequest botRequest) {
        if (RequestSource.EMAIL.equals(botRequest.getSource())) {
            return;
        }

        Chat chat = botRequest.getMessage().getChat();
        if (TelegramUtils.isPrivateChat(chat)) {
            return;
        }

        List<User> usersOfChat = userStatsService.getUsersOfChat(chat);
        List<UserEmail> usersEmails = userEmailService.getByUsers(usersOfChat)
                .stream()
                .filter(EmailUtils::isShippingEnabled)
                .toList();

        if (usersEmails.isEmpty()) {
            return;
        }

        emailSender.sendMail(responseEmailMapper.toEmailResponse(botRequest, usersEmails));
    }

}
