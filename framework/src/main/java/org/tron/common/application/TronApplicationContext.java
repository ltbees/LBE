package org.un.common.application;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.un.common.overlay.discover.DiscoverServer;
import org.un.common.overlay.discover.node.NodeManager;
import org.un.common.overlay.server.ChannelManager;
import org.un.core.db.Manager;

public class LbeApplicationContext extends AnnotationConfigApplicationContext {

  public LbeApplicationContext() {
  }

  public LbeApplicationContext(DefaultListableBeanFactory beanFactory) {
    super(beanFactory);
  }

  public LbeApplicationContext(Class<?>... annotatedClasses) {
    super(annotatedClasses);
  }

  public LbeApplicationContext(String... basePackages) {
    super(basePackages);
  }

  @Override
  public void destroy() {

    Application appT = ApplicationFactory.create(this);
    appT.shutdownServices();
    appT.shutdown();

    DiscoverServer discoverServer = getBean(DiscoverServer.class);
    discoverServer.close();
    ChannelManager channelManager = getBean(ChannelManager.class);
    channelManager.close();
    NodeManager nodeManager = getBean(NodeManager.class);
    nodeManager.close();

    Manager dbManager = getBean(Manager.class);
    dbManager.stopRePushThread();
    dbManager.stopRePushTriggerThread();
    super.destroy();
  }
}
