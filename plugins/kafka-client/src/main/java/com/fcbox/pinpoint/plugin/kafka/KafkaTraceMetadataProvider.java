package com.fcbox.pinpoint.plugin.kafka;

import com.navercorp.pinpoint.common.trace.TraceMetadataProvider;
import com.navercorp.pinpoint.common.trace.TraceMetadataSetupContext;

/**
 * @author Jinkai.Ma
 */
public class KafkaTraceMetadataProvider implements TraceMetadataProvider {
    @Override
    public void setup(TraceMetadataSetupContext context) {
        context.addServiceType(KafkaConfiguration.KAFKA_CLIENT_PRODUCER);
        context.addServiceType(KafkaConfiguration.KAFKA_CLIENT_CONSUMER);
        context.addServiceType(KafkaConfiguration.KAFKA_CLIENT_CONSUMER_NO_STATISTICS);
        context.addAnnotationKey(KafkaConfiguration.KAFKA_ARGS_ANNOTATION_KEY);
//        context.addAnnotationKey(KafkaConfiguration.DUBBO_RESULT_ANNOTATION_KEY);
//        context.addAnnotationKey(KafkaConfiguration.DUBBO_RPC_ANNOTATION_KEY);
    }
}
