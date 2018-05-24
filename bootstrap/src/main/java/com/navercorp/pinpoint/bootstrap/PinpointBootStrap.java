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

package com.navercorp.pinpoint.bootstrap;

import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

import com.navercorp.pinpoint.ProductInfo;

/**
 * @author emeroad
 * @author netspider
 * pinpoint 总入口，使用javaagent技术
 */
public class PinpointBootStrap {

    private static final BootLogger logger = BootLogger.getLogger(PinpointBootStrap.class.getName());

    private static final LoadState STATE = new LoadState();


    public static void premain(String agentArgs, Instrumentation instrumentation) {
        if (agentArgs == null) {
            agentArgs = "";
        }
        logger.info(ProductInfo.NAME + " agentArgs:" + agentArgs);
        logger.info("classLoader:" + PinpointBootStrap.class.getClassLoader());
        logger.info("contextClassLoader:" + Thread.currentThread().getContextClassLoader());
        if (Object.class.getClassLoader() != PinpointBootStrap.class.getClassLoader()) {
            final URL location = LocationUtils.getLocation(PinpointBootStrap.class);
            logger.warn("Invalid pinpoint-bootstrap.jar:" + location);
            return;
        }


        final boolean success = STATE.start();
        if (!success) {
            logger.warn("pinpoint-bootstrap already started. skipping agent loading.");
            return;
        }
        //整理启动参数成一个map
        Map<String, String> agentArgsMap = argsToMap(agentArgs);

        //java agent路径解析器
        JavaAgentPathResolver javaAgentPathResolver = JavaAgentPathResolver.newJavaAgentPathResolver();
        String agentPath = javaAgentPathResolver.resolveJavaAgentPath();
        logger.info("JavaAgentPath:" + agentPath);
        //定位pinpoint所需要的资源
        final ClassPathResolver classPathResolver = new AgentDirBaseClassPathResolver(agentPath);
        if (!classPathResolver.verify()) {
            logger.warn("Agent Directory Verify fail. skipping agent loading.");
            logPinpointAgentLoadFail();
            return;
        }

        //获取bootstrapFile，并且将所需要的jar包路径注入到instrumentation中
        BootstrapJarFile bootstrapJarFile = classPathResolver.getBootstrapJarFile();
        appendToBootstrapClassLoader(instrumentation, bootstrapJarFile);

        ClassLoader parentClassLoader = getParentClassLoader();
        if (ModuleUtils.isModuleSupported()) { //检查是否支持，java9特有的方式
            logger.info("java9 module detected");
            logger.info("ModuleBootLoader start");
            ModuleBootLoader moduleBootLoader = new ModuleBootLoader(instrumentation, parentClassLoader);
            moduleBootLoader.loadModuleSupport();

            // for development option
            // avoid java.sql.Date not found
            // will be removed future release
            if ("platform".equalsIgnoreCase(System.getProperty("pinpoint.dev.option.agentClassLoader"))) {
                parentClassLoader = moduleBootLoader.getPlatformClassLoader();
                logger.info("override parentClassLoader:" + parentClassLoader);
            }
        }

        //准备好的参数启动pinpoint核心
        PinpointStarter bootStrap = new PinpointStarter(parentClassLoader, agentArgsMap, bootstrapJarFile, classPathResolver, instrumentation);
        if (!bootStrap.start()) {
            logPinpointAgentLoadFail();
        }

    }


    private static ClassLoader getParentClassLoader() {
        final ClassLoader classLoader = getPinpointBootStrapClassLoader();
        if (classLoader == Object.class.getClassLoader()) {
            logger.info("parentClassLoader:BootStrapClassLoader ref{}" + classLoader );
        } else {
            logger.info("parentClassLoader:" + classLoader);
        }
        return classLoader;
    }

    private static ClassLoader getPinpointBootStrapClassLoader() {
        return PinpointBootStrap.class.getClassLoader();
    }


    private static Map<String, String> argsToMap(String agentArgs) {
        ArgsParser argsParser = new ArgsParser();
        Map<String, String> agentArgsMap = argsParser.parse(agentArgs);
        if (!agentArgsMap.isEmpty()) {
            logger.info("agentParameter:" + agentArgs);
        }
        return agentArgsMap;
    }

    private static void appendToBootstrapClassLoader(Instrumentation instrumentation, BootstrapJarFile agentJarFile) {
        List<JarFile> jarFileList = agentJarFile.getJarFileList();
        for (JarFile jarFile : jarFileList) {
            logger.info("appendToBootstrapClassLoader:" + jarFile.getName());
            instrumentation.appendToBootstrapClassLoaderSearch(jarFile);
        }
    }



    private static void logPinpointAgentLoadFail() {
        final String errorLog =
                "*****************************************************************************\n" +
                        "* Pinpoint Agent load failure\n" +
                        "*****************************************************************************";
        System.err.println(errorLog);
    }


}
