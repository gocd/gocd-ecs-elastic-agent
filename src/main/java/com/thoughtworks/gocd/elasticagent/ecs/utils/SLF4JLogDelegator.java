/*
 * Copyright 2022 Thoughtworks, Inc.
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

package com.thoughtworks.gocd.elasticagent.ecs.utils;

import com.thoughtworks.go.plugin.api.logging.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.AbstractLogger;

import java.util.Arrays;
import java.util.stream.Stream;

public class SLF4JLogDelegator extends AbstractLogger {
    private final Logger logger;

    public SLF4JLogDelegator(String name, Class<?> fallbackLoggerClass) {
        super.name = name;
        logger = Logger.getLoggerFor(tryFindClassFor(name, fallbackLoggerClass));
    }

    private static Class<?> tryFindClassFor(String name, Class<?> fallbackLoggerClass) {
        try {
            return fallbackLoggerClass.getClassLoader().loadClass(name);
        } catch (Exception e) {
            return fallbackLoggerClass;
        }
    }

    @Override
    protected String getFullyQualifiedCallerName() {
        return "";
    }

    @Override
    protected void handleNormalizedLoggingCall(Level level, Marker marker, String messagePattern, Object[] arguments, Throwable throwable) {
        Object[] combinedArguments = Stream.concat(
                arguments != null ? Arrays.stream(arguments) : Stream.empty(),
                Stream.ofNullable(throwable)
        ).toArray();

        switch (level) {
            case TRACE, DEBUG:
                logger.debug(messagePattern, combinedArguments);
                break;
            case INFO:
                logger.info(messagePattern, combinedArguments);
                break;
            case WARN:
                logger.warn(messagePattern, combinedArguments);
                break;
            case ERROR:
                logger.error(messagePattern, combinedArguments);
                break;
        }
    }

    @Override
    public boolean isTraceEnabled() {
        return true;
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return true;
    }

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return true;
    }

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return true;
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return true;
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return true;
    }
}
