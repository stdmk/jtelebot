package org.telegram.bot.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.File;
import org.telegram.bot.domain.entities.User;

import java.util.List;

/**
 * Spring Data repository for the File entity.
 */

@Repository
public interface FileRepository extends JpaRepository<File, Long> {
    List<File> findByChatAndParentIdOrId(Chat chat, Long parentId, Long id);
    Page<File> findAllByChatAndUserAndParentId(Chat chat, User user, Long parentId, Pageable pageable);
}
