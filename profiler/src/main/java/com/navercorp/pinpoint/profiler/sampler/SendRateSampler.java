package com.navercorp.pinpoint.profiler.sampler;

import com.navercorp.pinpoint.bootstrap.sampler.SendRate;
import com.navercorp.pinpoint.common.util.MathUtils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by zhengjunbo on 2018/5/21.
 */
public class SendRateSampler implements SendRate {


    private final AtomicInteger counter = new AtomicInteger(0);
    private final int sendRate;

    public SendRateSampler(int sendRate) {
        if (sendRate <= 0) {
            throw new IllegalArgumentException("Invalid samplingRate " + sendRate);
        }
        this.sendRate = sendRate;
    }



    @Override
    public boolean isNeedSend() {
        int samplingCount = MathUtils.fastAbs(counter.getAndIncrement());
        int isSampling = samplingCount % sendRate;
        return isSampling == 0;
    }

    @Override
    public String toString() {
        return "SendRateSampler{" +
                "counter=" + counter +
                "samplingRate=" + sendRate +
                '}';
    }

}
