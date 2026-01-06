package org.telegram.bot.mapper.email.response;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.UserEmail;
import org.telegram.bot.domain.model.request.Attachment;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.*;
import org.telegram.bot.services.ChatService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResponseEmailMapperTest {

    private final EmailResponseMapper emailResponseMapper = mock(EmailResponseMapper.class);

    private final List<EmailResponseMapper> mappers = List.of(emailResponseMapper);
    private final FileMapper fileMapper = mock(FileMapper.class);
    private final ChatService chatService = mock(ChatService.class);
    private final Bot bot = mock(Bot.class);

    private final ResponseEmailMapper responseEmailMapper = new ResponseEmailMapper(mappers, fileMapper, chatService, bot);

    @Test
    void toEmailResponsesTest() {
        final String email1 = "email1@example.com";
        final String email2 = "email2@example.com";
        final Long chatId = 1L;
        final String response2Text = "text";

        UserEmail userEmail1 = new UserEmail().setEmail(email1);
        UserEmail userEmail2 = new UserEmail().setEmail(email2);
        List<UserEmail> userEmails = List.of(userEmail1, userEmail2);

        BotResponse botResponse1 = new TextResponse().setChatId(chatId);
        BotResponse botResponse2 = new TextResponse().setChatId(chatId);

        doReturn(TextResponse.class).when(emailResponseMapper).getMappingClass();
        ReflectionTestUtils.invokeMethod(responseEmailMapper, "postConstruct");
        when(bot.getBotUsername()).thenReturn("jtelebot");

        EmailResponse emailResponse1 = new EmailResponse();
        EmailResponse emailResponse2 = new EmailResponse().setText(response2Text);
        List<BotResponse> botResponses = List.of(botResponse1, botResponse2);
        when(emailResponseMapper.map(same(botResponse1))).thenReturn(emailResponse1);
        when(emailResponseMapper.map(same(botResponse2))).thenReturn(emailResponse2);

        List<EmailResponse> emailResponses = responseEmailMapper.toEmailResponse(botResponses, userEmails);

        assertNotNull(emailResponses);
        assertEquals(botResponses.size(), emailResponses.size());

        assertTrue(emailResponses.contains(emailResponse1));
        assertTrue(emailResponses.contains(emailResponse2));

        Set<String> emailAddresses = emailResponse1.getEmailAddresses();
        assertEquals(emailAddresses, emailResponse2.getEmailAddresses());
        assertTrue(emailAddresses.contains(email1));
        assertTrue(emailAddresses.contains(email2));

        assertEquals(chatId.toString(), emailResponse1.getSubject());
        assertEquals(chatId.toString(), emailResponse2.getSubject());
        assertNotNull(emailResponse1.getText());
        assertTrue(emailResponse2.getText().contains(response2Text));
    }

    @Test
    void toEmailResponseTest() {
        final String email1 = "email1@example.com";
        final String email2 = "email2@example.com";
        final Long chatId = 1L;
        final String responseText = "text";
        final String chatName = "chatName";

        UserEmail userEmail1 = new UserEmail().setEmail(email1);
        UserEmail userEmail2 = new UserEmail().setEmail(email2);
        List<UserEmail> userEmails = List.of(userEmail1, userEmail2);

        BotResponse botResponse = new TextResponse().setChatId(chatId);

        doReturn(TextResponse.class).when(emailResponseMapper).getMappingClass();
        ReflectionTestUtils.invokeMethod(responseEmailMapper, "postConstruct");

        EmailResponse expected = new EmailResponse().setText(responseText);
        when(emailResponseMapper.map(same(botResponse))).thenReturn(expected);
        when(chatService.get(chatId)).thenReturn(new Chat().setName(chatName));

        EmailResponse actual = responseEmailMapper.toEmailResponse(botResponse, userEmails);

        assertEquals(expected, actual);

        Set<String> emailAddresses = actual.getEmailAddresses();
        assertTrue(emailAddresses.contains(email1));
        assertTrue(emailAddresses.contains(email2));

        assertEquals(chatName + " (" + chatId + ")", actual.getSubject());

        assertTrue(actual.getText().contains(responseText));
    }

    @Test
    void toEmailResponseFromNullableBotRequestTest() {
        BotRequest botRequest = new BotRequest();
        EmailResponse emailResponse = responseEmailMapper.toEmailResponse(botRequest, List.of());
        assertNull(emailResponse);
    }

    @Test
    void toEmailResponseFromBotRequestWithoutFilesTest() {
        final String email1 = "email1@example.com";
        final String email2 = "email2@example.com";
        final Long chatId = 1L;
        final String chatName = "chatName";

        UserEmail userEmail1 = new UserEmail().setEmail(email1);
        UserEmail userEmail2 = new UserEmail().setEmail(email2);
        List<UserEmail> userEmails = List.of(userEmail1, userEmail2);

        List<Attachment> attachments = new ArrayList<>();
        Chat chat = new Chat().setChatId(chatId).setName(chatName);
        Message message = new Message()
                .setChat(chat)
                .setUser(TestUtils.getUser())
                .setDateTime(LocalDateTime.of(2000, 1, 1, 0, 0))
                .setAttachments(attachments);
        BotRequest botRequest = new BotRequest().setMessage(message);

        when(chatService.get(chatId)).thenReturn(chat);
        when(fileMapper.toFiles(attachments)).thenReturn(null);

        EmailResponse emailResponse = responseEmailMapper.toEmailResponse(botRequest, userEmails);

        assertNotNull(emailResponse);

        Set<String> emailAddresses = emailResponse.getEmailAddresses();
        assertTrue(emailAddresses.contains(email1));
        assertTrue(emailAddresses.contains(email2));

        assertEquals(chatName + " (" + chatId + ")", emailResponse.getSubject());

        assertEquals("<u>01.01.2000 00:00:00 <a href=\"tg://user?id=1\">username</a></u>:<br><br>", emailResponse.getText());

        assertTrue(emailResponse.getAttachments().isEmpty());
    }

    @Test
    void toEmailResponseFromBotRequestTest() {
        final String email1 = "email1@example.com";
        final String email2 = "email2@example.com";
        final String messageText = "messageText";
        final Long chatId = 1L;
        final String chatName = "chatName";

        UserEmail userEmail1 = new UserEmail().setEmail(email1);
        UserEmail userEmail2 = new UserEmail().setEmail(email2);
        List<UserEmail> userEmails = List.of(userEmail1, userEmail2);

        List<Attachment> attachments = new ArrayList<>();
        Chat chat = new Chat().setChatId(chatId).setName(chatName);
        Message message = new Message()
                .setChat(chat)
                .setUser(TestUtils.getUser())
                .setText(messageText)
                .setDateTime(LocalDateTime.of(2000, 1, 1, 0, 0))
                .setAttachments(attachments);
        BotRequest botRequest = new BotRequest().setMessage(message);

        when(chatService.get(chatId)).thenReturn(chat);
        List<File> files = List.of(
                new File(FileType.FILE, new byte[]{}, "name", "add1"),
                new File(FileType.FILE, new byte[]{}, "name"),
                new File(FileType.FILE, new byte[]{}, "name", "add2"));
        when(fileMapper.toFiles(attachments)).thenReturn(files);

        EmailResponse emailResponse = responseEmailMapper.toEmailResponse(botRequest, userEmails);

        assertNotNull(emailResponse);

        Set<String> emailAddresses = emailResponse.getEmailAddresses();
        assertTrue(emailAddresses.contains(email1));
        assertTrue(emailAddresses.contains(email2));

        assertEquals(chatName + " (" + chatId + ")", emailResponse.getSubject());

        assertEquals("<u>01.01.2000 00:00:00 <a href=\"tg://user?id=1\">username</a></u>:<br><br>messageText<br>add1<br>add2", emailResponse.getText());

        assertEquals(files, emailResponse.getAttachments());
    }

}