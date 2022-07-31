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

package org.slf4j.impl;

import org.slf4j.helpers.MarkerIgnoringBase;

import java.io.Serializable;

import static com.thoughtworks.gocd.elasticagent.ecs.ECSElasticPlugin.LOG;

public class SLF4JLogDelegator extends MarkerIgnoringBase implements Serializable {
    @Override
    public boolean isTraceEnabled() {
        return true;
    }

    @Override
    public void trace(String msg) {
        doLog(msg, LogLevel.TRACE);
    }

    @Override
    public void trace(String format, Object arg) {
        doLog(String.format(format, arg), LogLevel.TRACE);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        doLog(String.format(format, arg1, arg2), LogLevel.TRACE);
    }

    @Override
    public void trace(String format, Object... arguments) {
        doLog(String.format(format, arguments), LogLevel.TRACE);
    }

    @Override
    public void trace(String msg, Throwable t) {
        doLog(msg, LogLevel.TRACE, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public void debug(String msg) {
        doLog(msg, LogLevel.DEBUG);
    }

    @Override
    public void debug(String format, Object arg) {
        doLog(String.format(format, arg), LogLevel.DEBUG);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        doLog(String.format(format, arg1, arg2), LogLevel.DEBUG);
    }

    @Override
    public void debug(String format, Object... arguments) {
        doLog(String.format(format, arguments), LogLevel.DEBUG);
    }

    @Override
    public void debug(String msg, Throwable t) {
        doLog(msg, LogLevel.DEBUG, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public void info(String msg) {
        doLog(msg, LogLevel.INFO);
    }

    @Override
    public void info(String format, Object arg) {
        doLog(String.format(format, arg), LogLevel.INFO);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        doLog(String.format(format, arg1, arg2), LogLevel.INFO);
    }

    @Override
    public void info(String format, Object... arguments) {
        doLog(String.format(format, arguments), LogLevel.INFO);
    }

    @Override
    public void info(String msg, Throwable t) {
        doLog(msg, LogLevel.INFO, t);
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public void warn(String msg) {
        doLog(msg, LogLevel.WARN);
    }

    @Override
    public void warn(String format, Object arg) {
        doLog(String.format(format, arg), LogLevel.WARN);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        doLog(String.format(format, arg1, arg2), LogLevel.WARN);
    }

    @Override
    public void warn(String format, Object... arguments) {
        doLog(String.format(format, arguments), LogLevel.WARN);
    }

    @Override
    public void warn(String msg, Throwable t) {
        doLog(msg, LogLevel.WARN, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    public void error(String msg) {
        doLog(msg, LogLevel.ERROR);
    }

    @Override
    public void error(String format, Object arg) {
        doLog(String.format(format, arg), LogLevel.ERROR);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        doLog(String.format(format, arg1, arg2), LogLevel.ERROR);
    }

    @Override
    public void error(String format, Object... arguments) {
        doLog(String.format(format, arguments), LogLevel.ERROR);
    }

    @Override
    public void error(String msg, Throwable t) {
        doLog(msg, LogLevel.ERROR, t);
    }

    private void doLog(String msg, LogLevel logLevel) {
        doLog(msg, logLevel, null);
    }

    private void doLog(String msg, LogLevel logLevel, Throwable t) {
        switch (logLevel) {
            case TRACE:
                LOG.debug(msg, t);
                break;
            case INFO:
                LOG.info(msg, t);
                break;
            case DEBUG:
                LOG.debug(msg, t);
                break;
            case ERROR:
                LOG.error(msg, t);
                break;
            case WARN:
                LOG.warn(msg, t);
                break;
        }
    }

    private enum LogLevel {
        TRACE, INFO, DEBUG, ERROR, WARN
    }
}
