package org.un.consensus.base;

import org.un.consensus.pbft.message.PbftBaseMessage;
import org.un.core.capsule.BlockCapsule;

public interface PbftInterface {

  boolean isSyncing();

  void forwardMessage(PbftBaseMessage message);

  BlockCapsule getBlock(long blockNum) throws Exception;

}