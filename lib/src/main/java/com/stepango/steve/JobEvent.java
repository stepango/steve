package com.stepango.steve;

public class JobEvent {

    final String jobId;
    final Job.State state;

    public JobEvent(String jobId, Job.State state) {
        this.jobId = jobId;
        this.state = state;
    }
}