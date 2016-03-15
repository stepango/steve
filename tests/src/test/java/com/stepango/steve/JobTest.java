package com.stepango.steve;

import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import rx.observers.TestSubscriber;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class JobTest {

    private Job<Object> job = JobTestHelper.makeJob();

    @Test
    public void init() throws Exception {
        assertFalse(job.isRunning());
        assertEquals(Job.State.CREATED, job.currentState);
        assertEquals(Job.Strategy.WAIT_FOR_PREVIOUS, job.strategy);
        assertEquals(Job.Type.PARALLEL, job.type);
        assertFalse(job.jobId.isEmpty());
    }

    @Test
    public void reflection() throws Exception {
        assertEquals(job.getClass().getSuperclass(), Job.class);
    }

    @Test
    public void generateJobId() throws Exception {
        String jobId = UUID.randomUUID().toString();
        Job<String> job = JobTestHelper.makeJob(jobId);
        assertTrue(job.jobId.contains(job.sPrefix));
        assertTrue(job.jobId.contains(jobId));
    }

    @Test(expected = IllegalStateException.class)
    public void waitFor() throws Exception {
        Job<Object> job = JobTestHelper.makeJob();
        JobEvent event = new JobEvent(job.jobId);
        job.waitFor(event);
        job.waitFor(event);
    }

    @Test(expected = IllegalArgumentException.class)
    public void waitForChecks() {
        Job job = JobTestHelper.makeJob();
        JobEvent event = new JobEvent(job.jobId);
        job.waitFor(event, event);
    }

    @Test
    public void start() {
        assertNotNull(JobTestHelper.makeJob().start());
    }

    @Test
    public void unsubscribe() {
        Job<Object> job = JobTestHelper.makeJob();
        job.unsubscribe();
        assertNotNull(job.eventsEmitter);
        assertNull(job.subscription);
        assertEquals(Job.State.FINISHED, job.currentState);
    }

    @Test
    public void unsubscribeAfterStart() {
        Job<Object> job = JobTestHelper.makeJob();
        job.start();
        job.unsubscribe();
        assertNotNull(job.eventsEmitter);
        assertNull(job.subscription);
        assertEquals(Job.State.FINISHED, job.currentState);
    }

    @Test
    public void executeAfter() {
        final AtomicInteger x = new AtomicInteger();
        Job<Object> job = JobTestHelper.makeJob();
        job.executeAfter(objectJob -> x.incrementAndGet());
        job.executeAfter(objectJob -> x.incrementAndGet());
        TestSubscriber<Object> testSubscriber = new TestSubscriber<>();
        job.start()
                .asObservable()
                .subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        assertEquals(2, x.intValue());
    }

}