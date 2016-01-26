package com.stepango.steve;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JobUtilsTest {

    @Test
    public void testExtractJobParams() throws Exception {
        assertEquals(1, JobUtils.extractJobParams(JobTestHelper.makeJob()).size());
        assertEquals(2, JobUtils.extractJobParams(JobTestHelper.makeJobWith2Serializable()).size());
    }

}