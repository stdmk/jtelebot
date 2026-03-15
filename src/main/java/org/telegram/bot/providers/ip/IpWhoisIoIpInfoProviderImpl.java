package org.telegram.bot.providers.ip;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.domain.model.Coordinates;
import org.telegram.bot.domain.model.whois.IpInfo;
import org.telegram.bot.exception.ip.IpInfoException;
import org.telegram.bot.exception.ip.IpInfoNoResponseException;

@RequiredArgsConstructor
@Component
@Slf4j
public class IpWhoisIoIpInfoProviderImpl implements IpInfoProvider {

    private static final String API_URL = "https://ipwho.is/";

    private final RestTemplate botRestTemplate;

    @Override
    public IpInfo getData(String ip, String lang) throws IpInfoException {
        String url = API_URL + ip;

        IpInfoDto ipInfoDto;
        if (lang != null) {
            ipInfoDto = getIpInfo(url, lang);
        } else {
            ipInfoDto = getIpInfo(url);
        }

        return toIpInfo(ipInfoDto);
    }

    private IpInfoDto getIpInfo(String url, String lang) throws IpInfoException {
        return getIpInfo(url + "?lang=" + lang);
    }

    @NotNull
    private IpInfoDto getIpInfo(String url) throws IpInfoException {
        System.out.println(url);
        try {
            ResponseEntity<IpInfoDto> response = botRestTemplate.exchange(url, HttpMethod.GET, null, IpInfoDto.class);

            IpInfoDto body = response.getBody();
            if (body == null) {
                throw new IpInfoNoResponseException("Empty response from ipwhois");
            }

            if (!body.isSuccess()) {
                throw new IpInfoException("API error: " + body.getMessage());
            }

            return body;
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new IpInfoException("API error: " + e.getStatusCode());
        } catch (ResourceAccessException e) {
            throw new IpInfoNoResponseException("Failed to access ipwhois API");
        }
    }

    private IpInfo toIpInfo(IpInfoDto dto) {
        IpInfo ipInfo = new IpInfo()
                .setIp(dto.getIp())
                .setType(dto.getType())
                .setContinent(dto.getContinent())
                .setCountry(dto.getCountry())
                .setRegion(dto.getRegion())
                .setCity(dto.getCity());

        if (dto.getLatitude() != 0 && dto.getLongitude() != 0) {
            ipInfo.setCoordinates(new Coordinates(dto.getLatitude(), dto.getLongitude()));
        }

        if (dto.getFlag() != null) {
            ipInfo.setFlagEmoji(dto.getFlag().getEmoji());
        }

        if (dto.getConnection() != null) {
            ipInfo.setAsn(dto.getConnection().getAsn())
                    .setOrg(dto.getConnection().getOrg())
                    .setIsp(dto.getConnection().getIsp())
                    .setDomain(dto.getConnection().getDomain());
        }

        return ipInfo;
    }

    @Data
    @Accessors(chain = true)
    public static class IpInfoDto {
        private String ip;

        private boolean success;

        private String message;

        private String type;

        private String continent;

        @JsonProperty("continent_code")
        private String continentCode;

        private String country;

        @JsonProperty("country_code")
        private String countryCode;

        private String region;

        @JsonProperty("region_code")
        private String regionCode;

        private String city;

        private double latitude;

        private double longitude;

        @JsonProperty("is_eu")
        private boolean isEu;

        private String postal;

        @JsonProperty("calling_code")
        private String callingCode;

        private String capital;

        private String borders;

        private Flag flag;

        private Connection connection;

        private Timezone timezone;
    }

    @Data
    @Accessors(chain = true)
    public static class Flag {
        private String img;

        private String emoji;

        @JsonProperty("emoji_unicode")
        private String emojiUnicode;
    }

    @Data
    @Accessors(chain = true)
    public static class Connection {
        private long asn;
        private String org;
        private String isp;
        private String domain;
    }

    @Data
    @Accessors(chain = true)
    public static class Timezone {
        private String id;

        private String abbr;

        @JsonProperty("is_dst")
        private boolean isDst;

        private int offset;

        private String utc;
    }

}
