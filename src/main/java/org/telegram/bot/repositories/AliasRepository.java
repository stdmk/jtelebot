package org.telegram.bot.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.telegram.bot.domain.entities.Alias;
import org.telegram.bot.domain.entities.Chat;
import org.telegram.bot.domain.entities.User;

import java.util.List;

/**
 * Spring Data repository for the Alias entity.
 */

@Repository
public interface AliasRepository extends JpaRepository<Alias, Long> {
    List<Alias> findByChatAndUser(Chat chat, User user);
    Page<Alias> findAllByChatAndUser(Chat chat, User user, Pageable pageable);
    Alias findByChatAndUserAndNameIgnoreCase(Chat chat, User user, String name);
    Alias findByChatAndUserAndId(Chat chat, User user, Long aliasId);
    Page<Alias> findAllByChat(Chat chat, Pageable pageable);
}
