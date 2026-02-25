package com.serdarahmanov.music_app_backend.auth.jobs.resetEmailJob;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

@Getter

@AllArgsConstructor
@NoArgsConstructor
public class SendResetPasswordEmailJob implements JobRequest {

    private Long userId;

    @Override
    public Class<? extends JobRequestHandler> getJobRequestHandler() {
        return SendResetPasswordEmailJobHandler.class;
    }
}
