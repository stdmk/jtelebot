package org.telegram.bot.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;
import org.telegram.bot.config.telegram.TelegramProxyProperties;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.starter.TelegramBotInitializer;
import org.telegram.telegrambots.meta.TelegramUrl;
import org.telegram.telegrambots.meta.api.methods.updates.GetUpdates;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import javax.net.ssl.SSLContext;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

@Configuration
public class Config {

    private static final List<String> ALLOWED_UPDATES = List.of(
            "message",
            "edited_message",
            "channel_post",
            "edited_channel_post",
            "inline_query",
            "chosen_inline_result",
            "callback_query",
            "my_chat_member",
            "chat_member",
            "chat_join_request",
            "message_reaction",
            "message_reaction_count");

    @Value("${defaultRequestTimeoutSeconds:60}")
    private Integer defaultRequestTimeoutSeconds;

    @Value("${sberApiRequestTimeoutSeconds:60}")
    private Integer sberApiRequestTimeoutSeconds;

    @Value("${telegramBotApiToken}")
    private String telegramBotApiToken;

    @Bean
    public TelegramBotInitializer telegramBotInitializer(TelegramBotsLongPollingApplication telegramBotsApplication,
                                                         ObjectProvider<List<SpringLongPollingBot>> longPollingBots) {
        return new TelegramBotInitializer(telegramBotsApplication,
                longPollingBots.getIfAvailable(Collections::emptyList));
    }

    @Bean
    public TelegramBotsLongPollingApplication telegramBotsApplication() {
        return new TelegramBotsLongPollingApplication() {
            @Override
            public BotSession registerBot(String botToken, Supplier<TelegramUrl> telegramUrlSupplier, Function<Integer, GetUpdates> getUpdatesGenerator, LongPollingUpdateConsumer updatesConsumer) throws TelegramApiException {
                Function<Integer, GetUpdates> customGenerator = (offset) ->
                        GetUpdates.builder()
                                .offset(offset + 1)
                                .timeout(50)
                                .limit(100)
                                .allowedUpdates(ALLOWED_UPDATES)
                                .build();

                return super.registerBot(botToken, telegramUrlSupplier, customGenerator, updatesConsumer);
            }
        };
    }

    @Bean
    public OkHttpClient telegramOkHttpClient(TelegramProxyProperties proxyProps) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .protocols(List.of(Protocol.HTTP_1_1))
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(proxyProps.getEnabled())) {
            Proxy.Type proxyType = proxyProps.getType() == TelegramProxyProperties.ProxyType.SOCKS
                    ? Proxy.Type.SOCKS
                    : Proxy.Type.HTTP;

            Proxy proxy = new Proxy(proxyType, new InetSocketAddress(proxyProps.getHost(), proxyProps.getPort()));

            builder.proxy(proxy);

            if (proxyProps.getUsername() != null && !proxyProps.getUsername().isBlank()) {
                if (proxyType == Proxy.Type.HTTP) {
                    builder.proxyAuthenticator((route, response) -> {
                        String credential = Credentials.basic(
                                proxyProps.getUsername(),
                                proxyProps.getPassword());
                        return response.request().newBuilder()
                                .header("Proxy-Authorization", credential)
                                .build();
                    });
                } else {
                    Authenticator.setDefault(new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            if (getRequestingHost().equalsIgnoreCase(proxyProps.getHost()) && getRequestingPort() == proxyProps.getPort()) {
                                return new PasswordAuthentication(proxyProps.getUsername(), proxyProps.getPassword().toCharArray());
                            }

                            return null;
                        }
                    });
                }
            }
        }

        return builder.build();
    }

    @Bean
    @Primary
    public TelegramClient telegramClient(OkHttpClient telegramOkHttpClient) {
        return new OkHttpTelegramClient(telegramOkHttpClient, telegramBotApiToken);
    }

    @Bean
    public RestTemplate botRestTemplate() {
        int timeoutSeconds = defaultRequestTimeoutSeconds;

        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(timeoutSeconds))
                .setSocketTimeout(Timeout.ofSeconds(timeoutSeconds))
                .build();

        PoolingHttpClientConnectionManager connectionManager =
                PoolingHttpClientConnectionManagerBuilder.create()
                        .setDefaultConnectionConfig(connectionConfig)
                        .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(timeoutSeconds))
                .setResponseTimeout(Timeout.ofSeconds(timeoutSeconds))
                .build();

        HttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        return new RestTemplate(requestFactory);
    }

    @Bean
    public RestTemplate sberRestTemplate() throws Exception {
        int timeoutSeconds = sberApiRequestTimeoutSeconds;

        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial((chain, authType) -> true)
                .build();

        SSLConnectionSocketFactory sslSocketFactory =
                new SSLConnectionSocketFactory(sslContext);

        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(timeoutSeconds))
                .setSocketTimeout(Timeout.ofSeconds(timeoutSeconds)) // важно
                .build();

        PoolingHttpClientConnectionManager connectionManager =
                PoolingHttpClientConnectionManagerBuilder.create()
                        .setSSLSocketFactory(sslSocketFactory)
                        .setDefaultConnectionConfig(connectionConfig)
                        .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(timeoutSeconds)) // важно
                .setResponseTimeout(Timeout.ofSeconds(timeoutSeconds))
                .build();

        HttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        return new RestTemplate(requestFactory);
    }

    @Bean
    public RestTemplate defaultRestTemplate() {
        int timeoutSeconds = defaultRequestTimeoutSeconds;

        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(timeoutSeconds))
                .setSocketTimeout(Timeout.ofSeconds(timeoutSeconds)) // важно
                .build();

        PoolingHttpClientConnectionManager connectionManager =
                PoolingHttpClientConnectionManagerBuilder.create()
                        .setDefaultConnectionConfig(connectionConfig)
                        .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(timeoutSeconds)) // важно
                .setResponseTimeout(Timeout.ofSeconds(timeoutSeconds))
                .build();

        HttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        return new RestTemplate(requestFactory);
    }

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        JsonMapper jsonMapper = new JsonMapper();
        jsonMapper.findAndRegisterModules();
        return jsonMapper;
    }

    @Bean
    public XmlMapper xmlMapper() {
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.findAndRegisterModules();
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return xmlMapper;
    }

    @Bean
    public LocaleResolver localeResolver() {
        FixedLocaleResolver fixedLocaleResolver = new FixedLocaleResolver();
        fixedLocaleResolver.setDefaultLocale(Locale.ENGLISH);
        return fixedLocaleResolver;
    }

    @Bean
    public ResourceBundleMessageSource bundleMessageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.displayName());
        return messageSource;
    }

}
