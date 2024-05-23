package com.uangel.ccaas.msggw.service.schedule.handler;

import com.uangel.ccaas.msggw.service.schedule.base.IntervalTaskUnit;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class SessionCheckHandler extends IntervalTaskUnit {
    public SessionCheckHandler(int interval) {
        super(interval);
    }

    @Override
    public void run() {
        try {
            log.info("Session count={}", sessionManager.getSessionMaps().size());

        } catch (Exception e) {
            log.warn("Failed to EndSession handle", e);
        }
    }


}
