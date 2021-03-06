/*
 * Copyright 2014 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.profiler;

import com.navercorp.pinpoint.ProductInfo;
import com.navercorp.pinpoint.bootstrap.Agent;
import com.navercorp.pinpoint.bootstrap.AgentOption;
import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;
import com.navercorp.pinpoint.bootstrap.interceptor.InterceptorInvokerHelper;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerBinder;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.common.util.Assert;
import com.navercorp.pinpoint.profiler.context.module.ApplicationContext;
import com.navercorp.pinpoint.profiler.context.module.DefaultApplicationContext;
import com.navercorp.pinpoint.profiler.context.module.DefaultModuleFactoryResolver;
import com.navercorp.pinpoint.profiler.context.module.ModuleFactory;
import com.navercorp.pinpoint.profiler.context.module.ModuleFactoryResolver;
import com.navercorp.pinpoint.profiler.util.SystemPropertyDumper;
import com.navercorp.pinpoint.profiler.logging.Slf4jLoggerBinder;

import com.navercorp.pinpoint.rpc.ClassPreLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author emeroad
 * @author koo.taejin
 * @author hyungil.jeong
 * 默认agent启动入口
 */
public class DefaultAgent implements Agent {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final PLoggerBinder binder;

    private final ProfilerConfig profilerConfig;

    private final ApplicationContext applicationContext;


    private final Object agentStatusLock = new Object();
    private volatile AgentStatus agentStatus;


    static {
        // Preload classes related to pinpoint-rpc module.
        //启动pinpoint的服务端监听和发送端的链接
        ClassPreLoader.preload();
    }

    public DefaultAgent(AgentOption agentOption) {
        if (agentOption == null) {
            throw new NullPointerException("agentOption must not be null");
        }
        if (agentOption.getInstrumentation() == null) {
            throw new NullPointerException("instrumentation must not be null");
        }
        if (agentOption.getProfilerConfig() == null) {
            throw new NullPointerException("profilerConfig must not be null");
        }

        logger.info("AgentOption:{}", agentOption);

        //绑定一个pinpointLog日志体系
        this.binder = new Slf4jLoggerBinder();
        bindPLoggerFactory(this.binder);

        //获取系统中的配置信息
        dumpSystemProperties();
        dumpConfig(agentOption.getProfilerConfig());

        changeStatus(AgentStatus.INITIALIZING);

        //获取配置信息
        this.profilerConfig = agentOption.getProfilerConfig();

        //启动上下文
        this.applicationContext = newApplicationContext(agentOption);

        
        InterceptorInvokerHelper.setPropagateException(profilerConfig.isPropagateInterceptorException());
    }

    protected ApplicationContext newApplicationContext(AgentOption agentOption) {
        Assert.requireNonNull(agentOption, "agentOption must not be null");
        ProfilerConfig profilerConfig = Assert.requireNonNull(agentOption.getProfilerConfig(), "profilerConfig must not be null");

        ModuleFactoryResolver moduleFactoryResolver = new DefaultModuleFactoryResolver(profilerConfig.getInjectionModuleFactoryClazzName());
        ModuleFactory moduleFactory = moduleFactoryResolver.resolve();
        return new DefaultApplicationContext(agentOption, moduleFactory);
    }

    protected ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    private void dumpSystemProperties() {
        SystemPropertyDumper dumper = new SystemPropertyDumper();
        dumper.dump();
    }

    private void dumpConfig(ProfilerConfig profilerConfig) {
        if (logger.isInfoEnabled()) {
            logger.info("{}\n{}", "dumpConfig", profilerConfig);

        }
    }

    private void changeStatus(AgentStatus status) {
        this.agentStatus = status;
        if (logger.isDebugEnabled()) {
            logger.debug("Agent status is changed. {}", status);
        }
    }

    private void bindPLoggerFactory(PLoggerBinder binder) {
        final String binderClassName = binder.getClass().getName();
        PLogger pLogger = binder.getLogger(binder.getClass().getName());
        pLogger.info("PLoggerFactory.initialize() bind:{} cl:{}", binderClassName, binder.getClass().getClassLoader());
        // Set binder to static LoggerFactory
        // Should we unset binder at shutdown hook or stop()?
        PLoggerFactory.initialize(binder);
    }


    @Override
    public void start() {
        synchronized (agentStatusLock) {
            if (this.agentStatus == AgentStatus.INITIALIZING) {
                changeStatus(AgentStatus.RUNNING);
            } else {
                logger.warn("Agent already started.");
                return;
            }
        }
        logger.info("Starting {} Agent.", ProductInfo.NAME);
        this.applicationContext.start();

    }

    @Override
    public void stop() {
        synchronized (agentStatusLock) {
            if (this.agentStatus == AgentStatus.RUNNING) {
                changeStatus(AgentStatus.STOPPED);
            } else {
                logger.warn("Cannot stop agent. Current status = [{}]", this.agentStatus);
                return;
            }
        }
        logger.info("Stopping {} Agent.", ProductInfo.NAME);
        this.applicationContext.close();

        // for testcase
        if (profilerConfig.getStaticResourceCleanup()) {
            PLoggerFactory.unregister(this.binder);
        }
    }

}
