package com.navercorp.pinpoint.profiler.sampler;

import com.navercorp.pinpoint.bootstrap.sampler.SendRate;

/**
 * Created by zhengjunbo on 2018/5/21.
 */
public class TrueSend implements SendRate {
    @Override
    public boolean isNeedSend() {
        return true;
    }

    @Override
    public String toString() {
        return "TrueSend";
    }
}
