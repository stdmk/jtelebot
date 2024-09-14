package org.telegram.bot.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.Bot;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.domain.model.response.File;
import org.telegram.bot.domain.model.response.FileResponse;
import org.telegram.bot.enums.BotSpeechTag;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.SpeechService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ButtsTest {
    
    @Mock
    private Bot bot;
    @Mock
    private SpeechService speechService;
    @Mock
    private RestTemplate botRestTemplate;
    @Mock
    private ResponseEntity<Object> response;

    @InjectMocks
    private Butts butts;

    @Test
    void parseWithNoResponseTest() {
        BotRequest request = TestUtils.getRequestFromGroup();
        when(botRestTemplate.getForEntity(anyString(), any())).thenThrow(new RestClientException(""));

        assertThrows(BotException.class, () -> butts.parse(request));
        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void parseWithNullButtsTest() {
        BotRequest request = TestUtils.getRequestFromGroup();
        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(response);

        assertThrows(BotException.class, () -> butts.parse(request));
        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void parseWithEmptyButtsTest() {
        BotRequest request = TestUtils.getRequestFromGroup();
        Butts.ButtsData[] buttsDataArray = {};

        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(response);
        when(response.getBody()).thenReturn(buttsDataArray);

        assertThrows(BotException.class, () -> butts.parse(request));
        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void parseTest() {
        BotRequest request = TestUtils.getRequestFromGroup();
        Butts.ButtsData buttsData = new Butts.ButtsData();
        buttsData.setPreview("butts_preview/12345.jpg");
        Butts.ButtsData[] buttsDataArray = {buttsData};

        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(response);
        when(response.getBody()).thenReturn(buttsDataArray);

        BotResponse botResponse = butts.parse(request).get(0);
        FileResponse image = TestUtils.checkDefaultFileResponseImageParams(botResponse);

        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
        TestUtils.checkDefaultFileResponseImageParams(image, true);
    }

    @ParameterizedTest
    @ValueSource(strings = {Integer.MAX_VALUE + "0", "0"})
    void parseWithWrongNumberAsArgumentTest(String buttsCount) {
        final String expectedErrorText = "error";
        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotRequest request = TestUtils.getRequestFromGroup("butts " + buttsCount);

        BotException botException = assertThrows(BotException.class, () -> butts.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void parseWithNumberAsArgumentTest() {
        BotRequest request = TestUtils.getRequestFromGroup("butts 2");

        Butts.ButtsData buttsData1 = new Butts.ButtsData();
        buttsData1.setPreview("butts_preview/12345.jpg");

        Butts.ButtsData buttsData2 = new Butts.ButtsData();
        buttsData2.setPreview("butts_preview/54321.jpg");
        buttsData2.setModel("model");

        Butts.ButtsData[] buttsDataArray = {buttsData1, buttsData2};

        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(response);
        when(response.getBody()).thenReturn(buttsDataArray);

        BotResponse botResponse = butts.parse(request).get(0);

        FileResponse fileResponse = TestUtils.checkDefaultFileResponseImageParams(botResponse);
        List<File> files = fileResponse.getFiles();
        assertEquals(2, files.size());

        assertTrue(files.stream().allMatch(image -> image.getFileSettings().isSpoiler()));
        assertTrue(files.stream().map(File::getName).anyMatch(name -> buttsData2.getModel().equals(name)));
        assertTrue(files.stream().map(File::getName).anyMatch(String::isEmpty));

        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
    }

    @Test
    void parseWithNumberMoreThanTenArgumentTest() {
        final String expectedUrl = "http://api.obutts.ru/butts/0/10/random";
        BotRequest request = TestUtils.getRequestFromGroup("butts 11");

        Butts.ButtsData buttsData1 = new Butts.ButtsData();
        buttsData1.setPreview("butts_preview/12345.jpg");

        Butts.ButtsData buttsData2 = new Butts.ButtsData();
        buttsData2.setPreview("butts_preview/54321.jpg");

        Butts.ButtsData[] buttsDataArray = {buttsData1, buttsData2};

        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(response);
        when(response.getBody()).thenReturn(buttsDataArray);

        butts.parse(request);

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(botRestTemplate).getForEntity(urlCaptor.capture(), eq(Butts.ButtsData[].class));
        assertEquals(expectedUrl, urlCaptor.getValue());
    }

    @Test
    void parseSearchForButtsWhenNotFoundTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("butts model");

        when(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING)).thenReturn(expectedErrorText);
        Butts.ButtsData[] buttsDataArray = {};
        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(response);
        when(response.getBody()).thenReturn(buttsDataArray);

        BotException botException = assertThrows(BotException.class, () -> butts.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING);
    }

    @Test
    void parseSearchForButtsTest() {
        final String modelName = "model";
        BotRequest request = TestUtils.getRequestFromGroup("butts model");

        Butts.ButtsData buttsData1 = new Butts.ButtsData();
        buttsData1.setPreview("butts_preview/12345.jpg");
        buttsData1.setModel(modelName);

        Butts.ButtsData buttsData2 = new Butts.ButtsData();
        buttsData2.setPreview("butts_preview/54321.jpg");
        buttsData2.setModel(modelName);

        Butts.ButtsData[] buttsDataArray = {buttsData1, buttsData2};

        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(response);
        when(response.getBody()).thenReturn(buttsDataArray);

        BotResponse botResponse = butts.parse(request).get(0);

        FileResponse fileResponse = TestUtils.checkDefaultFileResponseImageParams(botResponse);
        List<File> files = fileResponse.getFiles();
        assertEquals(2, files.size());

        assertTrue(files.stream().allMatch(image -> image.getFileSettings().isSpoiler()));
        assertTrue(files.stream().map(File::getName).anyMatch(modelName::equals));

        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
    }
    
}