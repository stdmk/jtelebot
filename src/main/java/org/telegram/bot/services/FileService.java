package org.telegram.bot.services;

import org.springframework.data.domain.Page;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.File;
import org.telegram.bot.domain.entities.User;

/**
 * Service Interface for managing {@link org.telegram.bot.domain.entities.File}.
 */
public interface FileService {
    /**
     * Get a File.
     *
     * @param id of File to get.
     * @return the persisted entity.
     */
    File get(Long id);

    /**
     * Get a UserTvs.
     *
     * @param chat Chat entity of File to get.
     * @param user User entity of File to get.
     * @return the persisted entities.
     */
    Page<File> get(Chat chat, User user, File parent, int page);

    /**
     * Save a File.
     *
     * @param file the entity to save.
     * @return the persisted entity.
     */
    File save(File file);

    /**
     * Remove the File.
     *
     * @param chat Chat entity of File to remove.
     * @param file of persisted entity for delete
     */
    void remove(Chat chat, File file);
}
