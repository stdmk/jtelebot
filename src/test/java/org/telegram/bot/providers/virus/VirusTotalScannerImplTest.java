package org.telegram.bot.providers.virus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.exception.virus.VirusScanApiKeyMissingException;
import org.telegram.bot.exception.virus.VirusScanException;
import org.telegram.bot.exception.virus.VirusScanNoResponseException;

import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VirusTotalScannerImplTest {

    @Mock
    private PropertiesConfig propertiesConfig;
    @Mock
    private RestTemplate defaultRestTemplate;
    @Mock
    private BotStats botStats;

    @Mock
    private ResponseEntity<VirusTotalScannerImpl.AnalysesResponse> response;
    @Mock
    private ResponseEntity<VirusTotalScannerImpl.AnalysesResponse> reportResponse;

    @InjectMocks
    private VirusTotalScannerImpl virusTotalScanner;

    @BeforeEach
    private void init() {
        ReflectionTestUtils.setField(virusTotalScanner, "reportWaitingTimeMillis", 1);
    }

    @Test
    void scanWithoutApiKeyTest() throws MalformedURLException {
        URL url = new URL("http://example.com");
        assertThrows((VirusScanApiKeyMissingException.class), () -> virusTotalScanner.scan(url));
    }

    @Test
    void scanWithoutResponseTest() throws MalformedURLException {
        URL url = new URL("http://example.com");

        when(propertiesConfig.getVirusTotalApiKey()).thenReturn("123");
        when(defaultRestTemplate.postForEntity(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<VirusTotalScannerImpl.AnalysesResponse>>any()))
                .thenThrow(new RuntimeException());

        assertThrows((VirusScanNoResponseException.class), () -> virusTotalScanner.scan(url));
    }

    @Test
    void scanWithEmptyResponseTest() throws MalformedURLException {
        URL url = new URL("http://example.com");

        when(propertiesConfig.getVirusTotalApiKey()).thenReturn("123");
        when(defaultRestTemplate.postForEntity(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<VirusTotalScannerImpl.AnalysesResponse>>any()))
                .thenReturn(response);

        assertThrows((VirusScanNoResponseException.class), () -> virusTotalScanner.scan(url));
    }

    @Test
    void scanUrlWithoutReportResponseTest() throws MalformedURLException {
        URL url = new URL("http://example.com");

        when(propertiesConfig.getVirusTotalApiKey()).thenReturn("123");
        when(response.getBody()).thenReturn(getSomeAnalysesResponse());
        when(defaultRestTemplate.postForEntity(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<VirusTotalScannerImpl.AnalysesResponse>>any()))
                .thenReturn(response);
        when(defaultRestTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), ArgumentMatchers.<Class<VirusTotalScannerImpl.AnalysesResponse>>any()))
                .thenThrow(new RuntimeException());

        assertThrows((VirusScanNoResponseException.class), () -> virusTotalScanner.scan(url));
    }

    @Test
    void scanUrlWithEmptyReportResponseTest() throws MalformedURLException {
        URL url = new URL("http://example.com");

        when(propertiesConfig.getVirusTotalApiKey()).thenReturn("123");
        when(response.getBody()).thenReturn(getSomeAnalysesResponse());
        when(defaultRestTemplate.postForEntity(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<VirusTotalScannerImpl.AnalysesResponse>>any()))
                .thenReturn(response);
        when(defaultRestTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), ArgumentMatchers.<Class<VirusTotalScannerImpl.AnalysesResponse>>any()))
                .thenReturn(reportResponse);

        assertThrows((VirusScanNoResponseException.class), () -> virusTotalScanner.scan(url));
    }

    @Test
    void scanUrlTest() throws MalformedURLException, VirusScanException {
        final String expectedString = "<b>27.01.2024 09:12:55</b>\n" +
                "${command.virus.stats.total}: <b>3</b>\n" +
                "${command.virus.stats.confirmedtimeout}: <b>1</b>\n" +
                "${command.virus.stats.failure}: <b>2</b>\n" +
                "${command.virus.stats.harmless}: <b>3</b>\n" +
                "${command.virus.stats.undetected}: <b>8</b>\n" +
                "${command.virus.stats.suspicious}: <b>5</b>\n" +
                "${command.virus.stats.malicious}: <b>4</b>\n" +
                "${command.virus.stats.typeunsupported}: <b>7</b>\n";
        URL url = new URL("http://example.com");
        VirusTotalScannerImpl.AnalysesResponse analysesResponse = getSomeAnalysesResponse();

        when(propertiesConfig.getVirusTotalApiKey()).thenReturn("123");
        when(response.getBody()).thenReturn(analysesResponse);
        when(defaultRestTemplate.postForEntity(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<VirusTotalScannerImpl.AnalysesResponse>>any()))
                .thenReturn(response);
        when(reportResponse.getBody()).thenReturn(analysesResponse);
        when(defaultRestTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), ArgumentMatchers.<Class<VirusTotalScannerImpl.AnalysesResponse>>any()))
                .thenReturn(reportResponse);

        String actualString = virusTotalScanner.scan(url);
        assertEquals(expectedString, actualString);
    }

    @Test
    void scanFileTest() throws MalformedURLException, VirusScanException {
        final String expectedString = "<b>27.01.2024 09:12:55</b>\n" +
                "${command.virus.stats.total}: <b>3</b>\n" +
                "${command.virus.stats.confirmedtimeout}: <b>1</b>\n" +
                "${command.virus.stats.failure}: <b>2</b>\n" +
                "${command.virus.stats.harmless}: <b>3</b>\n" +
                "${command.virus.stats.undetected}: <b>8</b>\n" +
                "${command.virus.stats.suspicious}: <b>5</b>\n" +
                "${command.virus.stats.malicious}: <b>4</b>\n" +
                "${command.virus.stats.typeunsupported}: <b>7</b>\n";
        ByteArrayInputStream file = new ByteArrayInputStream("http://example.com".getBytes());
        VirusTotalScannerImpl.AnalysesResponse analysesResponse = getSomeAnalysesResponse();

        when(propertiesConfig.getVirusTotalApiKey()).thenReturn("123");
        when(response.getBody()).thenReturn(analysesResponse);
        when(defaultRestTemplate.postForEntity(anyString(), any(HttpEntity.class), ArgumentMatchers.<Class<VirusTotalScannerImpl.AnalysesResponse>>any()))
                .thenReturn(response);
        when(reportResponse.getBody()).thenReturn(analysesResponse);
        when(defaultRestTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), ArgumentMatchers.<Class<VirusTotalScannerImpl.AnalysesResponse>>any()))
                .thenReturn(reportResponse);

        String actualString = virusTotalScanner.scan(file);
        assertEquals(expectedString, actualString);
    }

    private VirusTotalScannerImpl.AnalysesResponse getSomeAnalysesResponse() {
        return new VirusTotalScannerImpl.AnalysesResponse()
                .setData(new VirusTotalScannerImpl.Analyses()
                        .setId("321")
                        .setAttributes(new VirusTotalScannerImpl.AnalysesResult()
                                .setDate(1706335975)
                                .setStatus(VirusTotalScannerImpl.AnalysesStatus.COMPLETED)
                                .setResults(Map.of("1", "1", "2", "2", "3", "3"))
                                .setStats(new VirusTotalScannerImpl.AnalysesStats()
                                        .setConfirmedTimeout(1)
                                        .setFailure(2)
                                        .setHarmless(3)
                                        .setMalicious(4)
                                        .setSuspicious(5)
                                        .setTimeout(6)
                                        .setTypeUnsupported(7)
                                        .setUndetected(8))));
    }

}