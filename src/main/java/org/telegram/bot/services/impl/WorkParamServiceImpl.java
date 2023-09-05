package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.WorkParam;
import org.telegram.bot.repositories.WorkParamRepository;
import org.telegram.bot.services.WorkParamService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkParamServiceImpl implements WorkParamService {

    private final WorkParamRepository workParamRepository;

    @Override
    public WorkParam save(WorkParam workParam) {
        log.debug("Request to save WorkParam: {} ", workParam);
        return workParamRepository.save(workParam);
    }

    @Override
    public void save(List<WorkParam> workParamList) {
        log.debug("Request to save WorkParams: {} ", workParamList);
        workParamRepository.saveAll(workParamList);
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
