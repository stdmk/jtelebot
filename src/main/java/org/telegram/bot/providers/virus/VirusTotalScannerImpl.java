package org.telegram.bot.providers.virus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.exception.virus.VirusScanApiKeyMissingException;
import org.telegram.bot.exception.virus.VirusScanException;
import org.telegram.bot.exception.virus.VirusScanNoResponseException;
import org.telegram.bot.utils.DateUtils;
import org.telegram.bot.utils.MultipartInputStreamFileResource;

import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@RequiredArgsConstructor
@Service
@Slf4j
public class VirusTotalScannerImpl implements VirusScanner {

    private static final String AUTH_HEADER_NAME = "x-apikey";
    private static final String VIRUS_TOTAL_API_URL = "https://www.virustotal.com/api/v3";
    private static final String SCAN_FILE_PATH = "/files";
    private static final String SCAN_URL_PATH = "/urls";
    private static final String ANALYSES_REPORT_PATH = "/analyses";

    @Value("${virusTotalReportWaitingDelayMillis:15000}")
    private Integer reportWaitingTimeMillis;

    private final PropertiesConfig propertiesConfig;
    private final RestTemplate defaultRestTemplate;
    private final BotStats botStats;

    @Override
    public String scan(URL url) throws VirusScanException {
        String analysisId = sendUrlToScan(url);
        return getScanReport(analysisId);
    }

    @Override
    public String scan(InputStream file) throws VirusScanException {
        String analysisId = uploadFileToScan(file);
        return getScanReport(analysisId);
    }

    private String getScanReport(String id) throws VirusScanException {
        AnalysesResult analysesResult = null;
        boolean notCompleted = true;
        while (notCompleted) {
            try {
                Thread.sleep(reportWaitingTimeMillis);
            } catch (InterruptedException e) {
                log.error("Failed to sleep thread: ", e);
                Thread.currentThread().interrupt();
                botStats.incrementErrors(id, e, "Failed to sleep thread");
                throw new VirusScanException(e.getMessage());
            }

            analysesResult = getAnalysesResult(id);
            if (AnalysesStatus.COMPLETED.equals(analysesResult.getStatus())) {
                notCompleted = false;
            }
        }

        return buildScanResponseText(analysesResult);
    }

    private String buildScanResponseText(AnalysesResult analysesResult) throws VirusScanException {
        if (analysesResult == null) {
            log.error("Failed to build response scan text: result is null");
            throw new VirusScanException("Failed to build response scan text: result is null");
        }

        AnalysesStats stats = analysesResult.getStats();
        return "<b>" + DateUtils.formatDateTime(Instant.ofEpochSecond(analysesResult.getDate())) + "</b>\n"
                + "${command.virus.stats.total}: <b>" + analysesResult.getResults().size() + "</b>\n"
                + "${command.virus.stats.confirmedtimeout}: <b>" + toString(stats::getConfirmedTimeout) + "</b>\n"
                + "${command.virus.stats.failure}: <b>" + toString(stats::getFailure) + "</b>\n"
                + "${command.virus.stats.harmless}: <b>" + toString(stats::getHarmless) + "</b>\n"
                + "${command.virus.stats.undetected}: <b>" + toString(stats::getUndetected) + "</b>\n"
                + "${command.virus.stats.suspicious}: <b>" + toString(stats::getSuspicious) + "</b>\n"
                + "${command.virus.stats.malicious}: <b>" + toString(stats::getMalicious) + "</b>\n"
                + "${command.virus.stats.typeunsupported}: <b>" + toString(stats::getTypeUnsupported) + "</b>\n";
    }

    private String toString(Supplier<Integer> getter) {
        Integer param = getter.get();
        return param == null ? "0" : param.toString();
    }

    private String sendUrlToScan(URL url) throws VirusScanException {
        return sendRequest("url", url.toString(), VIRUS_TOTAL_API_URL + SCAN_URL_PATH);
    }

    private String uploadFileToScan(InputStream file) throws VirusScanException {
        return sendRequest("file", new MultipartInputStreamFileResource(file, "file"), VIRUS_TOTAL_API_URL + SCAN_FILE_PATH);
    }

    private synchronized String sendRequest(String key, Object value, String url) throws VirusScanException {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set(AUTH_HEADER_NAME, getApiKey());

        MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
        map.add(key, value);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(map, headers);

        ResponseEntity<AnalysesResponse> response;
        try {
            response = defaultRestTemplate.postForEntity(url, request, AnalysesResponse.class);
        } catch (Exception e) {
            log.error("Failed to send request to virustotal", e);
            throw new VirusScanNoResponseException(e.getMessage());
        }

        return Optional.ofNullable(response.getBody())
                .map(AnalysesResponse::getData)
                .map(Analyses::getId)
                .orElseThrow(() -> new VirusScanNoResponseException("Failed to send request to virustotal: empty response body"));
    }

    private AnalysesResult getAnalysesResult(String id) throws VirusScanException {
        HttpHeaders headers = new HttpHeaders();
        headers.set(AUTH_HEADER_NAME, getApiKey());

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<AnalysesResponse> response;
        try {
            response = defaultRestTemplate.exchange(VIRUS_TOTAL_API_URL + ANALYSES_REPORT_PATH + "/" + id, HttpMethod.GET, request, AnalysesResponse.class);
        } catch (Exception e) {
            log.error("Failed to get analyses response", e);
            throw new VirusScanNoResponseException(e.getMessage());
        }

        return Optional.ofNullable(response.getBody())
                .map(AnalysesResponse::getData)
                .map(Analyses::getAttributes)
                .orElseThrow(() -> new VirusScanNoResponseException("Failed to get report of file scan: empty response body"));
    }

    private String getApiKey() throws VirusScanApiKeyMissingException {
        String apiKey = propertiesConfig.getVirusTotalApiKey();
        if (apiKey == null) {
            throw new VirusScanApiKeyMissingException("Api key is missing");
        }

        return apiKey;
    }

    @Data
    @Accessors(chain = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AnalysesResponse {
        private Analyses data;
    }

    @Data
    @Accessors(chain = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Analyses {
        private AnalysesResult attributes;
        private String id;
        private String type;
    }

    @Data
    @Accessors(chain = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AnalysesResult {
        private Integer date;
        private AnalysesStats stats;
        private AnalysesStatus status;
        private Map<String, Object> results;
    }

    @Data
    @Accessors(chain = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AnalysesStats {
        @JsonProperty("confirmed-timeout")
        private Integer confirmedTimeout;
        private Integer failure;
        private Integer harmless;
        private Integer malicious;
        private Integer suspicious;
        private Integer timeout;
        @JsonProperty("type-unsupported")
        private Integer typeUnsupported;
        private Integer undetected;
    }

    @Data
    @Accessors(chain = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScanResult {
        private String category;
        @JsonProperty("engine_name")
        private String engineName;
        @JsonProperty("engine_version")
        private String engineVersion;
        private String result;
        private String method;
        @JsonProperty("engine_update")
        private String engineUpdate;
    }

    @RequiredArgsConstructor
    @Getter
    public enum AnalysesStatus {
        COMPLETED("completed"),
        QUEUED("queued"),
        IN_PROGRESS("in-progress");

        @JsonValue
        private final String status;
    }

}
