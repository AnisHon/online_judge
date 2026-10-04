package com.anishan.user.service;

import com.anishan.api.event.PointAwardEvent;

public interface PointAwardApplicationService {

    void apply(PointAwardEvent event);
}
