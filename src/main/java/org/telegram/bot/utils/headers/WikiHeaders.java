package org.telegram.bot.utils.headers;

import lombok.experimental.UtilityClass;
import org.springframework.http.HttpHeaders;

@UtilityClass
public class WikiHeaders {

    private static final HttpHeaders DEFAULT_HEADERS = new HttpHeaders();
    static {
        DEFAULT_HEADERS.set(HttpHeaders.USER_AGENT, "jtelebot/1.0 (https://github.com/stdmk/jtelebot)");
    }

    public HttpHeaders getDefaultHeaders() {
        return DEFAULT_HEADERS;
    }

}
