package com.stepango.steve;

import org.junit.Test;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

public class WaitForTest {

    @Test(timeout = 1000)
    public void waitFor() throws Exception {
        ArrayList<String> result = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        Job<String> job = JobTestHelper.makeJob("1", "1", 100, result::add);
        Job<String> job2 = JobTestHelper.makeJob("2", "2", 100, (e) -> {
            result.add(e);
            latch.countDown();
        });
        Job<String> job3 = JobTestHelper.makeJob("3", "3", 100, result::add);
        job2.waitFor(new JobEvent(job3.jobId));
        job3.waitFor(job);
        job.start();
        job2.start();
        job3.start();
        latch.await();
        assertArrayEquals(new String[]{"1", "3", "2"}, result.toArray());
    }

    @Test(timeout = 100)
    public void waitForArray() throws Exception {
        ArrayList<String> result = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        Job<String> job = JobTestHelper.makeJob("1", "1", 0, result::add);
        Job<String> job2 = JobTestHelper.makeJob("2", "2", 0, result::add);
        Job<String> job3 = JobTestHelper.makeJob("3", "3", 0, s -> latch.countDown());
        job3.waitFor(job, job2);
        job3.start();
        job2.start();
        job.start();
        latch.await();
        assertTrue(result.contains("1"));
        assertTrue(result.contains("2"));
    }

}
