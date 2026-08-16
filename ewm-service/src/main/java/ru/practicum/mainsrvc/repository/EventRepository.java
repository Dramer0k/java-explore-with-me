package ru.practicum.mainsrvc.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.mainsrvc.entity.Event;
import ru.practicum.mainsrvc.entity.EventStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    @Query("SELECT e FROM Event e " +
            "JOIN FETCH e.category " +
            "JOIN FETCH e.initiator " +
            "WHERE e.id = :id AND e.state = :state")
    Optional<Event> findByIdAndState(@Param("id") Long id, @Param("state") EventStatus state);

    @Query("SELECT e FROM Event e " +
            "JOIN FETCH e.category " +
            "JOIN FETCH e.initiator " +
            "WHERE e.id = :eventId AND e.initiator.id = :initiatorId")
    Optional<Event> findByIdAndInitiator(@Param("eventId") Long eventId, @Param("initiatorId") Long initiatorId);

    @Query("SELECT e FROM Event e " +
            "JOIN FETCH e.category " +
            "JOIN FETCH e.initiator " +
            "WHERE e.initiator.id = :initiatorId")
    Page<Event> findAllByInitiatorId(@Param("initiatorId") Long initiatorId, Pageable pageable);

    @Query("SELECT e FROM Event e " +
            "JOIN FETCH e.category " +
            "JOIN FETCH e.initiator " +
            "WHERE e.state IN :states " +
            "AND e.eventDate >= :rangeStart " +
            "AND e.eventDate <= :rangeEnd " +
            "AND e.initiator.id IN :users " +
            "AND e.category.id IN :categories")
    Page<Event> findAdminAll(
            @Param("states") List<String> states,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            @Param("users") List<Long> users,
            @Param("categories") List<Long> categories,
            Pageable pageable);

    @Query("SELECT e FROM Event e " +
            "JOIN FETCH e.category " +
            "JOIN FETCH e.initiator " +
            "WHERE e.state IN :states " +
            "AND e.eventDate >= :rangeStart " +
            "AND e.eventDate <= :rangeEnd " +
            "AND e.initiator.id IN :users")
    Page<Event> findAdminWithUsers(
            @Param("states") List<String> states,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            @Param("users") List<Long> users,
            Pageable pageable);

    @Query("SELECT e FROM Event e " +
            "JOIN FETCH e.category " +
            "JOIN FETCH e.initiator " +
            "WHERE e.state IN :states " +
            "AND e.eventDate >= :rangeStart " +
            "AND e.eventDate <= :rangeEnd " +
            "AND e.category.id IN :categories")
    Page<Event> findAdminWithCategories(
            @Param("states") List<String> states,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            @Param("categories") List<Long> categories,
            Pageable pageable);

    @Query("SELECT e FROM Event e " +
            "JOIN FETCH e.category " +
            "JOIN FETCH e.initiator " +
            "WHERE (:states IS NULL OR e.state IN :states) " +
            "AND e.eventDate >= :rangeStart " +
            "AND e.eventDate <= :rangeEnd")
    Page<Event> findAdminBasic(
            @Param("states") List<String> states,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            Pageable pageable);

    @Query("SELECT e FROM Event e " +
            "JOIN FETCH e.category " +
            "JOIN FETCH e.initiator " +
            "WHERE e.state = 'PUBLISHED' " +
            "AND e.paid = :paid " +
            "AND (LOWER(e.annotation) LIKE LOWER(CONCAT('%', :text, '%')) OR " +
            "LOWER(e.description) LIKE LOWER(CONCAT('%', :text, '%'))) " +
            "AND e.eventDate >= :rangeStart " +
            "AND e.eventDate <= :rangeEnd")
    Page<Event> findPublicWithoutCategoriesAll(
            @Param("paid") Boolean paid,
            @Param("text") String text,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            Pageable pageable);

    @Query("SELECT e FROM Event e " +
            "JOIN FETCH e.category " +
            "JOIN FETCH e.initiator " +
            "WHERE e.state = 'PUBLISHED' " +
            "AND e.paid = :paid " +
            "AND e.eventDate >= :rangeStart " +
            "AND e.eventDate <= :rangeEnd")
    Page<Event> findPublicWithoutCategoriesPaidOnly(
            @Param("paid") Boolean paid,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            Pageable pageable);

    @Query("SELECT e FROM Event e " +
            "JOIN FETCH e.category " +
            "JOIN FETCH e.initiator " +
            "WHERE e.state = 'PUBLISHED' " +
            "AND (LOWER(e.annotation) LIKE LOWER(CONCAT('%', :text, '%')) OR " +
            "LOWER(e.description) LIKE LOWER(CONCAT('%', :text, '%'))) " +
            "AND e.eventDate >= :rangeStart " +
            "AND e.eventDate <= :rangeEnd")
    Page<Event> findPublicWithoutCategoriesTextOnly(
            @Param("text") String text,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            Pageable pageable);

    @Query("SELECT e FROM Event e " +
            "JOIN FETCH e.category " +
            "JOIN FETCH e.initiator " +
            "WHERE e.state = 'PUBLISHED' " +
            "AND e.eventDate >= :rangeStart " +
            "AND e.eventDate <= :rangeEnd")
    Page<Event> findPublicWithoutCategoriesBasic(
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            Pageable pageable);

    @Query("SELECT e FROM Event e " +
            "JOIN FETCH e.category " +
            "JOIN FETCH e.initiator " +
            "WHERE e.state = 'PUBLISHED' " +
            "AND e.category.id IN :categories " +
            "AND e.paid = :paid " +
            "AND (LOWER(e.annotation) LIKE LOWER(CONCAT('%', :text, '%')) OR " +
            "LOWER(e.description) LIKE LOWER(CONCAT('%', :text, '%'))) " +
            "AND e.eventDate >= :rangeStart " +
            "AND e.eventDate <= :rangeEnd")
    Page<Event> findPublicWithCategoriesAll(
            @Param("categories") List<Long> categories,
            @Param("paid") Boolean paid,
            @Param("text") String text,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            Pageable pageable);

    @Query("SELECT e FROM Event e " +
            "JOIN FETCH e.category " +
            "JOIN FETCH e.initiator " +
            "WHERE e.state = 'PUBLISHED' " +
            "AND e.category.id IN :categories " +
            "AND e.paid = :paid " +
            "AND e.eventDate >= :rangeStart " +
            "AND e.eventDate <= :rangeEnd")
    Page<Event> findPublicWithCategoriesPaidOnly(
            @Param("categories") List<Long> categories,
            @Param("paid") Boolean paid,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            Pageable pageable);

    @Query("SELECT e FROM Event e " +
            "JOIN FETCH e.category " +
            "JOIN FETCH e.initiator " +
            "WHERE e.state = 'PUBLISHED' " +
            "AND e.category.id IN :categories " +
            "AND (LOWER(e.annotation) LIKE LOWER(CONCAT('%', :text, '%')) OR " +
            "LOWER(e.description) LIKE LOWER(CONCAT('%', :text, '%'))) " +
            "AND e.eventDate >= :rangeStart " +
            "AND e.eventDate <= :rangeEnd")
    Page<Event> findPublicWithCategoriesTextOnly(
            @Param("categories") List<Long> categories,
            @Param("text") String text,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            Pageable pageable);

    @Query("SELECT e FROM Event e " +
            "JOIN FETCH e.category " +
            "JOIN FETCH e.initiator " +
            "WHERE e.state = 'PUBLISHED' " +
            "AND e.category.id IN :categories " +
            "AND e.eventDate >= :rangeStart " +
            "AND e.eventDate <= :rangeEnd")
    Page<Event> findPublicWithCategoriesBasic(
            @Param("categories") List<Long> categories,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.id = :eventId")
    Optional<Event> findById(@Param("eventId") Long eventId);

    @Query("SELECT e FROM Event e " +
            "JOIN FETCH e.category " +
            "JOIN FETCH e.initiator " +
            "WHERE e.id = :eventId")
    Optional<Event> findByIdWithDetails(@Param("eventId") Long eventId);
    }