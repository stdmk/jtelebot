package org.telegram.bot.providers.groq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.bot.config.ConditionalOnPropertyNotEmpty;
import org.telegram.bot.exception.speech.SpeechParseException;
import org.telegram.bot.providers.SpeechParser;

@RequiredArgsConstructor
@Service
@Slf4j
@ConditionalOnPropertyNotEmpty("groqApiKey")
public class GroqSpeechParser implements SpeechParser {

    private static final String GROQ_SPEECH_API_URL = "https://api.groq.com/openai/v1/audio/transcriptions";
    private static final String MODEL = "whisper-large-v3-turbo";

    private final RestTemplate botRestTemplate;

    @Value("${groqApiKey}")
    private String groqApiKey;

    @Override
    public String parse(byte[] file, Integer duration) throws SpeechParseException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(groqApiKey);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        body.add("file", new ByteArrayResource(file) {
            @Override
            public String getFilename() {
                return "voice.ogg";
            }
        });
        body.add("model", MODEL);
        body.add("language", "ru");

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<SpeechRecognizeResult> response;

        try {
            response = botRestTemplate.postForEntity(GROQ_SPEECH_API_URL, request, SpeechRecognizeResult.class);
        } catch (RestClientException e) {
            log.error("Failed to get response from Groq Speech", e);
            throw new SpeechParseException(e.getMessage());
        }

        SpeechRecognizeResult speechRecognizeResult = response.getBody();

        if (speechRecognizeResult == null) {
            log.error("Failed to get response from Groq Speech: empty body");
            throw new SpeechParseException("Failed to get response from Groq Speech: empty body");
        }

        return speechRecognizeResult.getText();
    }

    @Data
    @Accessors(chain = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SpeechRecognizeResult {

        @JsonProperty("text")
        private String text;
    }
}