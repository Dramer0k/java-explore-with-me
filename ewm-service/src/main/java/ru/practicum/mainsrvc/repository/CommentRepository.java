package ru.practicum.mainsrvc.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.mainsrvc.entity.Comment;
import ru.practicum.mainsrvc.entity.CommentStatus;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByEventIdAndStatusAndDeletedFalse(
            Long eventId,
            CommentStatus status,
            Pageable pageable
    );

    Page<Comment> findByAuthorIdAndDeletedFalse(Long authorId, Pageable pageable);

    Page<Comment> findByStatusAndDeletedFalse(CommentStatus status, Pageable pageable);

    @Query("SELECT c FROM Comment c WHERE c.event.id = :eventId AND c.deleted = false ORDER BY c.created DESC")
    Page<Comment> findPublicByEventId(@Param("eventId") Long eventId, Pageable pageable);

    @Query("SELECT c FROM Comment c WHERE c.event.id = :eventId AND c.deleted = false")
    List<Comment> findAllByEventIdAndDeletedFalse(@Param("eventId") Long eventId);

    Long countByEventIdAndStatusAndDeletedFalse(Long eventId, CommentStatus status);
}