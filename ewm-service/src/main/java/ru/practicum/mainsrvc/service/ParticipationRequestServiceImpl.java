package ru.practicum.mainsrvc.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.mainsrvc.Mapper.ParticipationRequestDtoMapper;
import ru.practicum.mainsrvc.dto.ParticipationRequestDto;
import ru.practicum.mainsrvc.dto.ParticipationRequestStatusDto;
import ru.practicum.mainsrvc.dto.RequestStatusUpdateResult;
import ru.practicum.mainsrvc.entity.*;
import ru.practicum.mainsrvc.exception.ConflictException;
import ru.practicum.mainsrvc.exception.NotFoundException;
import ru.practicum.mainsrvc.repository.EventRepository;
import ru.practicum.mainsrvc.repository.RequestRepository;
import ru.practicum.mainsrvc.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class ParticipationRequestServiceImpl {

    private final RequestRepository requestRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ParticipationRequestDtoMapper participationRequestDtoMapper;

    public ParticipationRequestServiceImpl(RequestRepository requestRepository,
                                           EventRepository eventRepository,
                                           UserRepository userRepository, ParticipationRequestDtoMapper participationRequestDtoMapper) {
        this.requestRepository = requestRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.participationRequestDtoMapper = participationRequestDtoMapper;
    }

    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        log.debug("Creating request: userId={}, eventId={}", userId, eventId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        if (event.getInitiator() != null && event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Инициатор события не может подать запрос на участие в собственном событии");
        }

        if (!event.getState().equals(EventStatus.PUBLISHED)) {
            throw new ConflictException("Нельзя участвовать в событии, которое не опубликовано");
        }

        boolean hasActiveRequest = requestRepository.existsByRequesterIdAndEventId(
                userId, eventId);
        if (hasActiveRequest) {
            throw new ConflictException("Запрос на участие уже существует");
        }

        long confirmedCount = requestRepository.countConfirmedByEventId(eventId);
        Integer participantLimit = event.getParticipantLimit();

        if (participantLimit != null && participantLimit > 0 && confirmedCount >= participantLimit) {
            throw new ConflictException("Достигнут лимит участников для этого события");
        }

        ParticipationRequest request = new ParticipationRequest();
        request.setCreated(LocalDateTime.now());
        request.setEvent(event);
        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        request.setRequester(requester);
        request.setComment(null);

        if (participantLimit != null && participantLimit == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
        } else if (!event.isRequestModeration()) {
            request.setStatus(RequestStatus.CONFIRMED);
        } else {
            request.setStatus(RequestStatus.PENDING);
        }

        request = requestRepository.save(request);
        log.info("Created request: id={}, userId={}, eventId={}, status={}",
                request.getId(), userId, eventId, request.getStatus());

        return participationRequestDtoMapper.toDto(request);
    }

    public Page<ParticipationRequestDto> getRequestsByUser(Long userId, int from, int size) {
        Pageable pageable = PageRequest.of(from, size);
        Page<ParticipationRequest> requests = requestRepository.findAllByRequesterId(userId, pageable);
        return requests.map(participationRequestDtoMapper::toDto);
    }

    public List<ParticipationRequestDto> getRequestsForEvent(Long eventId) {
        var requests = requestRepository.findAllByEventId(eventId);
        return requests.stream().map(participationRequestDtoMapper::toDto).collect(Collectors.toList());
    }

    public ParticipationRequestDto approveRequest(Long requestId, Long initiatorId) {
        return approveOrReject(requestId, initiatorId, RequestStatus.CONFIRMED);
    }

    public ParticipationRequestDto rejectRequest(Long requestId, Long initiatorId) {
        return approveOrReject(requestId, initiatorId, RequestStatus.REJECTED);
    }

    public ParticipationRequestDto approveOrReject(Long requestId, Long initiatorId, RequestStatus status) {
        if (requestId == null) {
            throw new IllegalArgumentException("requestId не может быть null");
        }
        if (initiatorId == null) {
            throw new IllegalArgumentException("initiatorId не может быть null");
        }
        if (status == null) {
            throw new IllegalArgumentException("status не может быть null");
        }

        ParticipationRequest req = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос не найден"));

        Event event = req.getEvent();
        Long eventInitiatorId = event.getInitiator() != null ? event.getInitiator().getId() : null;

        if (!Objects.equals(eventInitiatorId, initiatorId)) {
            throw new IllegalStateException("Только инициатор события может изменить статус запроса");
        }

        if (req.getStatus() != RequestStatus.PENDING) {
            throw new ConflictException("Можно обрабатывать только заявки в статусе PENDING");
        }

        if (status == RequestStatus.CONFIRMED) {
            long confirmedCount = requestRepository.countConfirmedByEventId(event.getId());
            Integer participantLimit = event.getParticipantLimit();
            if (participantLimit != null && participantLimit > 0 && confirmedCount >= participantLimit) {
                throw new ConflictException("Достигнут лимит участников для этого события");
            }
        }

        req.setStatus(status);
        req = requestRepository.save(req);
        log.info("Request {} status changed to {} by initiator {}", requestId, status, initiatorId);

        return participationRequestDtoMapper.toDto(req);
    }

    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        ParticipationRequest req = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Заявка не найдена"));

        if (req.getRequester() == null || !req.getRequester().getId().equals(userId)) {
            throw new IllegalStateException("Пользователь может отменять только свои заявки");
        }

        if (req.getStatus() == RequestStatus.CONFIRMED) {
            throw new IllegalStateException("Нельзя отменить подтверждённую заявку");
        }

        req.setStatus(RequestStatus.CANCELED);
        req = requestRepository.save(req);
        log.info("Запрос {} отменен (отклонен) пользователем {}", requestId, userId);

        return participationRequestDtoMapper.toDto(req);
    }

    public Page<ParticipationRequestDto> getRequestsByUserAndEvent(Long userId, Long eventId, int from, int size) {
        Pageable pageable = PageRequest.of(from, size);
        Page<ParticipationRequest> requests = requestRepository.findAllByRequesterIdAndEventId(userId, eventId, pageable);
        return requests.map(participationRequestDtoMapper::toDto);
    }

    public Page<ParticipationRequestDto> getRequestsByEvent(Long eventId, int from, int size) {
        Pageable pageable = PageRequest.of(from, size);
        Page<ParticipationRequest> requests = requestRepository.findAllByEventId(eventId, pageable);
        return requests.map(participationRequestDtoMapper::toDto);
    }

    @Transactional
    public RequestStatusUpdateResult processRequestStatus(
            Long userId, Long eventId, ParticipationRequestStatusDto dto) {

        log.debug("Processing request status: userId={}, eventId={}, dto={}", userId, eventId, dto);

        if (dto == null) {
            throw new IllegalArgumentException("requestIds не может быть null");
        }
        if (dto.getRequestIds() == null || dto.getRequestIds().isEmpty()) {
            throw new IllegalArgumentException("requestIds не может быть пустым");
        }
        if (dto.getStatus() == null) {
            throw new IllegalArgumentException("status не может быть null");
        }

        RequestStatus status;
        try {
            status = RequestStatus.valueOf(dto.getStatus());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Неизвестный статус: " + dto.getStatus() +
                    ". Допустимые значения: CONFIRMED, REJECTED");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Пользователь не является инициатором события");
        }

        if (event.getState() != EventStatus.PUBLISHED) {
            throw new ConflictException("Событие не опубликовано");
        }

        List<ParticipationRequest> requests = requestRepository.findAllById(dto.getRequestIds());

        if (requests.size() != dto.getRequestIds().size()) {
            List<Long> foundIds = requests.stream()
                    .map(ParticipationRequest::getId)
                    .collect(Collectors.toList());
            List<Long> notFound = dto.getRequestIds().stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toList());
            throw new NotFoundException("Заявки не найдены: " + notFound);
        }

        for (ParticipationRequest request : requests) {
            if (!request.getEvent().getId().equals(eventId)) {
                throw new ConflictException("Заявка не принадлежит этому событию");
            }
        }

        List<ParticipationRequest> confirmed = new ArrayList<>();
        List<ParticipationRequest> rejected = new ArrayList<>();

        if (status == RequestStatus.CONFIRMED) {
            Integer participantLimit = event.getParticipantLimit();
            long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);

            if (participantLimit != null && participantLimit > 0 && confirmedCount >= participantLimit) {
                throw new ConflictException("Достигнут лимит участников для события");
            }

            for (ParticipationRequest request : requests) {
                if (request.getStatus() != RequestStatus.PENDING) {
                    throw new ConflictException(
                            "Заявка " + request.getId() + " не в статусе PENDING. Текущий статус: " + request.getStatus()
                    );
                }
                request.setStatus(RequestStatus.CONFIRMED);
                confirmed.add(request);
                confirmedCount++;

                if (participantLimit != null && participantLimit > 0 && confirmedCount >= participantLimit) {
                    rejectAllPendingRequests(eventId, rejected);
                    log.info("Лимит участников достигнут, все оставшиеся заявки отклонены");
                    break;
                }
            }

            requestRepository.saveAll(confirmed);
            if (!rejected.isEmpty()) {
                requestRepository.saveAll(rejected);
            }

        } else if (status == RequestStatus.REJECTED) {
            for (ParticipationRequest request : requests) {
                if (request.getStatus() != RequestStatus.PENDING) {
                    throw new ConflictException(
                            "Заявка " + request.getId() + " не в статусе PENDING. Текущий статус: " + request.getStatus()
                    );
                }
                request.setStatus(RequestStatus.REJECTED);
                rejected.add(request);
            }
            requestRepository.saveAll(rejected);

        } else {
            throw new IllegalArgumentException("Неизвестный статус: " + status);
        }

        return new RequestStatusUpdateResult(
                confirmed.stream().map(participationRequestDtoMapper::toDto).collect(Collectors.toList()),
                rejected.stream().map(participationRequestDtoMapper::toDto).collect(Collectors.toList())
        );
    }


    private void rejectAllPendingRequests(Long eventId, List<ParticipationRequest> rejected) {
        List<ParticipationRequest> pending = requestRepository.findByEventIdAndStatus(eventId, RequestStatus.PENDING);
        for (ParticipationRequest r : pending) {
            r.setStatus(RequestStatus.REJECTED);
            rejected.add(r);
        }
    }


    public List<ParticipationRequestDto> getRequestsByUserAsList(Long userId, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        Page<ParticipationRequest> page = requestRepository.findAllByRequesterId(userId, pageable);
        return page.getContent().stream()
                .map(participationRequestDtoMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<ParticipationRequestDto> getUserRequestsAsList(Long userId) {
        log.debug("Getting user requests as list: userId={}", userId);

        List<ParticipationRequest> requests = requestRepository.findByRequesterId(userId);
        return requests.stream()
                .map(participationRequestDtoMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<ParticipationRequestDto> getEventRequestsAsList(Long eventId) {
        log.debug("Getting event requests as list: eventId={}", eventId);

        List<ParticipationRequest> requests = requestRepository.findByEventId(eventId);
        return requests.stream()
                .map(participationRequestDtoMapper::toDto)
                .collect(Collectors.toList());
    }


}