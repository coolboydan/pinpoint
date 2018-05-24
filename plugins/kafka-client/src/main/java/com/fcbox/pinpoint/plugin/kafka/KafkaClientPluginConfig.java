package com.fcbox.pinpoint.plugin.kafka;

import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;

import java.util.List;

/**
 * Created by zhengjunbo on 2018/5/23.
 */
public class KafkaClientPluginConfig {
    private final boolean dubboEnabled;

    public KafkaClientPluginConfig(ProfilerConfig config) {
        this.dubboEnabled = config.readBoolean("profiler.kafka.enable", true);
    }

    public boolean isDubboEnabled() {
        return dubboEnabled;
    }

}
