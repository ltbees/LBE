package org.un.consensus.base;

import org.un.consensus.base.Param.Miner;
import org.un.core.capsule.BlockCapsule;

public interface BlockHandle {

  State getState();

  Object getLock();

  BlockCapsule produce(Miner miner, long blockTime, long timeout);

}