package com.navercorp.pinpoint.profiler.sampler;

import com.navercorp.pinpoint.bootstrap.sampler.SendRate;

/**
 * Created by zhengjunbo on 2018/5/21.
 */
public class SendRateFactory {
    public SendRate createSendRate(int samplingRate){
        if ( samplingRate <= 0) {
            return new FalseSend();
        }
        if (samplingRate == 1) {
            return new TrueSend();
        }
        return new SendRateSampler(samplingRate);
    }
}
