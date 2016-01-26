package com.stepango.steve;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.functions.Func1;

final class RetryCondition implements Func1<Observable<? extends Throwable>, Observable<?>> {

    private final int maxRetries;
    private int retryCount;

    RetryCondition(final int maxRetries) {
        this.maxRetries = maxRetries;
        this.retryCount = 0;
    }

    @Override
    public Observable<?> call(Observable<? extends Throwable> attempts) {
        return attempts.flatMap(new Func1<Throwable, Observable<?>>() {
            @Override
            public Observable<?> call(Throwable throwable) {
                if (++retryCount < maxRetries) {
                    // When this Observable calls onNext, the original
                    // Observable will be retried (i.e. re-subscribed).
                    return Observable.timer(1, TimeUnit.SECONDS);
                }

                // Max retries hit. Just pass the error along.
                return Observable.error(throwable);
            }
        });
    }
}