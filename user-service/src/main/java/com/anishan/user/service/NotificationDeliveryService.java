package com.anishan.user.service;

import com.anishan.api.event.CommunityEvent;

public interface NotificationDeliveryService {

    void deliver(CommunityEvent event);
}
