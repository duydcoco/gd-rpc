package io.goudai.rpc.bootstarp;

import io.goudai.net.Connector;
import io.goudai.net.Reactor;
import io.goudai.net.session.factory.DefaultSessionFactory;
import io.goudai.rpc.exception.RpcException;
import io.goudai.rpc.invoker.RequestSessionFactory;
import io.goudai.rpc.invoker.SingleInvoker;
import io.goudai.rpc.listener.SessionManager;
import io.goudai.rpc.proxy.JavaProxyServiceFactory;
import io.goudai.rpc.proxy.ProxyServiceFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * Created by vip on 2016/1/28.
 */
@Slf4j
public class SingleBootstrap implements Bootstrap {
    private final DefaultSessionFactory sessionFactory = new DefaultSessionFactory();
    private Connector connector;
    private Reactor reactor;
    private String serverIp;
    private int serverPort;
    private ProxyServiceFactory proxyServiceFactory;

    public SingleBootstrap(String serverIp, int serverPort, int reactors) throws IOException {
        reactor = new Reactor(reactors, sessionFactory);
        connector = new Connector("goudai-rpc-connector-thread", reactor);
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    @Override
    public <T> T getService(Class<T> clazz) {
        return proxyServiceFactory.createServiceProxy(clazz);
    }

    @Override
    public void startup() {
        this.connector.startup();
        this.reactor.startup();
        proxyServiceFactory = new JavaProxyServiceFactory(new SingleInvoker(new RequestSessionFactory(serverIp, serverPort, connector, sessionFactory)));
    }

    @Override
    public void shutdown() {
        try {
            SessionManager.getInstance().close();
        } catch (Exception e) {
            throw new RpcException(e.getMessage(), e);
        }
        this.connector.shutdown();
        log.info("[{}] shutdown connector complete", this.connector);
        this.reactor.shutdown();
        log.info("[{}] shutdown reactorPool complete", this.reactor);
    }
}
