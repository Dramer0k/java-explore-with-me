package ru.practicum.mainsrvc.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.mainsrvc.entity.Compilation;
import ru.practicum.mainsrvc.entity.Event;

import java.util.List;

public interface CompilationRepository extends JpaRepository<Compilation, Long> {

    @Query("SELECT c FROM Compilation c WHERE (:pinned IS NULL OR c.pinned = :pinned)")
    Page<Compilation> findAllOrByPinned(@Param("pinned") Boolean pinned, Pageable pageable);

    boolean existsByTitle(String title);

    @Query("SELECT e FROM Event e JOIN e.compilations c WHERE c.id IN (:compilationIds)")
    List<Event> findEventsByCompilationIds(@Param("compilationIds") List<Long> compilationIds);

}