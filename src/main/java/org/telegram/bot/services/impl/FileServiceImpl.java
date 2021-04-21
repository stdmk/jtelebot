package org.telegram.bot.services.impl;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.File;
import org.telegram.bot.domain.entities.User;
import org.telegram.bot.repositories.FileRepository;
import org.telegram.bot.services.FileService;

import java.util.List;

@Service
@AllArgsConstructor
public class FileServiceImpl implements FileService {

    private final Logger log = LoggerFactory.getLogger(FileService.class);

    private final FileRepository fileRepository;

    @Override
    public File get(Long id) {
        log.debug("Request to get File by id: {}", id);
        return fileRepository.findById(id).orElse(null);
    }

    @Override
    public Page<File> get(Chat chat, User user, File parent, int page) {
        log.debug("Request to get files by User: {} and Chat: {}, parent: {}, page: {}", user, chat, parent, page);
        return fileRepository.findAllByChatAndUserAndParentId(chat, user, parent.getId(), PageRequest.of(page, 10));
    }

    @Override
    public File save(File file) {
        log.debug("Request to save File: {}", file);
        return fileRepository.save(file);
    }

    @Override
    public void remove(Chat chat, File file) {
        log.debug("Request to delete File: {}", file);
        List<File> listFileToDelete = fileRepository.findByChatAndParentIdOrId(chat, file.getId(), file.getId());
        listFileToDelete.add(file);

        fileRepository.deleteAll(listFileToDelete);
    }
}
