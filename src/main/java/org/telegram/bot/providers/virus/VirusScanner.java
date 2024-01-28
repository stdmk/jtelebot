package org.telegram.bot.providers.virus;

import org.telegram.bot.exception.virus.VirusScanException;

import java.io.InputStream;
import java.net.URL;

public interface VirusScanner {

    String scan(URL url) throws VirusScanException;

    String scan(InputStream file) throws VirusScanException;

}
