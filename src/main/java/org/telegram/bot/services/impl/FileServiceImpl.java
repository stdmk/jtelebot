package org.telegram.bot.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.File;
import org.telegram.bot.repositories.FileRepository;
import org.telegram.bot.services.FileService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileServiceImpl implements FileService {

    private final FileRepository fileRepository;

    @Override
    public File get(Long id) {
        log.debug("Request to get File by id: {}", id);
        return fileRepository.findById(id).orElse(null);
    }

    @Override
    public Page<File> get(Chat chat, File parent, int page) {
        log.debug("Request to get files by Chat: {}, parent: {}, page: {}", chat, parent, page);
        return fileRepository.findAllByChatAndParentId(chat, parent.getId(), PageRequest.of(page, 10));
    }

    @Override
    public void save(File file) {
        log.debug("Request to save File: {}", file);
        fileRepository.save(file);
    }

    @Override
    public void remove(Chat chat, File file) {
        log.debug("Request to delete File: {}", file);

        List<File> listFileToDelete = fileRepository.findByChatAndParentIdOrId(chat, file.getId(), file.getId());
        listFileToDelete.add(file);

        fileRepository.deleteAll(listFileToDelete);
    }
}
