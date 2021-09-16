package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Wiki;
import org.telegram.bot.repositories.WikiRepository;
import org.telegram.bot.services.WikiService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WikiServiceImpl implements WikiService {

    private final WikiRepository wikiRepository;

    @Override
    public Wiki get(Integer wikiPageId) {
        log.debug("Request to get Wiki by Id: {}", wikiPageId);
        return wikiRepository.findById(wikiPageId).orElse(null);
    }

    @Override
    public Wiki save(Wiki wiki) {
        log.debug("Request to save Wiki: {}", wiki);
        return wikiRepository.save(wiki);
    }

    @Override
    public List<Wiki> save(List<Wiki> wikiList) {
        log.debug("Request to save WikiList: {}", wikiList);
        return wikiRepository.saveAll(wikiList);
    }
}
