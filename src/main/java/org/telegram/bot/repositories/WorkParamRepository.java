package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.telegram.bot.domain.entities.WorkParam;

import java.util.List;

/**
 * Spring Data repository for the Alias entity.
 */

@Repository
public interface WorkParamRepository extends JpaRepository<WorkParam, String> {
    List<WorkParam> findByBotToken(String token);
    List<WorkParam> findByBotTokenAndNameIn(String token, List<String> nameList);
}
