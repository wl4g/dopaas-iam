/*
 * Copyright 2017 ~ 2025 the original author or authors. <wanglsir@gmail.com, 983708408@qq.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wl4g.iam.gateway.route;

import static com.wl4g.component.common.lang.Assert2.isTrue;
import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.ScheduledFuture;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;

import com.wl4g.component.common.task.RunnerProperties;
import com.wl4g.component.core.task.ApplicationTaskRunner;
import com.wl4g.iam.gateway.exception.CurrentlyInRefreshingException;
import com.wl4g.iam.gateway.route.config.RouteProperties;

/**
 * Gateway configuration refreshable scheduler coordinator.
 *
 * @author Wangl.sir <983708408@qq.com>
 * @version 2019年6月2日
 * @since v1.0
 */
public class TimingRoutesRefresher extends ApplicationTaskRunner<RunnerProperties> implements ApplicationRunner, DisposableBean {

    @Autowired
    protected RouteProperties config;

    @Autowired
    protected IRouteCacheRefresh refresher;

    /** Refreshing scheduled future */
    protected ScheduledFuture<?> future;

    public TimingRoutesRefresher() {
        super(new RunnerProperties().withConcurrency(1));
    }

    @Override
    public void destroy() throws Exception {
        super.close();
    }

    /**
     * Starting configuration refresher schedule.
     */
    @Override
    public void run() {
        refreshRouters();
    }

    /**
     * Restarting configuration refresher schedule.
     */
    public void restartRefresher() {
        isTrue(future.cancel(false), CurrentlyInRefreshingException.class,
                "Updating refreshDelayMs failed, because refreshing is currently in progressing");

        refreshRouters();
    }

    public IRouteCacheRefresh getRefresher() {
        return refresher;
    }

    /**
     * Creating or refreshing routers.
     * 
     * @throws IllegalStateException
     */
    private void refreshRouters() throws IllegalStateException {
        if (nonNull(future) && !future.isDone()) {
            throw new IllegalStateException(format("Cannot refresh routes, because already refreshing task. %s", future));
        }

        this.future = getWorker().scheduleWithFixedDelay(() -> {
            try {
                log.info("Refreshing routes ... - refreshDelayMs: {}", config.getRefreshDelayMs());
                refresher.refreshRoutesPermanentToMemery();
            } catch (Exception e) {
                log.error("Failed to refreshing routes.", e);
            }
        }, 1_000L, config.getRefreshDelayMs(), MILLISECONDS);

    }

}