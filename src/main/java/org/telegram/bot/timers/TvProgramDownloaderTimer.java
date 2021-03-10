package org.telegram.bot.timers;

import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.bot.domain.entities.Timer;
import org.telegram.bot.domain.entities.TvChannel;
import org.telegram.bot.domain.entities.TvProgram;
import org.telegram.bot.services.TimerService;
import org.telegram.bot.services.TvChannelService;
import org.telegram.bot.services.TvProgramService;
import org.telegram.bot.services.config.PropertiesConfig;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlValue;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.telegram.bot.utils.DateUtils.atStartOfDay;

@Component
@AllArgsConstructor
public class TvProgramDownloaderTimer extends TimerParent {

    private final Logger log = LoggerFactory.getLogger(TvProgramDownloaderTimer.class);

    private final TimerService timerService;
    private final TvChannelService tvChannelService;
    private final TvProgramService tvProgramService;

    private final String TV_PROGRAM_DATA_FILE_NAME = "tvguide.zip";

    @Override
    @Scheduled(fixedRate = 14400000)
    public void execute() {
        Timer timer = timerService.get("tvProgramDownloader");
        if (timer == null) {
            log.error("Unable to read timer tvProgramDownloader. Creating new...");
            timer = new Timer();
            timer.setName("tvProgramDownloader");
            timer.setLastAlarmDt(LocalDateTime.now().minusDays(1));
            timerService.save(timer);
        }

        LocalDateTime dateTimeNow = LocalDateTime.now();
        LocalDateTime nextAlarm = timer.getLastAlarmDt().plusDays(1);

        if (dateTimeNow.isAfter(nextAlarm)) {
            log.info("Timer for downloading and transfering tv-program");
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

            transferTvProgramDataToDb(tv);

            timer.setLastAlarmDt(atStartOfDay(dateTimeNow));
            timerService.save(timer);

            log.info("Timer for downloading and transfering tv-program completed successfully");
        }
    }

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

    private void transferTvProgramDataToDb(Tv tv) {
        tvChannelService.clearTable();
        tvChannelService.save(mapChannelToEntity(tv.getChannel()));

        tvProgramService.clearTable();
        tvProgramService.save(mapProgramToEntity(tv.getProgramme()));
    }

    private TvChannel mapChannelToEntity(Channel channel) {
        TvChannel entity = new TvChannel();
        entity.setId(channel.getId());
        entity.setName(channel.getDisplayName().getContent());

        return entity;
    }

    private TvProgram mapProgramToEntity(Program program) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyMMddHHmmss");

        TvChannel tvChannel = new TvChannel();
        tvChannel.setId(program.getChannel());

        TvProgram entity = new TvProgram();
        entity.setChannel(tvChannel);
        entity.setStart(LocalDateTime.parse(program.getStart().substring(0, 14), formatter));
        entity.setStop(LocalDateTime.parse(program.getStop().substring(0, 14), formatter));

        entity.setTitle(getContent(program.getTitle()));
        entity.setCategory(getContent(program.getCategory()));
        entity.setDesc(getContent(program.getDesc()));

        return entity;
    }

    private String getContent(Content content) {
        if (content == null) {
            return null;
        }

        return content.getContent();
    }

    private List<TvChannel> mapChannelToEntity(List<Channel> channels) {
        return channels.stream().map(this::mapChannelToEntity).collect(Collectors.toList());
    }

    private List<TvProgram> mapProgramToEntity(List<Program> programList) {
        return programList.stream().map(this::mapProgramToEntity).collect(Collectors.toList());
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
