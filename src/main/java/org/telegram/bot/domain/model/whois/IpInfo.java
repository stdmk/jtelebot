package org.telegram.bot.domain.model.whois;

import lombok.Data;
import lombok.experimental.Accessors;
import org.telegram.bot.domain.model.Coordinates;

@Data
@Accessors(chain = true)
public class IpInfo {
    private String ip;
    private String type;
    private String continent;
    private String country;
    private String region;
    private String city;
    private Coordinates coordinates;
    private String flagEmoji;
    private Long asn;
    private String org;
    private String isp;
    private String domain;
}
