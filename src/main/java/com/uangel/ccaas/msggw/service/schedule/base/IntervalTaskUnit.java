package com.uangel.ccaas.msggw.service.schedule.base;

import com.uangel.ccaas.msggw.config.MsgGwConfig;
import com.uangel.ccaas.msggw.service.AppInstance;
import com.uangel.ccaas.msggw.session.SessionManager;
import lombok.Data;

/**
 * @author kangmoo Heo
 */
@Data
public abstract class IntervalTaskUnit implements Runnable {
    protected final AppInstance appInstance = AppInstance.getInstance();
    protected final MsgGwConfig config = appInstance.getConfig();
    protected final SessionManager sessionManager = SessionManager.getInstance();

    protected int interval;

    protected IntervalTaskUnit(int interval) {
        this.interval = interval;
    }
}
