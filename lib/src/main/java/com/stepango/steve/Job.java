package com.stepango.steve;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subjects.Subject;

//TODO switch states internally instead of ControlCenter
@SuppressWarnings({"Convert2Lambda", "Anonymous2MethodRef"})
public abstract class Job<S> {

    public final String sPrefix = idPrefix();
    @SuppressWarnings("IllegalCatch")
    private final Action1<S> onSuccess = new Action1<S>() {
        @Override
        public void call(S result) {
            try {
                processResult(result);
                after();
            } catch (Exception e) {
                onError.call(e);
            }
        }
    };
    @SuppressWarnings("IllegalCatch")
    private final Action1<Throwable> onError = new Action1<Throwable>() {
        @Override
        public void call(Throwable t) {
            try {
                afterError(t);
            } catch (Exception e) {
                recoverFromError(e);
            }
        }
    };

    @Serialize
    final String jobId;
    private final Queue<Action1<Job>> afterActions = new LinkedBlockingDeque<>();
    private final Queue<Action1<Job>> beforeActions = new LinkedBlockingDeque<>();

    Subject<S, S> eventsEmitter;
    boolean isRunning = false;
    State currentState = State.CREATED;
    Strategy strategy = Strategy.WAIT_FOR_PREVIOUS;
    Type type = Type.PARALLEL;

    @Serialize
    Set<JobEvent> waitingList;
    Subscription subscription;

    public Job(String jobId) {
        this.jobId = generateJobId(jobId);
    }

    void recoverFromError(Exception e) {
        throw new UnsupportedOperationException("recoverFromError method not implemented", e);
    }

    void afterError(Throwable t) {

    }

    void after() {
        executeAll(afterActions);
    }

    public void executeAfter(Action1<Job> action) {
        afterActions.add(action);
    }

    //TODO may be we could return here something meaningful
    protected abstract void processResult(S result);

    public boolean isRunning() {
        return isRunning;
    }

    String generateJobId(String initialId) {
        return sPrefix + initialId;
    }

    protected String idPrefix() {
        return getClass().getSimpleName() + "_";
    }

    public abstract Observable<S> operation();

    /**
     * Attempt to start current request
     * Possible variants:
     * 1. Job starts immediately(waiting list is empty and no currently processing requests with same ID)
     * 2. Job will replace currently running job with same id {@link com.stepango.steve.Job.Strategy#RESUBSCRIBE_PREVIOUS}
     * 3. Job will wait for successful execution jobs from waiting list
     * 4. Job will stick with events emitter of, previous launched, same ID job
     */
    public Job<S> start() {
        switchState(Job.State.WAITING);
        before();
        return ControlCenter.getInstance().start(this);
    }

    private void before() {
        executeAll(beforeActions);
    }

    private void executeAll(Queue<Action1<Job>> afterActions) {
        for (Iterator<Action1<Job>> iterator = afterActions.iterator(); iterator.hasNext(); ) {
            Action1<Job> action = iterator.next();
            action.call(this);
            //Remove all actions here
            iterator.remove();
        }
    }

    public void subscribeTo(Observable<S> operation) {
        switchState(State.RUNNING);
        if (subscription != null) {
            subscription.unsubscribe();
        }
        subscription = operation
                .doOnError(onError)
                .doOnNext(onSuccess)
                .doOnCompleted(new Action0() {
                    @Override
                    public void call() {
                        eventsEmitter.onCompleted();
                    }
                })
                .subscribeOn(Schedulers.io())
                .subscribe(new Action1<S>() {
                    @Override
                    public void call(S s) {
                        eventsEmitter.onNext(s);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        eventsEmitter.onError(throwable);
                    }
                });
    }

    public void subscribeTo(Observable<S> operation, final Subject<S, S> subject) {
        if (eventsEmitter != null) {
            throw new IllegalStateException();
        }
        eventsEmitter = subject;
        subscribeTo(operation);
    }

    void setEmitter(Subject<S, S> emitter) {
        if (eventsEmitter != null) {
            throw new IllegalStateException("Emitter is already set");
        }
        eventsEmitter = emitter;
    }

    public void waitFor(JobEvent event) {
        waitFor(Collections.singleton(event));
    }

    public void waitFor(JobEvent... events) {
        waitFor(Arrays.asList(events));
    }

    public void waitFor(List<JobEvent> eventList) {
        HashSet<JobEvent> events = new HashSet<>(eventList);
        if (events.size() < eventList.size()) {
            throw new IllegalArgumentException("duplicate events");
        }
        waitFor(events);
    }

    public void waitFor(Set<JobEvent> events) {
        if (waitingList == null) {
            waitingList = events;
        } else {
            throw new IllegalStateException("Waiting list is already set");
        }
    }

    private void switchState(State state) {
        currentState = state;
    }

    public void unsubscribe() {
        after();
        switchState(State.FINISHED);
        if (subscription != null) {
            subscription.unsubscribe();
            subscription = null;
        }
        if (eventsEmitter != null) {
            eventsEmitter.onCompleted();
            eventsEmitter = null;
        }
    }

    public Observable<S> asObservable() {
        return eventsEmitter.asObservable();
    }

    public void addAfterAction(Action1<Job> after) {
        afterActions.add(after);
    }

    enum State {
        CREATED,
        WAITING,
        RUNNING,
        FINISHED
    }

    enum Type {
        /**
         * Allow jobs with same type run in parallel
         */
        PARALLEL,
        /**
         * Allow jobs with same type run only in series
         */
        IN_SERIES
    }

    public enum Strategy {
        /**
         * If request with same id is in progress it will be stopped.
         */
        RESUBSCRIBE_PREVIOUS,
        /**
         * If request with same id is in progress this request will be stopped.
         */
        WAIT_FOR_PREVIOUS,
        /**
         * Any other requests with same id will not be affected.
         * Should not be used in combination with {@link #start()}.
         */
        MANUAL
    }

}
