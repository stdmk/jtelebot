package org.telegram.bot.services.impl;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.CommandProperties;
import org.telegram.bot.domain.enums.AccessLevel;
import org.telegram.bot.repositories.CommandPropertiesRepository;
import org.telegram.bot.services.CommandPropertiesService;

import java.util.List;

import static org.telegram.bot.utils.TextUtils.getPotentialCommandInText;
import static org.telegram.bot.utils.TextUtils.removeCapital;

@Service
@AllArgsConstructor
public class CommandPropertiesServiceImpl implements CommandPropertiesService {

    private final Logger log = LoggerFactory.getLogger(CommandPropertiesServiceImpl.class);

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

        return getCommand(getPotentialCommandInText(textOfMessage));
    }

    @Override
    public Integer getAccessLevelForCommand(String className) {
        log.debug("Request to get access level for command by its class name");
        CommandProperties commandProperties = commandPropertiesRepository.findByClassName(className);
        if (commandProperties == null || commandProperties.getAccessLevel() == null) {
            return AccessLevel.ADMIN.getValue();
        }

        return commandProperties.getAccessLevel();
    }

    @Override
    public List<CommandProperties> getAvailableCommandsForLevel(Integer accessLevel) {
        log.debug("Request to get available commands for level {}", accessLevel);
        return commandPropertiesRepository.findByAccessLevelLessThanEqual(accessLevel);
    }

    @Override
    public CommandProperties getCommand(String name) {
        log.debug("Request to get command propertiest by name {}", name);
        return commandPropertiesRepository.findByCommandNameOrRussifiedNameOrEnRuName(name);
    }

    @Override
    public String getCommand(Class<?> commandClass) {
        String className = removeCapital(commandClass.getSimpleName());
        log.debug("Request to get command propertiest by class name {}", className);
        return commandPropertiesRepository.findByClassName(className).getCommandName();
    }
}
