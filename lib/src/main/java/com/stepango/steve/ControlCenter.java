package com.stepango.steve;

import java.util.concurrent.ConcurrentHashMap;

import rx.functions.Action1;
import rx.subjects.BehaviorSubject;

//TODO Job shouldn't broadcast events
@SuppressWarnings("Convert2Lambda")
public class ControlCenter {

    //May be replace String by Pair<Class, ID>
    final ConcurrentHashMap<String, Job> JOBS = new ConcurrentHashMap<>();
    private Action1<Job> after = new Action1<Job>() {
        @Override
        public void call(Job job) {
            JOBS.remove(job.jobId);
        }
    };

    ControlCenter() {
        //Empty
    }

    static ControlCenter getInstance() {
        return SingletonHolder.HOLDER_INSTANCE;
    }

    synchronized <S> Job<S> start(Job<S> job) {
        if (couldStart(job)) {
            return applyStrategy(job);
        }
        return job;
    }

    boolean couldStart(Job job) {
        return job.waitingList == null || job.waitingList.isEmpty();
    }

    private <S> Job<S> applyStrategy(Job<S> job) {
        switch (job.strategy) {
            case RESUBSCRIBE_PREVIOUS:
                return resubscribePrevious(job);
            case WAIT_FOR_PREVIOUS:
                return waitForPrevious(job);
            case MANUAL:
                return manual();
            default:
                return null;
        }
    }

    private <S> Job<S> manual() {
        throw new IllegalAccessError("Strategy should not be applied");
    }

    <S> Job<S> waitForPrevious(Job<S> slave) {
        //Since we have one map for all jobs, we need to sync only jobs with same class
        synchronized (slave.getClass()) {
            @SuppressWarnings("unchecked")
            final Job<S> master = JOBS.get(slave.jobId);
            //If we have previous request with same id we just replace events emitter to existing one
            if (master != null) {
                return master;
            } else {
                JOBS.put(slave.jobId, slave);
                slave.addAfterAction(after);
                slave.subscribeTo(slave.operation(), BehaviorSubject.create());
            }
        }
        return slave;
    }

    /**
     * Previous job would receive events from master. Since this call are synchronised, each
     * master could have only one slave. And master could be slave for next job with same id.
     *
     * @param master - new job that will emmit events to slave
     */
    <S> Job<S> resubscribePrevious(Job<S> master) {
        synchronized (master.getClass()) {
            master.addAfterAction(after);
            master.subscribeTo(master.operation(), BehaviorSubject.create());
            @SuppressWarnings("unchecked")
            Job<S> slave = JOBS.put(master.jobId, master);
            if (slave != null) {
                slave.subscribeTo(master.eventsEmitter.asObservable());
            }
        }
        return master;
    }

    public static class SingletonHolder {
        public static final ControlCenter HOLDER_INSTANCE = new ControlCenter();
    }

}
