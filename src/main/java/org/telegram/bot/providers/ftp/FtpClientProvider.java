package org.telegram.bot.providers.ftp;

import org.apache.commons.net.ftp.FTPClient;
import org.springframework.stereotype.Component;

@Component
public class FtpClientProvider {
    public FTPClient getFTPClient() {
        return new FTPClient();
    }
}
