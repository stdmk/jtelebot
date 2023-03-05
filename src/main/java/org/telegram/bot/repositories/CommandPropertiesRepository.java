package org.telegram.bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.telegram.bot.domain.entities.CommandProperties;

import java.util.List;

/**
 * Spring Data repository for the CommandNames entity.
 */

@Repository
public interface CommandPropertiesRepository extends JpaRepository<CommandProperties, Long> {

    @Query(value = "SELECT cp FROM CommandProperties cp WHERE " +
            "cp.commandName = :nameOfCommand or " +
            "cp.russifiedName = :nameOfCommand or " +
            "cp.enRuName = :nameOfCommand")
    CommandProperties findByCommandNameOrRussifiedNameOrEnRuName(@Param("nameOfCommand") String nameOfCommand);

    CommandProperties findByClassName(String className);

    List<CommandProperties> findByAccessLevelLessThanEqual(Integer level);

    List<CommandProperties> findAllByDefaultDisabledForGroups(boolean disabled);
}
