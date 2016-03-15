package com.stepango.steve;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import rx.schedulers.Schedulers;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Created by stepangoncarov on 24/01/16.
 */
public class StrategyTest {

    @Test
    public void waitForPrevious() throws Exception {
        Job<String> job1 = JobTestHelper.makeJob("1-waitForPrevious", "1", 100);
        Job<String> job2 = JobTestHelper.makeJob("1-waitForPrevious", "2", 100);
        final String[] result = new String[2];
        final CountDownLatch countDown = new CountDownLatch(2);
        job1.start()
                .asObservable()
                .compose(JobTestHelper.countDownTransformer(countDown))
                .subscribe(x -> result[0] = x);
        job2.start()
                .asObservable()
                .compose(JobTestHelper.countDownTransformer(countDown))
                .subscribe(x -> result[1] = x);
        countDown.await();
        assertArrayEquals(new String[]{"1", "1"}, result);
        assertEquals(0, ControlCenter.getInstance().JOBS.size());
    }

    @Test
    public void resubscribePrevious() throws Exception {
        Job<String> job1 = JobTestHelper.makeJob("1-resubscribePrevious", "1", 100);
        job1.strategy = Job.Strategy.RESUBSCRIBE_PREVIOUS;
        Job<String> job2 = JobTestHelper.makeJob("1-resubscribePrevious", "2", 100);
        job2.strategy = Job.Strategy.RESUBSCRIBE_PREVIOUS;
        final String[] result = new String[2];
        final CountDownLatch countDown = new CountDownLatch(2);
        job1.start()
                .asObservable()
                .compose(JobTestHelper.countDownTransformer(countDown))
                .subscribe(x -> result[0] = x);
        job2.start()
                .asObservable()
                .compose(JobTestHelper.countDownTransformer(countDown))
                .subscribe(x -> result[1] = x);
        countDown.await();
        assertArrayEquals(new String[]{"2", "2"}, result);
        assertEquals(0, ControlCenter.getInstance().JOBS.size());
    }

    @Test(timeout = 100)
    public void processResultTest() throws Exception {
        CountDownLatch latch = new CountDownLatch(2);
        JobTestHelper.countDownJob(latch).start();
        JobTestHelper.countDownJob(latch).start();
        latch.await();
        assertEquals(0, ControlCenter.getInstance().JOBS.size());
    }

}
