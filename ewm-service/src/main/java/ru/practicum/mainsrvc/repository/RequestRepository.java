package ru.practicum.mainsrvc.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.mainsrvc.entity.ParticipationRequest;
import ru.practicum.mainsrvc.entity.RequestStatus;

import java.util.List;

public interface RequestRepository extends JpaRepository<ParticipationRequest, Long> {

    List<ParticipationRequest> findAllByRequesterId(Long requesterId);

    @EntityGraph(attributePaths = {"event", "event.category", "event.initiator"})
    Page<ParticipationRequest> findAllByEventId(Long eventId, Pageable pageable);

    List<ParticipationRequest> findAllByEventId(Long eventId);

    @EntityGraph(attributePaths = {"event", "event.category", "event.initiator"})
    Page<ParticipationRequest> findAllByRequesterId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"event", "event.category", "event.initiator"})
    Page<ParticipationRequest> findAllByRequesterIdAndEventId(Long userId, Long eventId, Pageable pageable);

    @Query("SELECT COUNT(r) FROM ParticipationRequest r WHERE r.event.id = :eventId AND r.status = :status")
    long countByEventIdAndStatus(@Param("eventId") Long eventId, @Param("status") RequestStatus status);

    @Query("SELECT COUNT(r) FROM ParticipationRequest r WHERE r.event.id = :eventId AND r.status = 'CONFIRMED'")
    long countConfirmedByEventId(@Param("eventId") Long eventId);

    @Query("SELECT r FROM ParticipationRequest r WHERE r.event.id = :eventId AND r.status = :status")
    List<ParticipationRequest> findByEventIdAndStatus(
            @Param("eventId") Long eventId,
            @Param("status") RequestStatus status);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
            "FROM ParticipationRequest r " +
            "WHERE r.requester.id = :requesterId " +
            "AND r.event.id = :eventId " +
            "AND r.status <> :status")
    boolean existsByRequesterIdAndEventIdAndStatusNot(
            @Param("requesterId") Long requesterId,
            @Param("eventId") Long eventId,
            @Param("status") RequestStatus status);

    List<ParticipationRequest> findByRequesterId(Long requesterId);

    List<ParticipationRequest> findByEventId(Long eventId);
}