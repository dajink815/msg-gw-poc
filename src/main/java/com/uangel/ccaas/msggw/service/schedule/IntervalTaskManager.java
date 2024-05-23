package com.uangel.ccaas.msggw.service.schedule;

import com.uangel.ccaas.msggw.config.MsgGwConfig;
import com.uangel.ccaas.msggw.service.AppInstance;
import com.uangel.ccaas.msggw.service.schedule.base.IntervalTaskUnit;
import com.uangel.ccaas.msggw.service.schedule.handler.LongSessionHandler;
import com.uangel.ccaas.msggw.service.schedule.handler.SessionCheckHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@Slf4j
public class IntervalTaskManager {
    @Getter
    private static final IntervalTaskManager instance = new IntervalTaskManager();
    private static final int TASK_INTERVAL_SEC = 1000;
    private final Map<String, IntervalTaskUnit> jobs = new HashMap<>();
    private final MsgGwConfig config = AppInstance.getInstance().getConfig();
    private ScheduledExecutorService executorService;
    private boolean isStarted = false;

    private IntervalTaskManager() {
        // Nothing
    }

    public void init() {
        addJob(SessionCheckHandler.class.getSimpleName(), new SessionCheckHandler(TASK_INTERVAL_SEC));
        if (config.getLongSession() > 0) {
            addJob(LongSessionHandler.class.getSimpleName(), new LongSessionHandler(TASK_INTERVAL_SEC));
        }
        start();
    }

    private void start() {
        if (isStarted) {
            log.info("() () () Already Started IntervalTaskManager");
            return;
        }
        isStarted = true;
        executorService = Executors.newScheduledThreadPool(jobs.size());
        for (IntervalTaskUnit runner : jobs.values()) {
            executorService.scheduleAtFixedRate(() -> {
                        Thread.currentThread().setName("IntervalTask_" + runner.getClass().getSimpleName());
                        runner.run();
                    },
                    runner.getInterval() - System.currentTimeMillis() % runner.getInterval(),
                    runner.getInterval(),
                    TimeUnit.MILLISECONDS);
        }
        log.info("() () () IntervalTaskManager Start");
    }

    public void stop() {
        if (!isStarted) {
            log.info("() () () Already Stopped IntervalTaskManager");
            return;
        }
        isStarted = false;
        executorService.shutdown();
        log.info("() () () IntervalTaskManager Stop");
    }

    public void addJob(String name, IntervalTaskUnit runner) {
        if (jobs.get(name) != null) {
            log.warn("() () () Hashmap Key duplication");
            return;
        }
        log.debug("() () () Add Runner [{}]", name);
        jobs.put(name, runner);
    }
}
