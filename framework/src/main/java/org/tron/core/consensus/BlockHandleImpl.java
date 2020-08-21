package org.un.core.consensus;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.un.common.backup.BackupManager;
import org.un.common.backup.BackupManager.BackupStatusEnum;
import org.un.consensus.Consensus;
import org.un.consensus.base.BlockHandle;
import org.un.consensus.base.Param.Miner;
import org.un.consensus.base.State;
import org.un.core.capsule.BlockCapsule;
import org.un.core.db.Manager;
import org.un.core.net.LbeNetService;
import org.un.core.net.message.BlockMessage;

@Slf4j(topic = "consensus")
@Component
public class BlockHandleImpl implements BlockHandle {

  @Autowired
  private Manager manager;

  @Autowired
  private BackupManager backupManager;

  @Autowired
  private LbeNetService unNetService;

  @Autowired
  private Consensus consensus;

  @Override
  public State getState() {
    if (!backupManager.getStatus().equals(BackupStatusEnum.MASTER)) {
      return State.BACKUP_IS_NOT_MASTER;
    }
    return State.OK;
  }

  public Object getLock() {
    return manager;
  }

  public BlockCapsule produce(Miner miner, long blockTime, long timeout) {
    BlockCapsule blockCapsule = manager.generateBlock(miner, blockTime, timeout);
    if (blockCapsule == null) {
      return null;
    }
    try {
      consensus.receiveBlock(blockCapsule);
      BlockMessage blockMessage = new BlockMessage(blockCapsule);
      unNetService.fastForward(blockMessage);
      manager.pushBlock(blockCapsule);
      unNetService.broadcast(blockMessage);
    } catch (Exception e) {
      logger.error("Handle block {} failed.", blockCapsule.getBlockId().getString(), e);
      return null;
    }
    return blockCapsule;
  }
}
