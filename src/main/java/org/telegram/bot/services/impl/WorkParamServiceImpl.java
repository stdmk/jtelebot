package org.telegram.bot.services.impl;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.WorkParam;
import org.telegram.bot.repositories.WorkParamRepository;
import org.telegram.bot.services.WorkParamService;

import java.util.List;

@Service
@AllArgsConstructor
public class WorkParamServiceImpl implements WorkParamService {

    private final Logger log = LoggerFactory.getLogger(WorkParamServiceImpl.class);

    private final WorkParamRepository workParamRepository;

    @Override
    public WorkParam save(WorkParam workParam) {
        log.debug("Request to save WorkParam: {} ", workParam);
        return workParamRepository.save(workParam);
    }

    @Override
    public List<WorkParam> save(List<WorkParam> workParamList) {
        log.debug("Request to save WorkParams: {} ", workParamList);
        return workParamRepository.saveAll(workParamList);
    }

    @Override
    public List<WorkParam> get(String token) {
        log.debug("Request to get Token by name: {} ", token);
        return workParamRepository.findByBotToken(token);
    }

    @Override
    public List<WorkParam> get(String token, List<String> nameList) {
        log.debug("Request to get Token by name: {} and names {}", token, nameList);
        return workParamRepository.findByBotTokenAndNameIn(token, nameList);
    }
}
