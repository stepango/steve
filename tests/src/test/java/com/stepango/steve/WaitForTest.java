package com.stepango.steve;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

public class WaitForTest {

    @Test
    public void waitFor() throws Exception {
        ArrayList<String> result = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        Job<String> job = JobTestHelper.makeJob("1", "1", 1, result::add);
        Job<String> job2 = JobTestHelper.makeJob("2", "2", 1, (e) -> {
            result.add(e);
            latch.countDown();
        });
        Job<String> job3 = JobTestHelper.makeJob("3", "3", 1, result::add);
        job2.waitFor(new JobEvent(job3.jobId, Job.State.FINISHED));
        job3.waitFor(new JobEvent(job.jobId, Job.State.FINISHED));
        job.start();
        job2.start();
        job3.start();
        latch.await();
        Assert.assertArrayEquals(new String[]{"1", "3", "2"}, result.toArray());
    }

}
