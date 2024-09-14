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
class BoobsTest {

    @Mock
    private Bot bot;
    @Mock
    private SpeechService speechService;
    @Mock
    private RestTemplate botRestTemplate;
    @Mock
    private ResponseEntity<Object> response;

    @InjectMocks
    private Boobs boobs;

    @Test
    void parseWithNoResponseTest() {
        BotRequest request = TestUtils.getRequestFromGroup();
        when(botRestTemplate.getForEntity(anyString(), any())).thenThrow(new RestClientException(""));

        assertThrows(BotException.class, () -> boobs.parse(request));
        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void parseWithNullBoobsTest() {
        BotRequest request = TestUtils.getRequestFromGroup();
        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(response);

        assertThrows(BotException.class, () -> boobs.parse(request));
        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void parseWithEmptyBoobsTest() {
        BotRequest request = TestUtils.getRequestFromGroup();
        Boobs.BoobsData[] boobsDataArray = {};

        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(response);
        when(response.getBody()).thenReturn(boobsDataArray);

        assertThrows(BotException.class, () -> boobs.parse(request));
        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.NO_RESPONSE);
    }

    @Test
    void parseTest() {
        BotRequest request = TestUtils.getRequestFromGroup();
        Boobs.BoobsData boobsData = new Boobs.BoobsData();
        boobsData.setPreview("boobs_preview/12345.jpg");
        Boobs.BoobsData[] boobsDataArray = {boobsData};

        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(response);
        when(response.getBody()).thenReturn(boobsDataArray);

        BotResponse botResponse = boobs.parse(request).get(0);
        FileResponse image = TestUtils.checkDefaultFileResponseImageParams(botResponse);

        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
        TestUtils.checkDefaultFileResponseImageParams(image, true);
    }

    @ParameterizedTest
    @ValueSource(strings = {Integer.MAX_VALUE + "0", "0"})
    void parseWithWrongNumberAsArgumentTest(String boobsCount) {
        final String expectedErrorText = "error";
        when(speechService.getRandomMessageByTag(BotSpeechTag.WRONG_INPUT)).thenReturn(expectedErrorText);

        BotRequest request = TestUtils.getRequestFromGroup("boobs " + boobsCount);

        BotException botException = assertThrows(BotException.class, () -> boobs.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.WRONG_INPUT);
    }

    @Test
    void parseWithNumberAsArgumentTest() {
        BotRequest request = TestUtils.getRequestFromGroup("boobs 2");

        Boobs.BoobsData boobsData1 = new Boobs.BoobsData();
        boobsData1.setPreview("boobs_preview/12345.jpg");

        Boobs.BoobsData boobsData2 = new Boobs.BoobsData();
        boobsData2.setPreview("boobs_preview/54321.jpg");
        boobsData2.setModel("model");

        Boobs.BoobsData[] boobsDataArray = {boobsData1, boobsData2};

        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(response);
        when(response.getBody()).thenReturn(boobsDataArray);

        BotResponse botResponse = boobs.parse(request).get(0);

        FileResponse fileResponse = TestUtils.checkDefaultFileResponseImageParams(botResponse);
        List<File> files = fileResponse.getFiles();
        assertEquals(2, files.size());

        assertTrue(files.stream().allMatch(image -> image.getFileSettings().isSpoiler()));
        assertTrue(files.stream().map(File::getName).anyMatch(name -> boobsData2.getModel().equals(name)));
        assertTrue(files.stream().map(File::getName).anyMatch(String::isEmpty));

        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
    }

    @Test
    void parseWithNumberMoreThanTenArgumentTest() {
        final String expectedUrl = "http://api.oboobs.ru/boobs/0/10/random";
        BotRequest request = TestUtils.getRequestFromGroup("boobs 11");

        Boobs.BoobsData boobsData1 = new Boobs.BoobsData();
        boobsData1.setPreview("boobs_preview/12345.jpg");

        Boobs.BoobsData boobsData2 = new Boobs.BoobsData();
        boobsData2.setPreview("boobs_preview/54321.jpg");

        Boobs.BoobsData[] boobsDataArray = {boobsData1, boobsData2};

        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(response);
        when(response.getBody()).thenReturn(boobsDataArray);

        boobs.parse(request);

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(botRestTemplate).getForEntity(urlCaptor.capture(), eq(Boobs.BoobsData[].class));
        assertEquals(expectedUrl, urlCaptor.getValue());
    }

    @Test
    void parseSearchForBoobsWhenNotFoundTest() {
        final String expectedErrorText = "error";
        BotRequest request = TestUtils.getRequestFromGroup("boobs model");

        when(speechService.getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING)).thenReturn(expectedErrorText);
        Boobs.BoobsData[] boobsDataArray = {};
        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(response);
        when(response.getBody()).thenReturn(boobsDataArray);

        BotException botException = assertThrows(BotException.class, () -> boobs.parse(request));
        assertEquals(expectedErrorText, botException.getMessage());

        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
        verify(speechService).getRandomMessageByTag(BotSpeechTag.FOUND_NOTHING);
    }

    @Test
    void parseSearchForBoobsTest() {
        final String modelName = "model";
        BotRequest request = TestUtils.getRequestFromGroup("boobs model");

        Boobs.BoobsData boobsData1 = new Boobs.BoobsData();
        boobsData1.setPreview("boobs_preview/12345.jpg");
        boobsData1.setModel(modelName);

        Boobs.BoobsData boobsData2 = new Boobs.BoobsData();
        boobsData2.setPreview("boobs_preview/54321.jpg");
        boobsData2.setModel(modelName);

        Boobs.BoobsData[] boobsDataArray = {boobsData1, boobsData2};

        when(botRestTemplate.getForEntity(anyString(), any())).thenReturn(response);
        when(response.getBody()).thenReturn(boobsDataArray);

        BotResponse botResponse = boobs.parse(request).get(0);

        FileResponse fileResponse = TestUtils.checkDefaultFileResponseImageParams(botResponse);
        List<File> files = fileResponse.getFiles();
        assertEquals(2, files.size());

        assertTrue(files.stream().allMatch(image -> image.getFileSettings().isSpoiler()));
        assertTrue(files.stream().map(File::getName).anyMatch(modelName::equals));

        verify(bot).sendUploadPhoto(request.getMessage().getChatId());
    }

}