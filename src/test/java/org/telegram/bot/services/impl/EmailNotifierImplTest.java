package org.telegram.bot.services.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.domain.entities.UserEmail;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.EmailResponse;
import org.telegram.bot.enums.RequestSource;
import org.telegram.bot.mapper.email.response.ResponseEmailMapper;
import org.telegram.bot.services.UserEmailService;
import org.telegram.bot.services.UserStatsService;
import org.telegram.bot.services.email.EmailSender;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailNotifierImplTest {

    @Mock
    private UserStatsService userStatsService;
    @Mock
    private EmailSender emailSender;
    @Mock
    private UserEmailService userEmailService;
    @Mock
    private ResponseEmailMapper responseEmailMapper;

    @InjectMocks
    private EmailNotifierImpl emailNotifier;

    @Captor
    private ArgumentCaptor<List<UserEmail>> userEmailsCaptor;

    @Test
    void notifyBotRequestFromEmailTest() {
        BotRequest request = new BotRequest().setSource(RequestSource.EMAIL);
        emailNotifier.notify(request);
        verify(emailSender, never()).sendMail(any(EmailResponse.class));
    }

    @Test
    void notifyBotRequestFromPrivateChatTest() {
        BotRequest request = new BotRequest()
                .setSource(RequestSource.TELEGRAM)
                .setMessage(new Message().setChat(new Chat().setChatId(1L)));
        emailNotifier.notify(request);
        verify(emailSender, never()).sendMail(any(EmailResponse.class));
    }

    @Test
    void notifyBotRequestWithoutEmailsTest() {
        Chat chat = new Chat().setChatId(-1L);
        BotRequest request = new BotRequest()
                .setSource(RequestSource.TELEGRAM)
                .setMessage(new Message().setChat(chat));

        List<User> usersOfChat = new ArrayList<>();
        when(userStatsService.getUsersOfChat(chat)).thenReturn(usersOfChat);
        List<UserEmail> userEmails = List.of(
                new UserEmail(),
                new UserEmail().setShippingEnabled(false),
                new UserEmail().setShippingEnabled(true),
                new UserEmail().setShippingEnabled(true).setVerified(false),
                new UserEmail().setShippingEnabled(true).setVerified(true),
                new UserEmail().setShippingEnabled(true).setVerified(true).setEmail(""),
                new UserEmail().setShippingEnabled(true).setVerified(true).setEmail("  "));
        when(userEmailService.getByUsers(usersOfChat)).thenReturn(userEmails);

        emailNotifier.notify(request);

        verify(emailSender, never()).sendMail(any(EmailResponse.class));
    }

    @Test
    void notifyTest() {
        UserEmail expectedUserEmail = new UserEmail()
                .setShippingEnabled(true)
                .setVerified(true)
                .setEmail("email@example.com");

        Chat chat = new Chat().setChatId(-1L);
        BotRequest request = new BotRequest()
                .setSource(RequestSource.TELEGRAM)
                .setMessage(new Message().setChat(chat));

        List<User> usersOfChat = new ArrayList<>();
        when(userStatsService.getUsersOfChat(chat)).thenReturn(usersOfChat);
        List<UserEmail> userEmails = List.of(
                new UserEmail(),
                new UserEmail().setShippingEnabled(false),
                new UserEmail().setShippingEnabled(true),
                new UserEmail().setShippingEnabled(true).setVerified(false),
                new UserEmail().setShippingEnabled(true).setVerified(true),
                new UserEmail().setShippingEnabled(true).setVerified(true).setEmail(""),
                new UserEmail().setShippingEnabled(true).setVerified(true).setEmail("  "),
                expectedUserEmail);
        when(userEmailService.getByUsers(usersOfChat)).thenReturn(userEmails);
        EmailResponse emailResponse = new EmailResponse();
        when(responseEmailMapper.toEmailResponse(eq(request), anyList())).thenReturn(emailResponse);

        emailNotifier.notify(request);

        verify(responseEmailMapper).toEmailResponse(eq(request), userEmailsCaptor.capture());
        List<UserEmail> filteredUserEmails = userEmailsCaptor.getValue();
        assertTrue(filteredUserEmails.contains(expectedUserEmail));

        verify(emailSender).sendMail(emailResponse);
    }

}