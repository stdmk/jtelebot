//package org.telegram.bot.timers;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.io.FileUtils;
//import org.apache.commons.io.IOUtils;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//import org.telegram.bot.domain.BotStats;
//import org.telegram.bot.domain.entities.Timer;
//import org.telegram.bot.domain.entities.TvChannel;
//import org.telegram.bot.domain.entities.TvProgram;
//import org.telegram.bot.services.TimerService;
//import org.telegram.bot.services.TvChannelService;
//import org.telegram.bot.services.TvProgramService;
//
//import javax.xml.namespace.QName;
//import javax.xml.stream.XMLEventReader;
//import javax.xml.stream.XMLInputFactory;
//import javax.xml.stream.XMLStreamException;
//import javax.xml.stream.events.EndElement;
//import javax.xml.stream.events.StartElement;
//import javax.xml.stream.events.XMLEvent;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.net.URL;
//import java.time.Instant;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.zip.ZipEntry;
//import java.util.zip.ZipFile;
//
//import static org.telegram.bot.utils.DateUtils.atStartOfDay;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class TvProgramDownloaderTimer extends TimerParent {
//
//    private final TimerService timerService;
//    private final TvChannelService tvChannelService;
//    private final TvProgramService tvProgramService;
//    private final BotStats botStats;
//
//    private final String TV_PROGRAM_DATA_XML_FILE_NAME = "tvguide.xml";
//
//    @Override
//    @Scheduled(fixedRate = 14400000)
//    public void execute() {
//        Timer timer = timerService.get("tvProgramDownloader");
//        if (timer == null) {
//            log.error("Unable to read timer tvProgramDownloader. Creating new...");
//            timer = new Timer()
//                    .setName("tvProgramDownloader")
//                    .setLastAlarmDt(LocalDateTime.now().minusDays(1));
//            timerService.save(timer);
//        }
//
//        LocalDateTime dateTimeNow = LocalDateTime.now();
//        LocalDateTime nextAlarm = timer.getLastAlarmDt().plusDays(1);
//
//        if (dateTimeNow.isAfter(nextAlarm)) {
//            log.info("Timer for downloading and transferring tv-program");
//
//
//            try {
//                transferTvProgramDataFile();
//            } catch (IOException e) {
//                log.error("Failed to download tv program data: {}", e.getMessage());
//                return;
//            }
//
//            clearTvTables();
//
//            try {
//                parseTvProgramData();
//            } catch (IOException | XMLStreamException e) {
//                log.error("Unable to read new TvData: " + e.getMessage());
//                return;
//            }
//
//            botStats.setLastTvUpdate(Instant.now());
//
//            timer.setLastAlarmDt(atStartOfDay(dateTimeNow));
//            timerService.save(timer);
//
//            log.info("Timer for downloading and transferring tv-program completed successfully");
//        }
//    }
//
//    /**
//     * Downloading and unzip tv program data file.
//     *
//     * @throws IOException if failed to transfer file.
//     */
//    private void transferTvProgramDataFile() throws IOException {
//        String zipFileName = "tvguide.zip";
//        final String tvProgramDataUrl = "https://www.teleguide.info/download/new3/" + zipFileName;
//
//        FileUtils.copyURLToFile(new URL(tvProgramDataUrl), new File(zipFileName));
//
//        ZipFile zipFile = new ZipFile(zipFileName);
//        ZipEntry entry = zipFile.entries().nextElement();
//
//        File entryDestination = new File(TV_PROGRAM_DATA_XML_FILE_NAME);
//        InputStream in = zipFile.getInputStream(entry);
//        OutputStream out = new FileOutputStream(entryDestination);
//        IOUtils.copy(in, out);
//
//        in.close();
//        out.close();
//        zipFile.close();
//    }
//
//    void clearTvTables() {
//        tvProgramService.clearTable();
//        tvChannelService.clearTable();
//    }
//
//    /**
//     * Parsing tv data from file.
//     *
//     * @throws IOException if failed to read.
//     * @throws XMLStreamException if failed to parse xml.
//     */
//    private void parseTvProgramData() throws IOException, XMLStreamException {
//        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
//        XMLEventReader reader = xmlInputFactory.createXMLEventReader(new FileInputStream(TV_PROGRAM_DATA_XML_FILE_NAME));
//
//        TvChannel tvChannel = null;
//        List<Integer> tvChannelIdList = new ArrayList<>();
//        TvProgram tvProgram = null;
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyMMddHHmmss");
//
//        while (reader.hasNext()) {
//            XMLEvent nextEvent = reader.nextEvent();
//
//            if (nextEvent.isStartElement()) {
//                StartElement startElement = nextEvent.asStartElement();
//                switch (startElement.getName().getLocalPart()) {
//                    case "channel": {
//                        Integer channelId = Integer.parseInt(startElement.getAttributeByName(new QName("id")).getValue());
//                        tvChannelIdList.add(channelId);
//                        tvChannel = new TvChannel().setId(channelId);
//                        break;
//                    }
//                    case "display-name": {
//                        nextEvent = reader.nextEvent();
//                        if (tvChannel != null) {
//                            tvChannel.setName(nextEvent.asCharacters().getData());
//                        }
//                        break;
//                    }
//                    case "programme": {
//                        Integer channelId = Integer.parseInt(startElement.getAttributeByName(new QName("channel")).getValue());
//                        if (tvChannelIdList.contains(channelId)) {
//                            String start = startElement.getAttributeByName(new QName("start")).getValue();
//                            String stop = startElement.getAttributeByName(new QName("stop")).getValue();
//
//                            tvProgram = new TvProgram()
//                                    .setChannel(new TvChannel().setId(channelId))
//                                    .setStart(LocalDateTime.parse(start.substring(0, 14), formatter))
//                                    .setStop(LocalDateTime.parse(stop.substring(0, 14), formatter));
//                        }
//                        break;
//                    }
//                    case "title": {
//                        nextEvent = reader.nextEvent();
//                        if (tvProgram != null) {
//                            tvProgram.setTitle(nextEvent.asCharacters().getData());
//                        }
//                        break;
//                    }
//                    case "desc": {
//                        nextEvent = reader.nextEvent();
//                        if (tvProgram != null) {
//                            tvProgram.setDesc(nextEvent.asCharacters().getData());
//                        }
//                        break;
//                    }
//                    case "category": {
//                        nextEvent = reader.nextEvent();
//                        if (tvProgram != null) {
//                            tvProgram.setCategory(nextEvent.asCharacters().getData());
//                        }
//                        break;
//                    }
//                }
//            }
//            if (nextEvent.isEndElement()) {
//                EndElement endElement = nextEvent.asEndElement();
//                if (endElement.getName().getLocalPart().equals("channel")) {
//                    tvChannelService.save(tvChannel);
//                    tvChannel = null;
//                } else if (endElement.getName().getLocalPart().equals("programme")) {
//                    tvProgramService.save(tvProgram);
//                    tvProgram = null;
//                }
//            }
//        }
//
//        reader.close();
//    }
//}
