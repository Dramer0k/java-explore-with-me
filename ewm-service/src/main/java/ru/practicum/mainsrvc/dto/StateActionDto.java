package ru.practicum.mainsrvc.dto;

import ru.practicum.mainsrvc.entity.EventAction;

public class StateActionDto {

    private EventAction stateAction;

    public EventAction getStateAction() {
        return stateAction;
    }

    public void setStateAction(EventAction stateAction) {
        this.stateAction = stateAction;
    }
}