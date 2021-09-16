package org.telegram.bot.timers;

import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.BotStats;
import org.telegram.bot.domain.entities.Timer;
import org.telegram.bot.domain.entities.TvChannel;
import org.telegram.bot.domain.entities.TvProgram;
import org.telegram.bot.services.TimerService;
import org.telegram.bot.services.TvChannelService;
import org.telegram.bot.services.TvProgramService;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlValue;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.telegram.bot.utils.DateUtils.atStartOfDay;

@Component
@RequiredArgsConstructor
@Slf4j
public class TvProgramDownloaderTimer extends TimerParent {

    private final TimerService timerService;
    private final TvChannelService tvChannelService;
    private final TvProgramService tvProgramService;
    private final BotStats botStats;

    private final String TV_PROGRAM_DATA_FILE_NAME = "tvguide.zip";

    @Override
    @Scheduled(fixedRate = 14400000)
    public void execute() {
        Timer timer = timerService.get("tvProgramDownloader");
        if (timer == null) {
            log.error("Unable to read timer tvProgramDownloader. Creating new...");
            timer = new Timer()
                    .setName("tvProgramDownloader")
                    .setLastAlarmDt(LocalDateTime.now().minusDays(1));
            timerService.save(timer);
        }

        LocalDateTime dateTimeNow = LocalDateTime.now();
        LocalDateTime nextAlarm = timer.getLastAlarmDt().plusDays(1);

        if (dateTimeNow.isAfter(nextAlarm)) {
            log.info("Timer for downloading and transferring tv-program");
            final String TV_PROGRAM_DATA_URL ="https://www.teleguide.info/download/new3/tvguide.zip";

            try {
                FileUtils.copyURLToFile(new URL(TV_PROGRAM_DATA_URL), new File(TV_PROGRAM_DATA_FILE_NAME));
            } catch (IOException e) {
                log.error("Failed to download tv program data: {}", e.getMessage());
                return;
            }

            Tv tv;
            try {
                tv = getNewTvData();
            } catch (IOException e) {
                log.error("Unable to read new TvData: " + e.getMessage());
                return;
            }

            botStats.setLastTvUpdate(Instant.now());
            transferTvProgramDataToDb(tv);

            timer.setLastAlarmDt(atStartOfDay(dateTimeNow));
            timerService.save(timer);

            log.info("Timer for downloading and transferring tv-program completed successfully");
        }
    }

    /**
     * Parsing tv data from file.
     *
     * @return tv data
     * @throws IOException if failed to read.
     */
    private Tv getNewTvData() throws IOException {
        ZipFile zipFile = new ZipFile(TV_PROGRAM_DATA_FILE_NAME);
        ZipEntry entry = zipFile.entries().nextElement();

        Reader reader = new InputStreamReader(zipFile.getInputStream(entry), StandardCharsets.UTF_8);

        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.setAnnotationIntrospector(new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()));
        Tv tv = xmlMapper.readValue(reader, Tv.class);

        reader.close();
        zipFile.close();

        return tv;
    }

    /**
     * Saving tv data to database.
     *
     * @param tv tv data.
     */
    private void transferTvProgramDataToDb(Tv tv) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyMMddHHmmss");

        tvChannelService.clearTable();

        List<Integer> tvChannelIdList = tv.getChannel()
                .stream()
                .map(channel -> new TvChannel()
                        .setId(channel.getId())
                        .setName(channel.getDisplayName().getContent()))
                .map(tvChannelService::save)
                .map(TvChannel::getId)
                .collect(Collectors.toList());

        tvProgramService.clearTable();

        tvProgramService.save(tv.getProgramme()
                .stream()
                .filter(tvProgram -> tvChannelIdList.contains(tvProgram.getChannel()))
                .map(program -> new TvProgram()
                        .setChannel(new TvChannel().setId(program.getChannel()))
                        .setStart(LocalDateTime.parse(program.getStart().substring(0, 14), formatter))
                        .setStop(LocalDateTime.parse(program.getStop().substring(0, 14), formatter))
                        .setTitle(getContent(program.getTitle()))
                        .setCategory(getContent(program.getCategory()))
                        .setDesc(getContent(program.getDesc())))
                .collect(Collectors.toList()));
    }

    private String getContent(Content content) {
        if (content == null) {
            return null;
        }

        return content.getContent();
    }

    @Data
    private static class Tv {
        @XmlAttribute(name = "generator-info-name")
        private String generatorInfoName;

        @XmlAttribute(name = "generator-info-url")
        private String generatorInfoUrl;

        private List<Channel> channel;

        private List<Program> programme;
    }

    @Data
    private static class Channel {
        private Integer id;

        @XmlElement(name = "display-name")
        private Content displayName;

        @XmlElement
        private Icon icon;
    }

    @Data
    private static class Program {
        private String stop;

        private String start;

        private Integer channel;

        @XmlElement
        private Content title;

        @XmlElement
        private Content category;

        @XmlElement
        private Content desc;
    }

    @Data
    private static class Content {
        private String lang;

        @XmlValue
        private String content;
    }

    @Data
    private static class Icon {
        private String src;
    }
}
