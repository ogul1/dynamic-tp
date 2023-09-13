/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dromara.dynamictp.adapter.motan;

import com.weibo.api.motan.transport.netty.StandardThreadExecutor;
import org.dromara.dynamictp.core.aware.AwareManager;
import org.dromara.dynamictp.core.aware.TaskEnhanceAware;
import org.dromara.dynamictp.core.reject.RejectHandlerGetter;
import org.dromara.dynamictp.core.support.ExecutorWrapper;
import org.dromara.dynamictp.core.support.task.wrapper.TaskWrapper;

import java.util.List;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.TimeUnit;

/**
 * @author hanli
 * @since 1.1.4
 */
public class StandardThreadExecutorProxy extends StandardThreadExecutor implements TaskEnhanceAware {

    private List<TaskWrapper> taskWrappers;

    public StandardThreadExecutorProxy(ExecutorWrapper executorWrapper) {
        this((StandardThreadExecutor) executorWrapper.getExecutor().getOriginal());
        executorWrapper.setOriginalProxy(this);
        this.taskWrappers = executorWrapper.getTaskWrappers();
        RejectedExecutionHandler handler = getRejectedExecutionHandler();
        setRejectedExecutionHandler(RejectHandlerGetter.getProxy(handler));
    }

    private StandardThreadExecutorProxy(StandardThreadExecutor executor) {
        super(executor.getCorePoolSize(), executor.getMaximumPoolSize(),
                executor.getKeepAliveTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS,
                executor.getMaxSubmittedTaskCount() - executor.getMaximumPoolSize(),
                executor.getThreadFactory(), executor.getRejectedExecutionHandler());
    }

    @Override
    public void execute(Runnable command) {
        command = getEnhancedTask(command, taskWrappers);
        AwareManager.execute(this, command);
        super.execute(command);
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        AwareManager.beforeExecute(this, t, r);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        AwareManager.afterExecute(this, r, t);
    }
}
