package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.repositories.CommandPropertiesRepository;
import org.telegram.bot.services.CommandPropertiesService;

import java.util.List;
import java.util.Locale;

import static org.telegram.bot.utils.TextUtils.getPotentialCommandInText;
import static org.telegram.bot.utils.TextUtils.removeCapital;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommandPropertiesServiceImpl implements CommandPropertiesService {

    private final CommandPropertiesRepository commandPropertiesRepository;

    @Override
    public CommandProperties findCommandInText(String textOfMessage, String botUsername) {
        log.debug("Request to find commands in text {}", textOfMessage);

        int i = textOfMessage.indexOf("@");
        if (i > 0 && textOfMessage.indexOf(botUsername) > 0) {
            if (!textOfMessage.substring(i + 1).equals(botUsername)) {
                return null;
            } else {
                textOfMessage = textOfMessage.replace("@" + botUsername, "");
            }
        }

        String potentialCommand = getPotentialCommandInText(textOfMessage);
        if (potentialCommand != null) {
            return getCommand(potentialCommand);
        }

        return null;
    }

    @Override
    public List<CommandProperties> getAvailableCommandsForLevel(Integer accessLevel) {
        log.debug("Request to get available commands for level {}", accessLevel);
        return commandPropertiesRepository.findByAccessLevelLessThanEqual(accessLevel);
    }

    @Override
    public CommandProperties getCommand(String name) {
        log.debug("Request to get command property by name {}", name);
        return commandPropertiesRepository.findByCommandNameOrRussifiedNameOrEnRuName(name.toLowerCase(Locale.ROOT));
    }

    @Override
    public CommandProperties getCommand(Long id) {
        log.debug("Request to get command property by id {}", id);
        return commandPropertiesRepository.findById(id).orElse(null);
    }

    @Override
    public CommandProperties getCommand(Class<?> commandClass) {
        String className = removeCapital(commandClass.getSimpleName());
        log.debug("Request to get command propertiest by class name {}", className);
        return commandPropertiesRepository.findByClassName(className);
    }

    public Page<CommandProperties> getAll(int page) {
        log.debug("Request to get all CommandProperties. Page " + page);
        return commandPropertiesRepository.findAll(PageRequest.of(page, 10));
    }

    @Override
    public List<CommandProperties> getAllDisabledByDefaultForGroups() {
        log.debug("Request to get all disabled by default CommandProperties entities");
        return commandPropertiesRepository.findAllByDefaultDisabledForGroups(true);
    }
}
