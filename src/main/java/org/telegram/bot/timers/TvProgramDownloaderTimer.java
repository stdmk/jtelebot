package org.telegram.bot.timers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.io.FileUtils;
import org.json.XML;
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

@Component
@AllArgsConstructor
public class TvProgramDownloaderTimer extends TimerParent {

    private final Logger log = LoggerFactory.getLogger(TvProgramDownloaderTimer.class);

    private final TimerService timerService;
    private final PropertiesConfig propertiesConfig;
    private final TvChannelService tvChannelService;
    private final TvProgramService tvProgramService;

    private final String TV_PROGRAM_DATA_FILE_NAME = "tvguide.zip";

    @Override
    @Scheduled(fixedRate = 86400000)
    public void execute() {
        Timer timer = timerService.get("tvProgramDownloader");
        if (timer == null) {
            log.error("Unable to read timer tvProgramDownloader. Creating new...");
            timer = new Timer();
            timer.setName("tvProgramDownloader");
            timer.setLastAlarmDt(LocalDateTime.now());
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

            TvData tvData;
            try {
                tvData = getNewTvData();
            } catch (IOException e) {
                log.error("Unable to read new TvData: " + e.getMessage());
                return;
            }

            transferTvProgramDataToDb(tvData);

            timer.setLastAlarmDt(nextAlarm);
            timerService.save(timer);
        }
    }

    private TvData getNewTvData() throws IOException {
        ZipFile zipFile = new ZipFile(TV_PROGRAM_DATA_FILE_NAME);
        ZipEntry entry = zipFile.entries().nextElement();
        Reader reader = new InputStreamReader(zipFile.getInputStream(entry), StandardCharsets.UTF_8);

        ObjectMapper mapper = new ObjectMapper();

        return mapper.readValue(XML.toJSONObject(reader).toString(), TvData.class);
    }

    private void transferTvProgramDataToDb(TvData tvData) {
        tvChannelService.clearTable();
        tvChannelService.save(mapChannelToEntity(tvData.getTv().getChannel()));

        tvProgramService.clearTable();
        tvProgramService.save(mapProgramToEntity(tvData.getTv().getProgramme()));
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
    private static class TvData {
        private Tv tv;
    }

    @Data
    private static class Tv {
        @JsonIgnore
        @JsonProperty("generator-info-name")
        private Object generatorInfoName;

        @JsonIgnore
        @JsonProperty("generator-info-url")
        private Object generatorInfoUrl;

        private List<Channel> channel;

        private List<Program> programme;
    }

    @Data
    private static class Channel {
        @JsonProperty("display-name")
        private Content displayName;

        @JsonIgnore
        private Object icon;

        private Integer id;
    }

    @Data
    private static class Program {
        private String stop;
        private String start;
        private Integer channel;
        private Content title;
        private Content category;
        private Content desc;
    }

    @Data
    private static class Content {
        private String lang;
        private String content;
    }
}
