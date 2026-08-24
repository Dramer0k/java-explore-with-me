package ru.practicum.mainsrvc.dto;

import java.util.List;

public record RequestStatusUpdateResult(
        List<ParticipationRequestDto> confirmedRequests,
        List<ParticipationRequestDto> rejectedRequests
) {}