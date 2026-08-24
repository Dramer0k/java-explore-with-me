package ru.practicum.mainsrvc.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.mainsrvc.dto.CompilationEventProjection;
import ru.practicum.mainsrvc.entity.Compilation;

import java.util.Collection;
import java.util.List;

public interface CompilationRepository extends JpaRepository<Compilation, Long> {

    @Query("SELECT c FROM Compilation c WHERE (:pinned IS NULL OR c.pinned = :pinned)")
    Page<Compilation> findAllOrByPinned(@Param("pinned") Boolean pinned, Pageable pageable);

    boolean existsByTitle(String title);

    @Query("SELECT c.id AS compilationId, e AS event " +
            "FROM Compilation c " +
            "JOIN c.events e " +
            "WHERE c.id IN :compilationIds")
    List<CompilationEventProjection> findEventsWithCompilationIds(
            @Param("compilationIds") Collection<Long> compilationIds);

}