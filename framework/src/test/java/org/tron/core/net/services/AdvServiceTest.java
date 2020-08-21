package org.un.core.net.services;

import com.google.common.collect.Lists;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.un.common.application.LbeApplicationContext;
import org.un.common.overlay.server.SyncPool;
import org.un.common.parameter.CommonParameter;
import org.un.common.utils.ReflectUtils;
import org.un.common.utils.Sha256Hash;
import org.un.core.Constant;
import org.un.core.capsule.BlockCapsule;
import org.un.core.config.DefaultConfig;
import org.un.core.config.args.Args;
import org.un.core.net.message.BlockMessage;
import org.un.core.net.message.TransactionMessage;
import org.un.core.net.peer.Item;
import org.un.core.net.peer.PeerConnection;
import org.un.core.net.service.AdvService;
import org.un.protos.Protocol;
import org.un.protos.Protocol.Inventory.InventoryType;

//@Ignore
public class AdvServiceTest {

  protected LbeApplicationContext context;
  private AdvService service;
  private PeerConnection peer;
  private SyncPool syncPool;

  /**
   * init context.
   */
  @Before
  public void init() {
    Args.setParam(new String[]{"--output-directory", "output-directory", "--debug"},
        Constant.TEST_CONF);
    context = new LbeApplicationContext(DefaultConfig.class);
    service = context.getBean(AdvService.class);
  }

  /**
   * destroy.
   */
  @After
  public void destroy() {
    Args.clearParam();
    context.destroy();
  }

  @Test
  public void test() {
    testAddInv();
    testBroadcast();
    testFastSend();
    testLbeBroadcast();
  }

  private void testAddInv() {
    boolean flag;
    Item itemLbe = new Item(Sha256Hash.ZERO_HASH, InventoryType.LBE);
    flag = service.addInv(itemLbe);
    Assert.assertTrue(flag);
    flag = service.addInv(itemLbe);
    Assert.assertFalse(flag);

    Item itemBlock = new Item(Sha256Hash.ZERO_HASH, InventoryType.BLOCK);
    flag = service.addInv(itemBlock);
    Assert.assertTrue(flag);
    flag = service.addInv(itemBlock);
    Assert.assertFalse(flag);

    service.addInvToCache(itemBlock);
    flag = service.addInv(itemBlock);
    Assert.assertFalse(flag);
  }

  private void testBroadcast() {

    try {
      peer = context.getBean(PeerConnection.class);
      syncPool = context.getBean(SyncPool.class);

      List<PeerConnection> peers = Lists.newArrayList();
      peers.add(peer);
      ReflectUtils.setFieldValue(syncPool, "activePeers", peers);
      BlockCapsule blockCapsule = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
          System.currentTimeMillis(), Sha256Hash.ZERO_HASH.getByteString());
      BlockMessage msg = new BlockMessage(blockCapsule);
      service.broadcast(msg);
      Item item = new Item(blockCapsule.getBlockId(), InventoryType.BLOCK);
      Assert.assertNotNull(service.getMessage(item));

      peer.close();
      syncPool.close();
    } catch (NullPointerException e) {
      System.out.println(e);
    }
  }

  private void testFastSend() {

    try {
      peer = context.getBean(PeerConnection.class);
      syncPool = context.getBean(SyncPool.class);

      List<PeerConnection> peers = Lists.newArrayList();
      peers.add(peer);
      ReflectUtils.setFieldValue(syncPool, "activePeers", peers);
      BlockCapsule blockCapsule = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
          System.currentTimeMillis(), Sha256Hash.ZERO_HASH.getByteString());
      BlockMessage msg = new BlockMessage(blockCapsule);
      service.fastForward(msg);
      Item item = new Item(blockCapsule.getBlockId(), InventoryType.BLOCK);
      //Assert.assertNull(service.getMessage(item));

      peer.getAdvInvRequest().put(item, System.currentTimeMillis());
      service.onDisconnect(peer);

      peer.close();
      syncPool.close();
    } catch (NullPointerException e) {
      System.out.println(e);
    }
  }

  private void testLbeBroadcast() {
    Protocol.Transaction trx = Protocol.Transaction.newBuilder().build();
    CommonParameter.getInstance().setValidContractProtoThreadNum(1);
    TransactionMessage msg = new TransactionMessage(trx);
    service.broadcast(msg);
    Item item = new Item(msg.getMessageId(), InventoryType.LBE);
    Assert.assertNotNull(service.getMessage(item));
  }

}
