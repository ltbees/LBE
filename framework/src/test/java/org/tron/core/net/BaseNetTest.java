package org.un.core.net;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.un.core.services.DelegationServiceTest;
import org.un.core.services.NodeInfoServiceTest;

@Slf4j
public class BaseNetTest extends BaseNet {

  @Test
  public void test() throws Exception {
    new NodeInfoServiceTest(context).test();
    new UdpTest(context).test();
    new TcpTest(context).test();
    new DelegationServiceTest(context).test();
  }
}
