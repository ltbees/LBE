package org.un.core.net.messagehandler;

import static org.un.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;

import com.google.common.collect.Lists;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.un.common.overlay.discover.node.statistics.MessageCount;
import org.un.common.overlay.message.Message;
import org.un.common.utils.Sha256Hash;
import org.un.core.capsule.BlockCapsule.BlockId;
import org.un.core.config.Parameter.NetConstants;
import org.un.core.exception.P2pException;
import org.un.core.exception.P2pException.TypeEnum;
import org.un.core.net.LbeNetDelegate;
import org.un.core.net.message.BlockMessage;
import org.un.core.net.message.FetchInvDataMessage;
import org.un.core.net.message.MessageTypes;
import org.un.core.net.message.TransactionMessage;
import org.un.core.net.message.TransactionsMessage;
import org.un.core.net.message.LbeMessage;
import org.un.core.net.peer.Item;
import org.un.core.net.peer.PeerConnection;
import org.un.core.net.service.AdvService;
import org.un.core.net.service.SyncService;
import org.un.protos.Protocol.Inventory.InventoryType;
import org.un.protos.Protocol.ReasonCode;
import org.un.protos.Protocol.Transaction;

@Slf4j(topic = "net")
@Component
public class FetchInvDataMsgHandler implements LbeMsgHandler {

  private static final int MAX_SIZE = 1_000_000;
  @Autowired
  private LbeNetDelegate unNetDelegate;
  @Autowired
  private SyncService syncService;
  @Autowired
  private AdvService advService;

  @Override
  public void processMessage(PeerConnection peer, LbeMessage msg) throws P2pException {

    FetchInvDataMessage fetchInvDataMsg = (FetchInvDataMessage) msg;

    check(peer, fetchInvDataMsg);

    InventoryType type = fetchInvDataMsg.getInventoryType();
    List<Transaction> transactions = Lists.newArrayList();

    int size = 0;

    for (Sha256Hash hash : fetchInvDataMsg.getHashList()) {
      Item item = new Item(hash, type);
      Message message = advService.getMessage(item);
      if (message == null) {
        try {
          message = unNetDelegate.getData(hash, type);
        } catch (Exception e) {
          logger.error("Fetch item {} failed. reason: {}", item, hash, e.getMessage());
          peer.disconnect(ReasonCode.FETCH_FAIL);
          return;
        }
      }

      if (type == InventoryType.BLOCK) {
        BlockId blockId = ((BlockMessage) message).getBlockCapsule().getBlockId();
        if (peer.getBlockBothHave().getNum() < blockId.getNum()) {
          peer.setBlockBothHave(blockId);
        }
        peer.sendMessage(message);
      } else {
        transactions.add(((TransactionMessage) message).getTransactionCapsule().getInstance());
        size += ((TransactionMessage) message).getTransactionCapsule().getInstance()
            .getSerializedSize();
        if (size > MAX_SIZE) {
          peer.sendMessage(new TransactionsMessage(transactions));
          transactions = Lists.newArrayList();
          size = 0;
        }
      }
    }
    if (!transactions.isEmpty()) {
      peer.sendMessage(new TransactionsMessage(transactions));
    }
  }

  private void check(PeerConnection peer, FetchInvDataMessage fetchInvDataMsg) throws P2pException {
    MessageTypes type = fetchInvDataMsg.getInvMessageType();

    if (type == MessageTypes.LBE) {
      for (Sha256Hash hash : fetchInvDataMsg.getHashList()) {
        if (peer.getAdvInvSpread().getIfPresent(new Item(hash, InventoryType.LBE)) == null) {
          throw new P2pException(TypeEnum.BAD_MESSAGE, "not spread inv: {}" + hash);
        }
      }
      int fetchCount = peer.getNodeStatistics().messageStatistics.unInLbeFetchInvDataElement
          .getCount(10);
      int maxCount = advService.getLbeCount().getCount(60);
      if (fetchCount > maxCount) {
        logger.error("maxCount: " + maxCount + ", fetchCount: " + fetchCount);
        //        throw new P2pException(TypeEnum.BAD_MESSAGE,
        //            "maxCount: " + maxCount + ", fetchCount: " + fetchCount);
      }
    } else {
      boolean isAdv = true;
      for (Sha256Hash hash : fetchInvDataMsg.getHashList()) {
        if (peer.getAdvInvSpread().getIfPresent(new Item(hash, InventoryType.BLOCK)) == null) {
          isAdv = false;
          break;
        }
      }
      if (isAdv) {
        MessageCount unOutAdvBlock = peer.getNodeStatistics().messageStatistics.unOutAdvBlock;
        unOutAdvBlock.add(fetchInvDataMsg.getHashList().size());
        int outBlockCountIn1min = unOutAdvBlock.getCount(60);
        int producedBlockIn2min = 120_000 / BLOCK_PRODUCED_INTERVAL;
        if (outBlockCountIn1min > producedBlockIn2min) {
          logger.error("producedBlockIn2min: " + producedBlockIn2min + ", outBlockCountIn1min: "
              + outBlockCountIn1min);
          //throw new P2pException(TypeEnum.BAD_MESSAGE, "producedBlockIn2min: "
          // + producedBlockIn2min
          //  + ", outBlockCountIn1min: " + outBlockCountIn1min);
        }
      } else {
        if (!peer.isNeedSyncFromUs()) {
          throw new P2pException(TypeEnum.BAD_MESSAGE, "no need sync");
        }
        for (Sha256Hash hash : fetchInvDataMsg.getHashList()) {
          long blockNum = new BlockId(hash).getNum();
          long minBlockNum =
              peer.getLastSyncBlockId().getNum() - 2 * NetConstants.SYNC_FETCH_BATCH_NUM;
          if (blockNum < minBlockNum) {
            throw new P2pException(TypeEnum.BAD_MESSAGE,
                "minBlockNum: " + minBlockNum + ", blockNum: " + blockNum);
          }
          if (peer.getSyncBlockIdCache().getIfPresent(hash) != null) {
            throw new P2pException(TypeEnum.BAD_MESSAGE,
                new BlockId(hash).getString() + " is exist");
          }
          peer.getSyncBlockIdCache().put(hash, System.currentTimeMillis());
        }
      }
    }
  }

}
