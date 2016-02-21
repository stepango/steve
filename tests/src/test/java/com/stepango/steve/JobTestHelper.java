package com.stepango.steve;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class JobTestHelper {

    public JobTestHelper() {
        throw new UnsupportedOperationException();
    }

    static Observable.Transformer<String, String> countDownTransformer(CountDownLatch countDown) {
        return o -> o.observeOn(Schedulers.newThread())
                .doOnCompleted(countDown::countDown);
    }

    private static class UuidJob extends Job<Object> {

        public UuidJob() {
            super(UUID.randomUUID().toString());
        }

        @Override
        protected void processResult(Object result) {
            //do nothing
        }

        @Override
        public Observable<Object> operation() {
            return Observable.just(new Object());
        }
    }

    public static Job<Object> makeJob() {
        return new UuidJob();
    }

    public static Job<String> makeJob(String id) {
        return makeJob(id, "", 0, null);
    }

    public static Job<String> makeJob(String id, String result, int delay) {
        return makeJob(id, result, delay, null);
    }

    public static Job<String> makeJob(String id, String result, int delay, Action1<String> resultProcessor) {
        return new Job<String>(id) {
            @Override
            protected void processResult(String result) {
                if (resultProcessor != null) {
                    resultProcessor.call(result);
                }
            }

            @Override
            public Observable<String> operation() {
                return Observable.timer(delay, TimeUnit.MILLISECONDS).map(x -> result);
            }
        };
    }

    public static Job<Object> makeJobWith2Serializable() {
        return new UuidJob() {
            @Override
            protected void processResult(Object result) {
                //do nothing
            }

            @Serialize
            Object o = null;
            @Serialize
            Object o2 = new Object();
        };
    }

    public static Job<Object> countDownJob(CountDownLatch latch) {
        return new UuidJob() {
            @Override
            protected void processResult(Object result) {
                latch.countDown();
            }
        };
    }
}
