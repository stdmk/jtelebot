package org.telegram.bot.config.email;

import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.bot.config.ConditionalOnPropertyNotEmpty;
import org.telegram.bot.domain.model.request.BotRequest;
import org.telegram.bot.domain.model.response.BotResponse;
import org.telegram.bot.services.EmailNotifier;
import org.telegram.bot.services.executors.email.EmailExecutor;

import java.util.List;
import java.util.Optional;
import java.util.Properties;

@RequiredArgsConstructor
@Configuration
@EnableConfigurationProperties(EmailProperties.class)
@Slf4j
public class EmailConfig {

    private final EmailProperties emailProperties;

    @Bean("botEmailAddress")
    @ConditionalOnPropertyNotEmpty("mail.smtp.host")
    public InternetAddress botEmailAddress() {
        EmailProperties.SmtpConfig smtpConfig = smtpConfig();
        if (smtpConfig != null) {
            try {
                return new InternetAddress(smtpConfig.getUser());
            } catch (AddressException e) {
                log.error("bot email address is incorrect");
            }
        }

        return null;
    }

    @Bean("smtpSession")
    @ConditionalOnPropertyNotEmpty("mail.smtp.host")
    public Session smtpSession() {
        EmailProperties.SmtpConfig smtpConfig = smtpConfig();
        if (smtpConfig != null) {
            Authenticator authenticator = new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(smtpConfig.getUser(), smtpConfig.getPassword());
                }
            };

            Properties connectionProperties = new Properties();
            connectionProperties.put("mail.transport.protocol", "smtp");
            connectionProperties.put("mail.smtp.host", smtpConfig.getHost());
            connectionProperties.put("mail.smtp.port", smtpConfig.getPort());
            connectionProperties.put("mail.smtp.auth", Boolean.TRUE.equals(smtpConfig.getAuth()));
            connectionProperties.put("mail.smtp.ssl.enable", Optional.of(smtpConfig).map(EmailProperties.SmtpConfig::getSsl).map(EmailProperties.SslConfig::getEnable).orElse(false));
            connectionProperties.put("mail.smtp.connectiontimeout", "10000");
            connectionProperties.put("mail.smtp.timeout", "10000");
            connectionProperties.put("mail.smtp.writetimeout", "10000");

            return Session.getInstance(connectionProperties, authenticator);
        }

        return null;
    }

    @Bean("imapsSession")
    @ConditionalOnPropertyNotEmpty("mail.imaps.host")
    public Session imapsSession() {
        EmailProperties.ImapsConfig imapsConfig = imapsConfig();

        Properties connectionProperties = new Properties();
        connectionProperties.put("mail.store.protocol", "imaps");
        connectionProperties.put("mail.imaps.host", imapsConfig.getHost());
        connectionProperties.put("mail.imaps.port", imapsConfig.getPort());
        connectionProperties.put("mail.imaps.ssl.enable", Optional.of(imapsConfig).map(EmailProperties.ImapsConfig::getSsl).map(EmailProperties.SslConfig::getEnable).orElse(false));
        connectionProperties.put("mail.imaps.peek", Boolean.TRUE.equals(imapsConfig.getPeek()));

        return Session.getInstance(connectionProperties);
    }

    @Bean("imapsConfig")
    @ConditionalOnPropertyNotEmpty("mail.imaps.host")
    public EmailProperties.ImapsConfig imapsConfig() {
        return Optional.ofNullable(emailProperties)
                .map(EmailProperties::getImaps)
                .orElse(null);
    }

    @Bean("smtpConfig")
    @ConditionalOnPropertyNotEmpty("mail.smtp.host")
    public EmailProperties.SmtpConfig smtpConfig() {
        return Optional.ofNullable(emailProperties)
                .map(EmailProperties::getSmtp)
                .orElse(null);
    }

    @Bean
    @ConditionalOnMissingBean(EmailExecutor.class)
    public EmailExecutor emailExecutor() {
        return new EmailExecutor() {
            @Override
            public void execute(List<BotResponse> botResponses, BotRequest request) {

            }

            @Override
            public void execute(BotResponse botResponse) {

            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(EmailNotifier.class)
    public EmailNotifier emailNotifier() {
        return botRequest -> {};
    }

}
