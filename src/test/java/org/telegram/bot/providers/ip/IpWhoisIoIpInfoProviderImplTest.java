package org.telegram.bot.providers.ip;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.TestUtils;
import org.telegram.bot.domain.model.whois.IpInfo;
import org.telegram.bot.exception.ip.IpInfoException;
import org.telegram.bot.exception.ip.IpInfoNoResponseException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IpWhoisIoIpInfoProviderImplTest {

    @Mock
    private RestTemplate botRestTemplate;

    @InjectMocks
    private IpWhoisIoIpInfoProviderImpl ipInfoProvider;

    @Mock
    private ResponseEntity<IpWhoisIoIpInfoProviderImpl.IpInfoDto> response;

    @Test
    void getDataHttpClientErrorExceptionTest() {
        final String expectedErrorText = "API error: 400 BAD_REQUEST";
        final String ip = "127.0.0.1";
        final String expectedUrl = "https://ipwho.is/" + ip;

        when(botRestTemplate.exchange(expectedUrl, HttpMethod.GET, null, IpWhoisIoIpInfoProviderImpl.IpInfoDto.class))
                .thenThrow(new HttpClientErrorException(HttpStatusCode.valueOf(400)));

        IpInfoException ipInfoException = assertThrows((IpInfoException.class), () -> ipInfoProvider.getData(ip, null));

        assertEquals(expectedErrorText, ipInfoException.getMessage());
    }

    @Test
    void getDataHttpServerErrorExceptionTest() {
        final String expectedErrorText = "API error: 500 INTERNAL_SERVER_ERROR";
        final String ip = "127.0.0.1";
        final String expectedUrl = "https://ipwho.is/" + ip;

        when(botRestTemplate.exchange(expectedUrl, HttpMethod.GET, null, IpWhoisIoIpInfoProviderImpl.IpInfoDto.class))
                .thenThrow(new HttpServerErrorException(HttpStatusCode.valueOf(500)));

        IpInfoException ipInfoException = assertThrows((IpInfoException.class), () -> ipInfoProvider.getData(ip, null));

        assertEquals(expectedErrorText, ipInfoException.getMessage());
    }

    @Test
    void getDataResourceAccessExceptionTest() {
        final String expectedErrorText = "Failed to access ipwhois API";
        final String ip = "127.0.0.1";
        final String expectedUrl = "https://ipwho.is/" + ip;

        when(botRestTemplate.exchange(expectedUrl, HttpMethod.GET, null, IpWhoisIoIpInfoProviderImpl.IpInfoDto.class))
                .thenThrow(new ResourceAccessException(expectedErrorText));

        IpInfoNoResponseException ipInfoException = assertThrows((IpInfoNoResponseException.class), () -> ipInfoProvider.getData(ip, null));

        assertEquals(expectedErrorText, ipInfoException.getMessage());
    }

    @Test
    void getDataNullableResponseTest() {
        final String expectedErrorText = "Empty response from ipwhois";
        final String ip = "127.0.0.1";
        final String expectedUrl = "https://ipwho.is/" + ip;

        when(botRestTemplate.exchange(expectedUrl, HttpMethod.GET, null, IpWhoisIoIpInfoProviderImpl.IpInfoDto.class))
                .thenReturn(response);

        IpInfoNoResponseException ipInfoException = assertThrows((IpInfoNoResponseException.class), () -> ipInfoProvider.getData(ip, null));

        assertEquals(expectedErrorText, ipInfoException.getMessage());
    }

    @Test
    void getDataUnSuccessResponseTest() {
        final String errorText = "Failed to access ipwhois API";
        final String expectedErrorText = "API error: " + errorText;
        final String ip = "127.0.0.1";
        final String expectedUrl = "https://ipwho.is/" + ip;

        IpWhoisIoIpInfoProviderImpl.IpInfoDto ipInfoDto = new IpWhoisIoIpInfoProviderImpl.IpInfoDto()
                .setSuccess(false)
                .setMessage(errorText);

        when(response.getBody()).thenReturn(ipInfoDto);
        when(botRestTemplate.exchange(expectedUrl, HttpMethod.GET, null, IpWhoisIoIpInfoProviderImpl.IpInfoDto.class))
                .thenReturn(response);

        IpInfoException ipInfoException = assertThrows((IpInfoException.class), () -> ipInfoProvider.getData(ip, null));

        assertEquals(expectedErrorText, ipInfoException.getMessage());
    }

    @Test
    void getDataMinDataTest() throws IpInfoException {
        final String ip = "127.0.0.1";
        final String lang = "en";
        final String expectedUrl = "https://ipwho.is/" + ip + "?lang=" + lang;

        IpWhoisIoIpInfoProviderImpl.IpInfoDto ipInfoDto = new IpWhoisIoIpInfoProviderImpl.IpInfoDto()
                .setSuccess(true);

        when(response.getBody()).thenReturn(ipInfoDto);
        when(botRestTemplate.exchange(expectedUrl, HttpMethod.GET, null, IpWhoisIoIpInfoProviderImpl.IpInfoDto.class))
                .thenReturn(response);

        IpInfo ipInfo = ipInfoProvider.getData(ip, lang);

        TestUtils.assertAllFieldsAreNull(ipInfo);
    }

    @Test
    void getDataAllDataTest() throws IpInfoException {
        final String ip = "127.0.0.1";
        final String lang = "en";
        final String expectedUrl = "https://ipwho.is/" + ip + "?lang=" + lang;

        IpWhoisIoIpInfoProviderImpl.IpInfoDto ipInfoDto = new IpWhoisIoIpInfoProviderImpl.IpInfoDto()
                .setSuccess(true);

        when(response.getBody()).thenReturn(ipInfoDto);
        when(botRestTemplate.exchange(expectedUrl, HttpMethod.GET, null, IpWhoisIoIpInfoProviderImpl.IpInfoDto.class))
                .thenReturn(response);

        IpInfo ipInfo = ipInfoProvider.getData(ip, lang);

        TestUtils.assertAllFieldsAreNull(ipInfo);
    }

    @Test
    void getDataMappingTest() throws IpInfoException {
        final String ip = "127.0.0.1";
        final String lang = "en";
        final String expectedUrl = "https://ipwho.is/" + ip + "?lang=" + lang;

        IpWhoisIoIpInfoProviderImpl.Flag flag = new IpWhoisIoIpInfoProviderImpl.Flag()
                .setEmoji("🏴");

        IpWhoisIoIpInfoProviderImpl.Connection connection = new IpWhoisIoIpInfoProviderImpl.Connection()
                .setAsn(64512L)
                .setOrg("Local Network Org")
                .setIsp("Loopback ISP")
                .setDomain("localhost.net");

        IpWhoisIoIpInfoProviderImpl.IpInfoDto ipInfoDto = new IpWhoisIoIpInfoProviderImpl.IpInfoDto()
                .setSuccess(true)
                .setIp(ip)
                .setType("IPv4")
                .setContinent("Localhost Continent")
                .setCountry("Local Country")
                .setRegion("Loopback Region")
                .setCity("Local City")
                .setLatitude(1.23)
                .setLongitude(4.56)
                .setFlag(flag)
                .setConnection(connection);

        when(response.getBody()).thenReturn(ipInfoDto);
        when(botRestTemplate.exchange(expectedUrl, HttpMethod.GET, null, IpWhoisIoIpInfoProviderImpl.IpInfoDto.class))
                .thenReturn(response);

        IpInfo ipInfo = ipInfoProvider.getData(ip, lang);

        assertEquals(ip, ipInfo.getIp());
        assertEquals("IPv4", ipInfo.getType());
        assertEquals("Localhost Continent", ipInfo.getContinent());
        assertEquals("Local Country", ipInfo.getCountry());
        assertEquals("Loopback Region", ipInfo.getRegion());
        assertEquals("Local City", ipInfo.getCity());

        assertNotNull(ipInfo.getCoordinates());
        assertEquals(1.23, ipInfo.getCoordinates().latitude());
        assertEquals(4.56, ipInfo.getCoordinates().longitude());

        assertEquals("🏴", ipInfo.getFlagEmoji());

        assertEquals(64512L, ipInfo.getAsn());
        assertEquals("Local Network Org", ipInfo.getOrg());
        assertEquals("Loopback ISP", ipInfo.getIsp());
        assertEquals("localhost.net", ipInfo.getDomain());
    }

}