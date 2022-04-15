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
package com.wl4g.iam.gateway.loadbalance.rule.stats;

import static com.wl4g.infra.common.collection.CollectionUtils2.safeMap;
import static com.wl4g.infra.common.lang.Assert2.notNullOf;
import static com.wl4g.infra.common.lang.FastTimeClock.currentTimeMillis;
import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;

import java.net.URI;
import java.time.Duration;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.ApplicationArguments;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.web.server.ServerWebExchange;

import com.wl4g.iam.gateway.loadbalance.config.LoadBalancerProperties;
import com.wl4g.infra.common.task.RunnerProperties;
import com.wl4g.infra.common.task.RunnerProperties.StartupMode;
import com.wl4g.infra.common.task.SafeScheduledTaskPoolExecutor;
import com.wl4g.infra.core.task.ApplicationTaskRunner;

import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.netty.http.client.HttpClient;

/**
 * {@link DefaultLoadBalancerStats}
 * 
 * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version 2021-09-13 v3.0.0
 * @since v3.0.0
 */
@Slf4j
public class DefaultLoadBalancerStats extends ApplicationTaskRunner<RunnerProperties> implements LoadBalancerStats {

    private final LoadBalancerProperties loadBalancerConfig;
    private final LoadBalancerCache loadBalancerCache;
    private final ReachableStrategy reachableStrategy;

    public DefaultLoadBalancerStats(LoadBalancerProperties loadBalancerConfig, LoadBalancerCache loadBalancerCache,
            ReachableStrategy reachableStrategy) {
        super(new RunnerProperties(StartupMode.ASYNC, 1));
        this.loadBalancerConfig = notNullOf(loadBalancerConfig, "loadBalancerConfig");
        this.loadBalancerCache = notNullOf(loadBalancerCache, "loadBalancerCache");
        this.reachableStrategy = notNullOf(reachableStrategy, "reachableStrategy");
    }

    @Override
    protected void onApplicationStarted(ApplicationArguments args, SafeScheduledTaskPoolExecutor worker) throws Exception {
        worker.scheduleWithFixedDelay(this, loadBalancerConfig.getStats().getInitialDelaySeconds(),
                loadBalancerConfig.getStats().getDelaySeconds(), TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        loadBalancerCache.getAllServices().forEach((serviceId, instances) -> {
            safeMap(instances).forEach((instanceId, instance) -> {
                try {
                    doPing(instance);
                } catch (Exception e) {
                    log.warn(format("Failed to the request ping. serviceId=%s, instanceId=%s", serviceId, instanceId), e);
                }
            });
        });
    }

    @Override
    public void register(List<ServiceInstanceStatus> instances) {
        if (nonNull(instances)) {
            instances.stream().forEach(i -> getOrCreateInstance(i.getInstance().getServiceId(), i.getInstance().getInstanceId()));
        }
    }

    @Override
    public int connect(ServerWebExchange exchange, ServiceInstance instance) {
        exchange.getAttributes().put(KEY_COST_TIME, currentTimeMillis());
        ServiceInstanceStatus status = getOrCreateInstance(instance.getServiceId(), instance.getInstanceId());
        return status.getStats().getConnections().addAndGet(1);
    }

    @Override
    public int disconnect(ServerWebExchange exchange, ServiceInstance instance) {
        ServiceInstanceStatus status = getOrCreateInstance(instance.getServiceId(), instance.getInstanceId());
        Stats stats = status.getStats();
        long beginTime = exchange.getRequiredAttribute(KEY_COST_TIME);
        save(status, new PassivePing((currentTimeMillis() - beginTime)));
        return stats.getConnections().addAndGet(-1);
    }

    @Override
    public List<ServiceInstanceStatus> getReachableInstances(String serviceId) {
        return getOrCreateService(serviceId).values().stream().filter(i -> i.getStats().getAlive()).collect(toList());
    }

    @Override
    public List<ServiceInstanceStatus> getAllInstances(String serviceId) {
        return getOrCreateService(serviceId).values().stream().collect(toList());
    }

    protected ServiceInstanceStatus getOrCreateInstance(String serviceId, String instanceId) {
        Map<String, ServiceInstanceStatus> service = getOrCreateService(serviceId);
        ServiceInstanceStatus status = service.get(instanceId);
        if (isNull(status)) {
            synchronized (instanceId) {
                status = service.get(instanceId);
                if (isNull(status)) {
                    service.put(instanceId, status = new ServiceInstanceStatus());
                }
            }
        }
        return status;
    }

    protected Map<String, ServiceInstanceStatus> getOrCreateService(String serviceId) {
        Map<String, ServiceInstanceStatus> instances = loadBalancerCache.getService(serviceId);
        if (isNull(instances)) {
            synchronized (serviceId) {
                instances = loadBalancerCache.getService(serviceId);
                if (isNull(instances)) {
                    loadBalancerCache.putService(serviceId, instances = new ConcurrentHashMap<>(16));
                }
            }
        }
        return instances;
    }

    protected Disposable doPing(ServiceInstanceStatus status) {
        /**
         * Notice: A timeout must be set when pinging an instance group to
         * prevent the group from being blocked for too long. Problems with
         * netflix ribbon implementation see:
         * {@link com.netflix.loadbalancer.BaseLoadBalancer.SerialPingStrategy}
         * and {@link com.netflix.loadbalancer.PingUrl#isAlive}
         * see:https://stackoverflow.com/questions/61843235/reactor-netty-not-getting-an-httpserver-response-when-the-httpclient-subscribes
         * see:https://github.com/reactor/reactor-netty/issues/151
         */
        Duration timeout = Duration.ofMillis(loadBalancerConfig.getStats().getTimeoutMs());
        return HttpClient.create()
                .wiretap(true)
                .get()
                .uri(buildUri(status))
                .responseContent()
                .aggregate()
                .asString()
                .timeout(timeout,
                        Mono.fromRunnable(() -> save(status, new ActivePing(currentTimeMillis(), true, null, null))))
                .doFinally(signal -> {
                    if (signal != SignalType.ON_COMPLETE) {
                        // Failed to request ping.
                        if (signal == SignalType.ON_ERROR || signal == SignalType.CANCEL) {
                            save(status, new ActivePing(currentTimeMillis(), false, null, null));
                        }
                    }
                })
                // main thread non-blocking.
                .subscribe(response -> {
                    save(status, new ActivePing(currentTimeMillis(), false, null, response));
                }, ex -> {
                    save(status, new ActivePing(currentTimeMillis(), false, null, null));
                }, () -> {
                    log.debug("Ping completion");
                });
    }

    protected URI buildUri(ServiceInstanceStatus status) {
        ServiceInstance instance = status.getInstance();
        return URI.create(
                instance.getScheme().concat("://").concat(instance.getHost()).concat(":").concat(instance.getPort() + "").concat(
                        "/healthz"));
    }

    protected synchronized void save(ServiceInstanceStatus status, ActivePing activePing) {
        Stats stats = status.getStats();
        Deque<ActivePing> queue = stats.getActivePings();
        if (queue.size() > loadBalancerConfig.getStats().getPingQueue()) {
            queue.poll();
        }
        queue.offer(activePing);
        reachableStrategy.updateStatus(loadBalancerConfig, status);
    }

    protected synchronized void save(ServiceInstanceStatus status, PassivePing passivePing) {
        Stats stats = status.getStats();
        Deque<PassivePing> queue = stats.getPassivePings();
        if (queue.size() > loadBalancerConfig.getStats().getPingQueue()) {
            queue.poll();
        }
        queue.offer(passivePing);
        stats.setLatestCostTime(queue.peekLast().getCostTime());
        stats.setOldestCostTime(queue.peekLast().getCostTime());
        stats.setMaxCostTime(queue.stream().mapToDouble(p -> p.getCostTime()).max().getAsDouble());
        stats.setMinCostTime(queue.stream().mapToDouble(p -> p.getCostTime()).min().getAsDouble());
        stats.setAvgCostTime(queue.stream().mapToDouble(p -> p.getCostTime()).average().getAsDouble());
    }

}
