package org.telegram.bot.timers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.bot.Bot;
import org.telegram.bot.config.ConditionalOnPropertyNotEmpty;
import org.telegram.bot.config.email.EmailProperties;
import org.telegram.bot.domain.entities.Timer;

import javax.mail.*;
import javax.mail.search.FlagTerm;

@RequiredArgsConstructor
@Component
@Slf4j
@ConditionalOnPropertyNotEmpty("mail.imaps.host")
public class EmailTimer extends Timer {

    private final Bot bot;
    private final EmailProperties.ImapsConfig imapsConfig;
    private final Session imapsSession;

    @Scheduled(fixedRate = 10000)
    public void execute() throws MessagingException {
        if (imapsConfig == null) {
            return;
        }

        Store store = imapsSession.getStore();
        store.connect(imapsConfig.getUser(), imapsConfig.getPassword());

        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_WRITE);

        Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));

        for (Message message : messages) {
            bot.consume(message);
            message.setFlag(Flags.Flag.SEEN, true);
        }

        inbox.close(false);
        store.close();
    }

}
