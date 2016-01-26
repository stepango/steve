package com.stepango.steve;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;

/**
 * Created by stepangoncarov on 24/01/16.
 */
public class StartTest {

    @Test
    public void start() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Job<Object> job = JobTestHelper.makeJob();
        job.addAfterAction(j -> latch.countDown());
        job.start();
        latch.await();
        assertEquals(0, ControlCenter.getInstance().JOBS.size());
    }

}
