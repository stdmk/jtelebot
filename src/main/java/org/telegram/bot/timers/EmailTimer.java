package org.telegram.bot.timers;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.mail.*;
import jakarta.mail.event.MessageCountAdapter;
import jakarta.mail.event.MessageCountEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.angus.mail.imap.IMAPFolder;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.config.ConditionalOnPropertyNotEmpty;
import org.telegram.bot.config.email.EmailProperties;
import org.telegram.bot.services.BotStats;

@RequiredArgsConstructor
@Component
@Slf4j
@ConditionalOnPropertyNotEmpty("mail.imaps.host")
public class EmailTimer {

    private final Bot bot;
    private final BotStats botStats;
    private final EmailProperties.ImapsConfig imapsConfig;
    private final Session imapsSession;

    private volatile boolean running = true;

    @PostConstruct
    public void start() {
        Thread thread = new Thread(this::runIdleLoop, "imap-idle-thread");
        thread.setDaemon(true);
        thread.start();
    }

    private void runIdleLoop() {
        while (running) {
            try {
                connectAndIdle();
            } catch (FolderClosedException e) {
                log.debug("IMAP session closed by server (timeout). Reconnecting...");
            } catch (MessagingException e) {
                log.error("IMAP messaging problem. Reconnecting...", e);
            } catch (Exception e) {
                String errorMessage = "Unexpected IMAP error: " + e.getMessage();
                log.error(errorMessage, e);
                botStats.incrementErrors(e, errorMessage);
            }

            sleep();
        }
    }

    private void connectAndIdle() throws Exception {
        Store store = imapsSession.getStore("imaps");
        store.connect(imapsConfig.getUser(), imapsConfig.getPassword());

        IMAPFolder inbox = (IMAPFolder) store.getFolder("INBOX");
        inbox.open(Folder.READ_WRITE);

        log.info("IMAP IDLE connected");

        inbox.addMessageCountListener(new MessageCountAdapter() {
            @Override
            public void messagesAdded(MessageCountEvent event) {
                for (Message message : event.getMessages()) {
                    try {
                        bot.consume(message);
                        message.setFlag(Flags.Flag.DELETED, true);
                    } catch (Exception e) {
                        log.error("Failed to process email message", e);
                    }
                }
            }
        });

        while (running && store.isConnected()) {
            inbox.idle();
        }

        inbox.close(true);
        store.close();
    }

    @PreDestroy
    public void stop() {
        running = false;
    }

    private void sleep() {
        try {
            Thread.sleep(10000);
        } catch (InterruptedException ignored) {}
    }
}
