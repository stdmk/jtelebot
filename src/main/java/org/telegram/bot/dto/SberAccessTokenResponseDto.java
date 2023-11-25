package org.telegram.bot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Response to getting access token request.
 */
@Data
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SberAccessTokenResponseDto {

    /**
     * Api access token.
     */
    @JsonProperty("access_token")
    private String accessToken;

    /**
     * Date of access token expire.
     */
    @JsonProperty("expires_at")
    private Long expiresAt;

}
