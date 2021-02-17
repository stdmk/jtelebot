package org.telegram.bot.services.impl;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.TvChannel;
import org.telegram.bot.repositories.TvChannelRepository;
import org.telegram.bot.services.TvChannelService;

import java.util.List;

@Service
@AllArgsConstructor
public class TvChannelServiceImpl implements TvChannelService {
    private final Logger log = LoggerFactory.getLogger(TvChannelServiceImpl.class);

    private final TvChannelRepository tvChannelRepository;

    @Override
    public TvChannel get(Integer tvChannelId) {
        log.debug("Request to get TvChannel by Id: {}", tvChannelId);
        return tvChannelRepository.findById(tvChannelId).orElse(null);
    }

    @Override
    public List<TvChannel> get(String tvChannelName) {
        log.debug("Request to get TvChannel by it name: {}", tvChannelName);
        return tvChannelRepository.findByNameContainsIgnoreCase(tvChannelName);
    }

    @Override
    public Page<TvChannel> getAll(int page) {
        log.debug("Request to get all TvChannels. Page " + page);
        return tvChannelRepository.findAll(PageRequest.of(page, 10));
    }

    @Override
    public TvChannel save(TvChannel tvChannel) {
        log.debug("Request to save TvChannel: {}", tvChannel);
        return tvChannelRepository.save(tvChannel);
    }

    @Override
    public List<TvChannel> save(List<TvChannel> tvChannelList) {
        log.debug("Request to save TvChannelList: {}", tvChannelList);
        return tvChannelRepository.saveAll(tvChannelList);
    }

    @Override
    public void clearTable() {
        log.debug("Request to delete all TvChannels: ");
        tvChannelRepository.deleteAll();
    }
}
