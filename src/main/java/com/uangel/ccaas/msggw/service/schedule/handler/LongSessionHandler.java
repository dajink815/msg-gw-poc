package com.uangel.ccaas.msggw.service.schedule.handler;

import com.uangel.ccaas.msggw.service.schedule.base.IntervalTaskUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LongSessionHandler extends IntervalTaskUnit {
    public LongSessionHandler(int interval) {
        super(interval);
    }

    @Override
    public void run() {
        try {
            // 삭제할 CallInfo 모으기

            // 삭제

        } catch (Exception e) {
            log.warn("Failed to LongSessionHandler");
        }
    }
}
