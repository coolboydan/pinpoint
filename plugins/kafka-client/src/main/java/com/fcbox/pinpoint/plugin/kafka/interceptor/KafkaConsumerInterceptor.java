package com.fcbox.pinpoint.plugin.kafka.interceptor;

import com.fcbox.kafka.FcKafkaConsumer;
import com.fcbox.kafka.bean.KafkaEvent;
import com.fcbox.kafka.core.FcKafkaWrapper;
import com.fcbox.pinpoint.plugin.kafka.KafkaConfiguration;
import com.navercorp.pinpoint.bootstrap.context.*;
import com.navercorp.pinpoint.bootstrap.interceptor.AroundInterceptor;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.util.NumberUtils;
import com.navercorp.pinpoint.common.trace.ServiceType;
import org.apache.kafka.clients.producer.ProducerRecord;

/**
 * Created by zhengjunbo on 2018/5/23.
 */
public class KafkaConsumerInterceptor implements AroundInterceptor {
    private static final String SCOPE_NAME = "##DUBBO_PROVIDER_TRACE";
    protected final PLogger logger;
    protected final boolean isDebug;

    protected final MethodDescriptor methodDescriptor;
    protected final TraceContext traceContext;

    public KafkaConsumerInterceptor(MethodDescriptor methodDescriptor, TraceContext traceContext) {
        this.logger = PLoggerFactory.getLogger(KafkaConsumerInterceptor.class);
        this.isDebug = logger.isDebugEnabled();
        this.methodDescriptor = methodDescriptor;
        this.traceContext = traceContext;
    }


    @Override
    public void before(Object target, Object[] args) {
        if (isDebug) {
            logger.beforeInterceptor(target, args);
        }

        Trace trace = traceContext.currentRawTraceObject();
        if (trace == null) {
            trace = createTrace(target, args);
            if (trace == null) {
                return;
            }

            try {
                final SpanRecorder recorder = trace.getSpanRecorder();
                doInBeforeTrace(recorder, target, args);
            } catch (Throwable th) {
                if (logger.isWarnEnabled()) {
                    logger.warn("BEFORE. Caused:{}", th.getMessage(), th);
                }
            }
            return;
        }

        if(isDebug) {
            logger.debug("Found trace {}, sampled={}.", trace, trace.canSampled());
        }
        // adding scope as flag for not closing the trace created by other interceptor
        trace.addScope(SCOPE_NAME);

        //如果不能统计
        if (!trace.canSampled()) {
            return;
        }

//        KafkaEvent invocation = (KafkaEvent) args[0];
        SpanEventRecorder recorder = trace.traceBlockBegin();
        recorder.recordServiceType(KafkaConfiguration.KAFKA_CLIENT_CONSUMER_NO_STATISTICS);
        recorder.recordApi(methodDescriptor);
//        recorder.recordAttribute(KafkaConfiguration.DUBBO_RPC_ANNOTATION_KEY,
//                invocation.getInvoker().getInterface().getSimpleName() + ":" + invocation.getMethodName());

    }



    protected Trace createTrace(Object target, Object[] args) {



        KafkaEvent kafkaEvent = (KafkaEvent) args[0];
        if (isDebug) {
            logger.debug("kafkaEvent data{}", kafkaEvent);
        }
        // If this transaction is not traceable, mark as disabled.
        if (kafkaEvent.getHeaders().get(KafkaConfiguration.META_DO_NOT_TRACE) != null) {
            return traceContext.disableSampling();
        }

        String transactionId = kafkaEvent.getHeaders().get(KafkaConfiguration.META_TRANSACTION_ID);

        // If there's no trasanction id, a new trasaction begins here.
        // FIXME There seems to be cases where the invoke method is called after a span is already created.
        // We'll have to check if a trace object already exists and create a span event instead of a span in that case.
        if (transactionId == null) {
            return traceContext.newTraceObject();
        }

        // otherwise, continue tracing with given data. 根据consumer给的参数放入本地链路跟踪
        long parentSpanID = NumberUtils.parseLong(kafkaEvent.getHeaders().get(KafkaConfiguration.META_PARENT_SPAN_ID), SpanId.NULL);
        long spanID = NumberUtils.parseLong(kafkaEvent.getHeaders().get(KafkaConfiguration.META_SPAN_ID), SpanId.NULL);
        short flags = NumberUtils.parseShort(kafkaEvent.getHeaders().get(KafkaConfiguration.META_FLAGS), (short) 0);
        TraceId traceId = traceContext.createTraceId(transactionId, parentSpanID, spanID, flags);
        //生成新的链路。
        return traceContext.continueTraceObject(traceId);
    }


    protected void doInBeforeTrace(SpanRecorder recorder, Object target, Object[] args) {
        KafkaEvent kafkaEvent = (KafkaEvent) args[0];
        FcKafkaWrapper kafkaConsumer = (FcKafkaWrapper) target;

        // You have to record a service type within Server range.
        recorder.recordServiceType(KafkaConfiguration.KAFKA_CLIENT_CONSUMER);


        recorder.recordEndPoint(kafkaConsumer.getClass().getSimpleName()+kafkaEvent.getTopic());

        // If this transaction did not begin here, record parent(client who sent this request) information
        if (!recorder.isRoot()) {
            String parentApplicationName = kafkaEvent.getHeaders().get(KafkaConfiguration.META_PARENT_APPLICATION_NAME);

            if (parentApplicationName != null) {
                short parentApplicationType = NumberUtils.parseShort(kafkaEvent.getHeaders().get(KafkaConfiguration.META_PARENT_APPLICATION_TYPE), ServiceType.UNDEFINED.getCode());
                recorder.recordParentApplication(parentApplicationName, parentApplicationType);

                // Pinpoint finds caller - callee relation by matching caller's end point and callee's acceptor host.
                // https://github.com/naver/pinpoint/issues/1395
//                recorder.recordAcceptorHost(rpcContext.getLocalAddressString());
            }
        }
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
        if (isDebug) {
            logger.afterInterceptor(target, args, result, throwable);
        }

        final Trace trace = traceContext.currentRawTraceObject();

        if (trace == null) {
            return;
        }

        // TODO STATDISABLE this logic was added to disable statistics tracing
        if (!trace.canSampled()) {
            if (trace.getScope(SCOPE_NAME) == null) {
                deleteTrace(trace);
            }
            return;
        }

        try {
            if (trace.getScope(SCOPE_NAME) == null) {
                final SpanRecorder recorder = trace.getSpanRecorder();
                doInAfterTrace(recorder, target, args, result, throwable);
            } else {
                trace.traceBlockEnd();
            }
        } catch (Throwable th) {
            if (logger.isWarnEnabled()) {
                logger.warn("AFTER. Caused:{}", th.getMessage(), th);
            }
        } finally {
            if (trace.getScope(SCOPE_NAME) == null) {
                deleteTrace(trace);
            }
        }
    }


    private void deleteTrace(final Trace trace) {
        if (isDebug) {
            logger.debug("Delete provider include trace={}, sampled={}", trace, trace.canSampled());
        }
        traceContext.removeTraceObject();
        trace.close();
    }

    protected void doInAfterTrace(SpanRecorder recorder, Object target, Object[] args, Object result, Throwable throwable) {

        recorder.recordApi(methodDescriptor);
        KafkaEvent kafkaEvent = (KafkaEvent) args[0];
        recorder.recordAttribute(KafkaConfiguration.KAFKA_ARGS_ANNOTATION_KEY,kafkaEvent);
        if (throwable == null) {
        } else {
            recorder.recordException(throwable);
        }
    }

}
