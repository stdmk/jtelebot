package org.telegram.bot.timers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.bot.config.PropertiesConfig;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.entities.Timer;
import org.telegram.bot.domain.entities.TvChannel;
import org.telegram.bot.domain.entities.TvProgram;
import org.telegram.bot.exception.BotException;
import org.telegram.bot.services.TimerService;
import org.telegram.bot.services.TvChannelService;
import org.telegram.bot.services.TvProgramService;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static org.telegram.bot.utils.DateUtils.atStartOfDay;

@Component
@RequiredArgsConstructor
@Slf4j
public class TvProgramDownloaderTimer extends TimerParent {

    private static final String TV_PROGRAM_DATA_XML_FILE_NAME = "tvguide.xml";

    private final TimerService timerService;
    private final TvChannelService tvChannelService;
    private final TvProgramService tvProgramService;
    private final PropertiesConfig propertiesConfig;
    private final BotStats botStats;

    @Override
    @Scheduled(fixedRate = 14400000)
    public void execute() {
        Timer timer = timerService.get("tvProgramDownloader");
        if (timer == null) {
            timer = new Timer()
                    .setName("tvProgramDownloader")
                    .setLastAlarmDt(LocalDateTime.now().minusDays(1));
            timerService.save(timer);
        }

        String xmlTvFileUrl = propertiesConfig.getXmlTvFileUrl();
        if (xmlTvFileUrl == null) {
            log.warn("xmlTvFileUrl is missing. TV program will not load");
            return;
        }

        LocalDateTime dateTimeNow = LocalDateTime.now();
        LocalDateTime nextAlarm = timer.getLastAlarmDt().plusDays(1);

        if (dateTimeNow.isAfter(nextAlarm)) {
            log.info("Timer for downloading and transferring tv-program");

            try {
                transferTvProgramDataFile(xmlTvFileUrl);
            } catch (IOException e) {
                log.error("Failed to download tv program data: {}", e.getMessage());
                return;
            }

            clearTvTables();

            try {
                parseTvProgramData();
            } catch (IOException | XMLStreamException e) {
                log.error("Unable to read new TvData: " + e.getMessage());
                return;
            }

            botStats.setLastTvUpdate(Instant.now());

            timer.setLastAlarmDt(atStartOfDay(dateTimeNow));
            timerService.save(timer);

            log.info("Timer for downloading and transferring tv-program completed successfully");
        }
    }

    /**
     * Downloading and unzip tv program data file.
     *
     * @throws IOException if failed to transfer file.
     */
    private void transferTvProgramDataFile(String xmlTvFileUrl) throws IOException {
        String zipFileName = xmlTvFileUrl.substring(xmlTvFileUrl.lastIndexOf("/") + 1);

        FileUtils.copyURLToFile(new URL(xmlTvFileUrl), new File(zipFileName));

        try (GZIPInputStream in = new GZIPInputStream(new FileInputStream(zipFileName));
             OutputStream out = new FileOutputStream(TV_PROGRAM_DATA_XML_FILE_NAME)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
        }
    }

    void clearTvTables() {
        tvProgramService.clearTable();
        tvChannelService.clearTable();
    }

    /**
     * Parsing tv data from file.
     *
     * @throws IOException if failed to read.
     * @throws XMLStreamException if failed to parse xml.
     */
    private void parseTvProgramData() throws IOException, XMLStreamException {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        XMLEventReader reader = xmlInputFactory.createXMLEventReader(new FileInputStream(TV_PROGRAM_DATA_XML_FILE_NAME));

        TvChannel tvChannel = null;
        List<Integer> tvChannelIdList = new ArrayList<>();
        TvProgram tvProgram = null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyMMddHHmmss");

        int attemptsReadNumber = 0;
        while (reader.hasNext()) {
            if (attemptsReadNumber > 10) {
                log.error("Unable to read tv file");
                throw new BotException("Unable to read tv file");
            }

            XMLEvent nextEvent;
            try {
                nextEvent = reader.nextEvent();
            } catch (XMLStreamException e) {
                attemptsReadNumber = attemptsReadNumber + 1;
                continue;
            }

            attemptsReadNumber = 0;
            if (nextEvent.isStartElement()) {
                StartElement startElement = nextEvent.asStartElement();
                switch (startElement.getName().getLocalPart()) {
                    case "channel": {
                        Integer channelId = Integer.parseInt(startElement.getAttributeByName(new QName("id")).getValue());
                        tvChannelIdList.add(channelId);
                        tvChannel = new TvChannel().setId(channelId);
                        break;
                    }
                    case "display-name": {
                        nextEvent = reader.nextEvent();
                        if (tvChannel != null) {
                            tvChannel.setName(nextEvent.asCharacters().getData());
                        }
                        break;
                    }
                    case "programme": {
                        Integer channelId = Integer.parseInt(startElement.getAttributeByName(new QName("channel")).getValue());
                        if (tvChannelIdList.contains(channelId)) {
                            String start = startElement.getAttributeByName(new QName("start")).getValue();
                            String stop = startElement.getAttributeByName(new QName("stop")).getValue();

                            tvProgram = new TvProgram()
                                    .setChannel(new TvChannel().setId(channelId))
                                    .setStart(LocalDateTime.parse(start.substring(0, 14), formatter))
                                    .setStop(LocalDateTime.parse(stop.substring(0, 14), formatter));
                        }
                        break;
                    }
                    case "title": {
                        nextEvent = reader.nextEvent();
                        if (tvProgram != null) {
                            tvProgram.setTitle(nextEvent.asCharacters().getData());
                        }
                        break;
                    }
                    case "desc": {
                        nextEvent = reader.nextEvent();
                        if (tvProgram != null) {
                            tvProgram.setDesc(nextEvent.asCharacters().getData());
                        }
                        break;
                    }
                    case "category": {
                        nextEvent = reader.nextEvent();
                        if (tvProgram != null) {
                            tvProgram.setCategory(nextEvent.asCharacters().getData());
                        }
                        break;
                    }
                    default:
                        break;
                }
            }
            if (nextEvent.isEndElement()) {
                EndElement endElement = nextEvent.asEndElement();
                if (endElement.getName().getLocalPart().equals("channel")) {
                    tvChannelService.save(tvChannel);
                    tvChannel = null;
                } else if (endElement.getName().getLocalPart().equals("programme")) {
                    tvProgramService.save(tvProgram);
                    tvProgram = null;
                }
            }
        }

        reader.close();
    }
}
