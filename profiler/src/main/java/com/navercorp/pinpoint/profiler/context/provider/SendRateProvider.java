package com.navercorp.pinpoint.profiler.context.provider;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;
import com.navercorp.pinpoint.bootstrap.sampler.SendRate;
import com.navercorp.pinpoint.profiler.sampler.SendRateFactory;

/**
 * Created by zhengjunbo on 2018/5/21.
 */
public class SendRateProvider implements Provider<SendRate> {

    private final ProfilerConfig profilerConfig;

    @Inject
    public SendRateProvider(ProfilerConfig profilerConfig) {
        this.profilerConfig = profilerConfig;
    }

    @Override
    public SendRate get() {
        int sendRate = profilerConfig.getSendRate();
        SendRateFactory sendRateFactory = new SendRateFactory();
        return sendRateFactory.createSendRate(sendRate);
    }
}
