package com.kaycheung.product_service.messaging.inbox;

public record InboxEventProcessResult(
        InboxEvent inboxEvent,
        InboxEventProcessStatus status,
        String errorString
) {
    public static InboxEventProcessResult success(InboxEvent inboxEvent){
        return new InboxEventProcessResult(inboxEvent, InboxEventProcessStatus.SUCCESS, null);
    }

    public static InboxEventProcessResult failed(InboxEvent inboxEvent, String reason){
        return new InboxEventProcessResult(inboxEvent, InboxEventProcessStatus.FAILED, reason);
    }

    public static InboxEventProcessResult dead(InboxEvent inboxEvent, String reason){
        return new InboxEventProcessResult(inboxEvent, InboxEventProcessStatus.FAILED, reason);
    }
}
