# IAM (Identity Authentication and Access Identification Management)

An enterprise-level open source unified identity authentication and access control management platform, out-of-the-box, supports WeChat/qq/google/facebook and other SNS and openldap joint authentication, AOP implements API-level multi-factor authentication; among them, the enterprise-level gateway module is enhanced: Supports such as canary request-based response cache filter, canary load balancer, universal signature authentication filter, oidc v1/oauth2.x authentication filter, ip filter, traffic replication filter, quota-based request limiter filter Injector, canary-based fault injector filter, and canary-based humanized log filter; among them, the message bus and real-time analysis modules based on Flink/Kafka/Pulsar/Rabbitmq/HBase/ES/Hive support functions such as abnormal events or Real-time risk warning and early warning, as well as historical event analysis reports, etc.

<p align="center">
    <img src="https://github.com/wl4g/iam/tree/master/shots/iam-logo.png" width="150">
    <h3 align="center">IAM</h3>
    <p align="center">
        A enterprise-level universal unified authentication and authorization 5A management platform based on cloud native and Spring Cloud.
        <br>
        <a href="https://github.com/wl4g/iam/"><strong>-- Home Page --</strong></a>
        <br>
        <br>
        <a href="https://maven-badges.herokuapp.com/maven-central/com.xuxueli/xxl-sso/">
            <img src="https://img.shields.io/badge/Maven-3.5+-green.svg" >
        </a>
         <a href="https://github.com/wl4g/iam/releases">
             <img src="https://img.shields.io/badge/release-v2.0.0-green.svg" >
         </a>
        <a href="http://www.apache.org/licenses/LICENSE-2.0">
            <img src="https://img.shields.io/badge/license-Apache2.0+-green.svg" >
        </a>
    </p>    
</p>

English version goes [here](README.md)

## 1. Features

- 1. 简洁：API直观简洁，可快速上手
- 2. 轻量级：环境依赖小，部署与接入成本较低
- 3. 单点登录：只需要登录一次就可以访问所有相互信任的应用系统
- 4. 分布式：接入 IAM/SSO 认证中心的应用，支持分布式部署
- 5. HA：Server端与Client端，均支持集群部署，提高系统可用性
- 6. 跨域：支持跨域应用接入 IAM/SSO 认证中心
- 7. Cookie+Token均支持：支持基于Cookie和基于Token两种接入方式，并均提供Sample项目
- 8. Web+APP均支持：支持Web和APP接入
- 9. 实时性：系统登陆、注销状态，全部有IAM Server统一控制，与Client端准同步
- 10. CS结构：基于CS结构，包括Server"认证中心"与Client"受保护应用"
- 11. 路径排除：支持自定义多个排除路径，支持Ant表达式。用于排除 IAM/SSO 客户端不需要过滤的路径
- 12. 支持多种模式部署运行（local模式）：传统单体应用，没有认证客户端一说，即，IAM server与BizApp在同一JVM进程，好处是部署、运维简单，适合小型管理类项目。
- 13. 支持多种模式部署运行（cluster模式）：将认证中心与业务应用和认证客户端分离，即，IAM client与BizApp在同一JVM进程，IAM server在一个进程，适合微服务或跨站跨域的多应用需要统一认证的情况。
- 14. 支持多种模式部署运行（gateway模式）：与cluster类似，区别是将认证客户端放到了网关，使BizApp专注于提供业务服务，实现了网关、业务应用、认证中心的完全分离， 即，gateway+IAM client、BizApp、IAM server，非常适合完全微服务架构的认证中心部署（量身定制）
- 15. OIDC 支持
- 16. 对比 [keycloak](https://github.com/keycloak/keycloak-quickstarts) [请参考这里](VS_KEYCLOAK.md)

## 2. Server Deploy

- Docker 部署

TODO

- 主机部署

TODO

## 3. Client Integration

### 3.1 Gateway Mode (Recommends)

此模式的架构哲学是 sidecar，以业务层和通用层尽量分离为思想，在传统企业应用中，由于业务应用代码与各认证、中间件等 SDK 是以依赖的形式强耦合，导致 SDK 升级难、易出错等，严重影响业务应用的快速迭代和服务的稳定性，而使用 gateway 将认证等通用逻辑分离，让其只专注于业务逻辑，极大降低大规模微服务部署出错概率，同时各组件由不同团队专人维护有助于稳定性及快速迭代能力极大提升，让企业收益最大化。

TODO

### 3.1 Spring Boot SDK Mode

- 3.1，PC 集成(前后端分离)
- 3.2，[安卓端接入（全局认证拦截器）](iam-client-example/src/main/java/com/wl4g/iam/example/android/AndroidIamUserCoordinator.java)
- 3.3，微信公众号集成，
- 3.4，服务端所有支持的yml配置(以及默认值):

```xml
<dependency>
    <groupId>com.wl4g</groupId>
    <artifactId>iam-client-springboot</artifactId>
    <version>${latest}</version>
</dependency>
```

## 4. Private Fast-Cas theory

TODO

## 5. Operation Monitoring and Observability

- [opentelemetry-java-instrumentation](https://github.com/wl4g-collect/opentelemetry-java-instrumentation)

- [opentelemetry-autoconfigure-jaeger](https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/autoconfigure/README.md#jaeger-exporter)

- ADD opentelemetry instrumentation(javaagent) Example

```bash
export OTEL_TRACES_EXPORTER=jaeger
export OTEL_EXPORTER_JAEGER_ENDPOINT=http://localhost:14250
export OTEL_EXPORTER_JAEGER_TIMEOUT=10000
export OTEL_METRICS_EXPORTER=prometheus
export OTEL_EXPORTER_PROMETHEUS_HOST=localhost
export OTEL_EXPORTER_PROMETHEUS_PORT=9090
java -javaagent:/opt/apps/some-javaagent/opentelemetry/opentelemetry-javaagent.jar -jar iam-web-{version}-bin.jar
```

## 6. Developer's Guide

- Maven mirrors Settings

```xml
mv $HOME/.m2/settings.xml $HOME/.m2/settings_bak.xml

cat <<-'EOF' >$HOME/.m2/settings.xml
<mirrors>
    <mirror>
        <id>alimaven-public</id>
        <mirrorOf>public</mirrorOf>
        <name>alimaven</name>
        <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
    <mirror>
        <id>alimaven-central</id>
        <mirrorOf>central</mirrorOf>
        <name>alimaven</name>
        <url>https://maven.aliyun.com/repository/central</url>
    </mirror>
    <mirror>
        <id>alimaven-grails-core</id>
        <mirrorOf>grails-core</mirrorOf>
        <name>aliyun maven</name>
        <url>https://maven.aliyun.com/repository/grails-core</url>
    </mirror>
    <mirror>
        <id>alimaven-google</id>
        <mirrorOf>google</mirrorOf>
        <name>aliyun maven</name>
        <url>https://maven.aliyun.com/repository/google</url>
    </mirror>
    <mirror>
        <id>alimaven-spring</id>
        <mirrorOf>spring</mirrorOf>
        <name>aliyun maven</name>
        <url>https://maven.aliyun.com/repository/spring</url>
    </mirror>
  </mirrors>
EOF
```

- Compiling

```bash
cd iam

# Build as a generic release package (directory structure).
mvn -U clean install -DskipTests -T 2C -P build:tar

# Build as spring boot single executable jar.
mvn -U clean install -DskipTests -T 2C -P build:springjar

# Build as a docker image based on the tar distribution.
mvn -U clean install -DskipTests -T 2C -P build:docker:tar

# Build an ELF native executable based on the graalvm native image.
mvn -U clean install -DskipTests -T 2C -P build:native
```

### 7.1 Integrate the server with SDK for custom development

- 5.1，独立运行模式，使用iam的数据库表，适用于新系统集成;
- 5.2，依赖嵌入模式，使用外部自定义数据库表，适用于旧系统改造集成;

TODO

## Development

于2018年初，我在github上创建 IAM 项目仓库并提交第一个commit，随之进行系统结构设计，UI选型，交互设计……
至今，IAM/SSO 已接入某物联网平台的生产环境，稳定运行1year+，接入场景如电商业务，O2O业务和核心中间件配置动态化等。
欢迎大家的关注和使用，IAM/SSO也将拥抱变化，持续发展。

## Contributing

欢迎参与项目贡献！比如提交PR修复一个bug，或者新建 [Issue](https://github.com/wl4g/iam/issues/) 讨论新特性或者变更。

## Copyright and License

This product is open source and free, and will continue to provide free community technical support. Individual or enterprise users are free to access and use.

- Licensed under the Apache License v2.
- Copyright (c) 2018-present, wanglsir.

产品开源免费，并且将持续提供免费的社区技术支持。个人或企业内部可自由的接入和使用。

## Donate

无论金额多少都足够表达您这份心意，非常感谢 ：）      [前往捐赠]()

## Stargazers over time

[![Stargazers over time](https://starchart.cc/wl4g/iam.svg)](https://starchart.cc/wl4g/iam)
