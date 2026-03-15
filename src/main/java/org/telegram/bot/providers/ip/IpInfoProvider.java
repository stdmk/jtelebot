package org.telegram.bot.providers.ip;

import jakarta.annotation.Nullable;
import org.telegram.bot.domain.model.whois.IpInfo;
import org.telegram.bot.exception.ip.IpInfoException;

public interface IpInfoProvider {
    IpInfo getData(String ip, @Nullable String lang) throws IpInfoException;
}
