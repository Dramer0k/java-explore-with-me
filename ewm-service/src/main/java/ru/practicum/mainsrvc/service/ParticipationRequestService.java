package ru.practicum.mainsrvc.service;

import org.springframework.data.domain.Page;
import ru.practicum.mainsrvc.dto.ParticipationRequestDto;
import ru.practicum.mainsrvc.dto.ParticipationRequestStatusDto;
import ru.practicum.mainsrvc.dto.RequestStatusUpdateResult;
import ru.practicum.mainsrvc.entity.RequestStatus;

import java.util.List;

public interface ParticipationRequestService {
    ParticipationRequestDto createRequest(Long userId, Long eventId);

    Page<ParticipationRequestDto> getRequestsByUser(Long userId, int from, int size);

    List<ParticipationRequestDto> getRequestsForEvent(Long eventId);

    ParticipationRequestDto approveRequest(Long requestId, Long initiatorId);

    ParticipationRequestDto approveOrReject(Long requestId, Long initiatorId, RequestStatus status);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    Page<ParticipationRequestDto> getRequestsByUserAndEvent(Long userId, Long eventId, int from, int size);

    Page<ParticipationRequestDto> getRequestsByEvent(Long eventId, int from, int size);

    RequestStatusUpdateResult processRequestStatus(
            Long userId, Long eventId, ParticipationRequestStatusDto dto);

    List<ParticipationRequestDto> getRequestsByUserAsList(Long userId, int from, int size);

    List<ParticipationRequestDto> getUserRequestsAsList(Long userId);

    List<ParticipationRequestDto> getEventRequestsAsList(Long eventId);

}