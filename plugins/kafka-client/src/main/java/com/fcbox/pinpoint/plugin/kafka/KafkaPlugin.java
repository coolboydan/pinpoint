package com.fcbox.pinpoint.plugin.kafka;

import com.navercorp.pinpoint.bootstrap.instrument.InstrumentClass;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentException;
import com.navercorp.pinpoint.bootstrap.instrument.Instrumentor;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformCallback;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformTemplate;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformTemplateAware;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPlugin;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPluginSetupContext;

import java.security.ProtectionDomain;

/**
 * Created by zhengjunbo on 2018/5/23.
 */
public class KafkaPlugin implements ProfilerPlugin, TransformTemplateAware {

    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());

    private TransformTemplate transformTemplate;

    @Override
    public void setTransformTemplate(TransformTemplate transformTemplate) {
        this.transformTemplate = transformTemplate;
    }

    @Override
    public void setup(ProfilerPluginSetupContext context) {
        KafkaClientPluginConfig config = new KafkaClientPluginConfig(context.getConfig());
        if (!config.isDubboEnabled()) {
            logger.info("DubboPlugin disabled");
            return;
        }


        this.addTransformers();
    }

    private void addTransformers() {
        transformTemplate.transform("com.fcbox.kafka.core.FcKafkaTemplate", new TransformCallback() {
            @Override
            public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);

                target.getDeclaredMethod("send", "org.apache.kafka.clients.producer.ProducerRecord").addInterceptor("com.fcbox.pinpoint.plugin.kafka.interceptor.KafkaProducerInterceptor");


                return target.toBytecode();
            }
        });
        transformTemplate.transform("com.fcbox.kafka.core.FcKafkaWrapper", new TransformCallback() {
            @Override
            public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);

                target.getDeclaredMethod("processor", "com.fcbox.kafka.bean.KafkaEvent").addInterceptor("com.fcbox.pinpoint.plugin.kafka.interceptor.KafkaConsumerInterceptor");

                return target.toBytecode();
            }
        });
    }
}
