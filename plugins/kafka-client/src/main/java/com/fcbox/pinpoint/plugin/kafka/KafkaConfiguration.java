package com.fcbox.pinpoint.plugin.kafka;

import com.navercorp.pinpoint.common.trace.AnnotationKey;
import com.navercorp.pinpoint.common.trace.AnnotationKeyFactory;
import com.navercorp.pinpoint.common.trace.ServiceType;
import com.navercorp.pinpoint.common.trace.ServiceTypeFactory;

import static com.navercorp.pinpoint.common.trace.ServiceTypeProperty.QUEUE;
import static com.navercorp.pinpoint.common.trace.ServiceTypeProperty.RECORD_STATISTICS;

/**
 * Created by zhengjunbo on 2018/5/23.
 */
public interface KafkaConfiguration {
    ServiceType KAFKA_CLIENT_PRODUCER = ServiceTypeFactory.of(8400, "KAFKA_CLIENT_PRODUCER", QUEUE, RECORD_STATISTICS);
    ServiceType KAFKA_CLIENT_CONSUMER = ServiceTypeFactory.of(8401, "KAFKA_CLIENT_CONSUMER", QUEUE, RECORD_STATISTICS);
    ServiceType KAFKA_CLIENT_CONSUMER_NO_STATISTICS = ServiceTypeFactory.of(8402, "KAFKA_CLIENT_CONSUMER_NO_STATISTICS", QUEUE, RECORD_STATISTICS);


    AnnotationKey KAFKA_ARGS_ANNOTATION_KEY = AnnotationKeyFactory.of(350, "kafka.args");

    String META_DO_NOT_TRACE = "_KAFKA_DO_NOT_TRACE";
    String META_NEED_SEND = "_KAFKA_NEED_SEND";
    String META_TRANSACTION_ID = "_KAFKA_TRASACTION_ID";
    String META_SPAN_ID = "_KAFKA_SPAN_ID";
    String META_PARENT_SPAN_ID = "_KAFKA_PARENT_SPAN_ID";
    String META_PARENT_APPLICATION_NAME = "_KAFKA_PARENT_APPLICATION_NAME";
    String META_PARENT_APPLICATION_TYPE = "_KAFKA_PARENT_APPLICATION_TYPE";
    String META_FLAGS = "_KAFKA_FLAGS";
}
