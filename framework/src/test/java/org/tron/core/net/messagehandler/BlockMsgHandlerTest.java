package org.un.core.net.messagehandler;

import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.testng.collections.Lists;
import org.un.common.application.LbeApplicationContext;
import org.un.common.utils.Sha256Hash;
import org.un.core.Constant;
import org.un.core.capsule.BlockCapsule;
import org.un.core.config.DefaultConfig;
import org.un.core.config.args.Args;
import org.un.core.exception.P2pException;
import org.un.core.net.message.BlockMessage;
import org.un.core.net.peer.Item;
import org.un.core.net.peer.PeerConnection;
import org.un.protos.Protocol.Inventory.InventoryType;
import org.un.protos.Protocol.Transaction;

public class BlockMsgHandlerTest {

  protected LbeApplicationContext context;
  private BlockMsgHandler handler;
  private PeerConnection peer;

  /**
   * init context.
   */
  @Before
  public void init() {
    Args.setParam(new String[]{"--output-directory", "output-directory", "--debug"},
        Constant.TEST_CONF);
    context = new LbeApplicationContext(DefaultConfig.class);
    handler = context.getBean(BlockMsgHandler.class);
    peer = context.getBean(PeerConnection.class);
  }

  @Test
  public void testProcessMessage() {
    BlockCapsule blockCapsule;
    BlockMessage msg;
    try {
      blockCapsule = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
          System.currentTimeMillis(), Sha256Hash.ZERO_HASH.getByteString());
      msg = new BlockMessage(blockCapsule);
      handler.processMessage(peer, msg);
    } catch (P2pException e) {
      Assert.assertTrue(e.getMessage().equals("no request"));
    }

    try {
      List<Transaction> transactionList = Lists.newArrayList();
      for (int i = 0; i < 1100000; i++) {
        transactionList.add(Transaction.newBuilder().build());
      }
      blockCapsule = new BlockCapsule(1, Sha256Hash.ZERO_HASH.getByteString(),
          System.currentTimeMillis() + 10000, transactionList);
      msg = new BlockMessage(blockCapsule);
      System.out.println("len = " + blockCapsule.getInstance().getSerializedSize());
      peer.getAdvInvRequest()
          .put(new Item(msg.getBlockId(), InventoryType.BLOCK), System.currentTimeMillis());
      handler.processMessage(peer, msg);
    } catch (P2pException e) {
      //System.out.println(e);
      Assert.assertTrue(e.getMessage().equals("block size over limit"));
    }

    try {
      blockCapsule = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
          System.currentTimeMillis() + 10000, Sha256Hash.ZERO_HASH.getByteString());
      msg = new BlockMessage(blockCapsule);
      peer.getAdvInvRequest()
          .put(new Item(msg.getBlockId(), InventoryType.BLOCK), System.currentTimeMillis());
      handler.processMessage(peer, msg);
    } catch (P2pException e) {
      //System.out.println(e);
      Assert.assertTrue(e.getMessage().equals("block time error"));
    }

    try {
      blockCapsule = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
          System.currentTimeMillis() + 1000, Sha256Hash.ZERO_HASH.getByteString());
      msg = new BlockMessage(blockCapsule);
      peer.getSyncBlockRequested()
          .put(msg.getBlockId(), System.currentTimeMillis());
      handler.processMessage(peer, msg);
    } catch (P2pException e) {
      //System.out.println(e);
    }

    try {
      blockCapsule = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
          System.currentTimeMillis() + 1000, Sha256Hash.ZERO_HASH.getByteString());
      msg = new BlockMessage(blockCapsule);
      peer.getAdvInvRequest()
          .put(new Item(msg.getBlockId(), InventoryType.BLOCK), System.currentTimeMillis());
      handler.processMessage(peer, msg);
    } catch (NullPointerException | P2pException e) {
      System.out.println(e);
    }
  }

  @After
  public void destroy() {
    Args.clearParam();
    context.destroy();
  }
}
