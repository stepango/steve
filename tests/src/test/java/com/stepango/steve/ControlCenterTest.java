package com.stepango.steve;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ControlCenterTest {

    @Test
    public void start() throws Exception {
        Job<Object> job = JobTestHelper.makeJob();
        job.start();
        assertEquals(0, ControlCenter.getInstance().JOBS.size());
        ControlCenter.getInstance().JOBS.clear();
    }

    @Test
    public void jobs() throws Exception {
        assertTrue(new ControlCenter().JOBS.isEmpty());
    }

}
