package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.model.Coordinates;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.request.Message;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.TextResponse;
import org.telegram.bot.domain.model.whois.IpInfo;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.exception.ip.IpInfoException;
import org.telegram.bot.exception.ip.IpInfoNoResponseException;
import org.telegram.bot.providers.ip.IpInfoProvider;
import org.telegram.bot.services.CommandWaitingService;
import org.telegram.bot.services.LanguageResolver;
import org.telegram.bot.services.SpeechService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IpTest {

    @Mock
    private Bot bot;
    @Mock
    private CommandWaitingService commandWaitingService;
    @Mock
    private SpeechService speechService;
    @Mock
    private LanguageResolver languageResolver;
    @Mock
    private IpInfoProvider ipInfoProvider;

    @InjectMocks
    private Ip ip;

    @Test
    void parseWithoutArgumentsTest() {
        final String expectedResponseText = "${command.ip.commandwaitingstart}";
        BotRequest request = TestUtils.getRequestFromGroup("ip");
        Message message = request.getMessage();

        BotResponse botResponse = ip.parse(request).getFirst();

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());

        verify(bot).sendTyping(message.getChatId());
        verify(commandWaitingService).add(message, Ip.class);
    }

    @Test
    void parseWithWrongIpAsArgumentsTest() {
        final String errorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("ip a");
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(errorText);

        BotException botException = assertThrows((BotException.class), () -> ip.parse(request));

        assertEquals(errorText, botException.getMessage());

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseWithIpInfoNoResponseExceptionTest() throws IpInfoException {
        final String errorText = "error";
        final String ipAddress = "127.0.0.1";
        final String lang = "en";
        BotRequest request = TestUtils.getRequestFromGroup("ip " + ipAddress);
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(languageResolver.getChatLanguageCode(request)).thenReturn(lang);
        when(ipInfoProvider.getData(ipAddress, lang)).thenThrow(new IpInfoNoResponseException(""));
        when(speechService.getRandomMessageByTag(BotSpeechTag.NO_RESPONSE)).thenReturn(errorText);

        BotException botException = assertThrows((BotException.class), () -> ip.parse(request));

        assertEquals(errorText, botException.getMessage());

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseWithIpInfoExceptionTest() throws IpInfoException {
        final String errorText = "error";
        final String ipAddress = "127.0.0.1";
        final String lang = "en";
        BotRequest request = TestUtils.getRequestFromGroup("ip " + ipAddress);
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(languageResolver.getChatLanguageCode(request)).thenReturn(lang);
        when(ipInfoProvider.getData(ipAddress, lang)).thenThrow(new IpInfoException(errorText));

        BotException botException = assertThrows((BotException.class), () -> ip.parse(request));

        assertEquals(errorText, botException.getMessage());

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseMinDataTest() throws IpInfoException {
        final String expectedResponseText = """
                <b>127.0.0.1</b>\s
                ()
                
                """;
        final String ipAddress = "127.0.0.1";
        final String lang = "en";
        BotRequest request = TestUtils.getRequestFromGroup("ip " + ipAddress);
        Message message = request.getMessage();

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(languageResolver.getChatLanguageCode(request)).thenReturn(lang);
        when(ipInfoProvider.getData(ipAddress, lang)).thenReturn(new IpInfo().setIp(ipAddress));

        BotResponse botResponse = ip.parse(request).getFirst();

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());

        verify(bot).sendTyping(message.getChatId());
    }

    @Test
    void parseAllDataTest() throws IpInfoException {
        final String expectedResponseText = """
                <b>127.0.0.1</b> (type)
                flagEmoji city (country, continent)
                /location_2_0000_3_0000
                
                ASN: <b>1</b>
                ORG: <b>org</b>
                ISP: <b>isp</b>
                domain
                """;
        final String ipAddress = "127.0.0.1";
        final String lang = "en";
        BotRequest request = TestUtils.getRequestFromGroup("ip " + ipAddress);
        Message message = request.getMessage();
        IpInfo ipInfo = new IpInfo().setIp(ipAddress)
                .setType("type")
                .setContinent("continent")
                .setCountry("country")
                .setRegion("region")
                .setCity("city")
                .setFlagEmoji("flagEmoji")
                .setAsn(1L)
                .setOrg("org")
                .setIsp("isp")
                .setDomain("domain")
                .setCoordinates(new Coordinates(2D, 3D));

        when(commandWaitingService.getText(message)).thenReturn(message.getCommandArgument());
        when(languageResolver.getChatLanguageCode(request)).thenReturn(lang);
        when(ipInfoProvider.getData(ipAddress, lang)).thenReturn(ipInfo);

        BotResponse botResponse = ip.parse(request).getFirst();

        TextResponse textResponse = TestUtils.checkDefaultTextResponseParams(botResponse);
        assertEquals(expectedResponseText, textResponse.getText());

        verify(bot).sendTyping(message.getChatId());
    }

}