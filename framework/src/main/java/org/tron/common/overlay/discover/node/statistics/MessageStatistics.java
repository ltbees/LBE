package org.un.common.overlay.discover.node.statistics;

import lombok.extern.slf4j.Slf4j;
import org.un.common.net.udp.message.UdpMessageTypeEnum;
import org.un.common.overlay.message.Message;
import org.un.core.net.message.FetchInvDataMessage;
import org.un.core.net.message.InventoryMessage;
import org.un.core.net.message.MessageTypes;
import org.un.core.net.message.TransactionsMessage;

@Slf4j
public class MessageStatistics {

  //udp discovery
  public final MessageCount discoverInPing = new MessageCount();
  public final MessageCount discoverOutPing = new MessageCount();
  public final MessageCount discoverInPong = new MessageCount();
  public final MessageCount discoverOutPong = new MessageCount();
  public final MessageCount discoverInFindNode = new MessageCount();
  public final MessageCount discoverOutFindNode = new MessageCount();
  public final MessageCount discoverInNeighbours = new MessageCount();
  public final MessageCount discoverOutNeighbours = new MessageCount();

  //tcp p2p
  public final MessageCount p2pInHello = new MessageCount();
  public final MessageCount p2pOutHello = new MessageCount();
  public final MessageCount p2pInPing = new MessageCount();
  public final MessageCount p2pOutPing = new MessageCount();
  public final MessageCount p2pInPong = new MessageCount();
  public final MessageCount p2pOutPong = new MessageCount();
  public final MessageCount p2pInDisconnect = new MessageCount();
  public final MessageCount p2pOutDisconnect = new MessageCount();

  //tcp un
  public final MessageCount unInMessage = new MessageCount();
  public final MessageCount unOutMessage = new MessageCount();

  public final MessageCount unInSyncBlockChain = new MessageCount();
  public final MessageCount unOutSyncBlockChain = new MessageCount();
  public final MessageCount unInBlockChainInventory = new MessageCount();
  public final MessageCount unOutBlockChainInventory = new MessageCount();

  public final MessageCount unInLbeInventory = new MessageCount();
  public final MessageCount unOutLbeInventory = new MessageCount();
  public final MessageCount unInLbeInventoryElement = new MessageCount();
  public final MessageCount unOutLbeInventoryElement = new MessageCount();

  public final MessageCount unInBlockInventory = new MessageCount();
  public final MessageCount unOutBlockInventory = new MessageCount();
  public final MessageCount unInBlockInventoryElement = new MessageCount();
  public final MessageCount unOutBlockInventoryElement = new MessageCount();

  public final MessageCount unInLbeFetchInvData = new MessageCount();
  public final MessageCount unOutLbeFetchInvData = new MessageCount();
  public final MessageCount unInLbeFetchInvDataElement = new MessageCount();
  public final MessageCount unOutLbeFetchInvDataElement = new MessageCount();

  public final MessageCount unInBlockFetchInvData = new MessageCount();
  public final MessageCount unOutBlockFetchInvData = new MessageCount();
  public final MessageCount unInBlockFetchInvDataElement = new MessageCount();
  public final MessageCount unOutBlockFetchInvDataElement = new MessageCount();


  public final MessageCount unInLbe = new MessageCount();
  public final MessageCount unOutLbe = new MessageCount();
  public final MessageCount unInLbes = new MessageCount();
  public final MessageCount unOutLbes = new MessageCount();
  public final MessageCount unInBlock = new MessageCount();
  public final MessageCount unOutBlock = new MessageCount();
  public final MessageCount unOutAdvBlock = new MessageCount();

  public void addUdpInMessage(UdpMessageTypeEnum type) {
    addUdpMessage(type, true);
  }

  public void addUdpOutMessage(UdpMessageTypeEnum type) {
    addUdpMessage(type, false);
  }

  public void addTcpInMessage(Message msg) {
    addTcpMessage(msg, true);
  }

  public void addTcpOutMessage(Message msg) {
    addTcpMessage(msg, false);
  }

  private void addUdpMessage(UdpMessageTypeEnum type, boolean flag) {
    switch (type) {
      case DISCOVER_PING:
        if (flag) {
          discoverInPing.add();
        } else {
          discoverOutPing.add();
        }
        break;
      case DISCOVER_PONG:
        if (flag) {
          discoverInPong.add();
        } else {
          discoverOutPong.add();
        }
        break;
      case DISCOVER_FIND_NODE:
        if (flag) {
          discoverInFindNode.add();
        } else {
          discoverOutFindNode.add();
        }
        break;
      case DISCOVER_NEIGHBORS:
        if (flag) {
          discoverInNeighbours.add();
        } else {
          discoverOutNeighbours.add();
        }
        break;
      default:
        break;
    }
  }

  private void addTcpMessage(Message msg, boolean flag) {

    if (flag) {
      unInMessage.add();
    } else {
      unOutMessage.add();
    }

    switch (msg.getType()) {
      case P2P_HELLO:
        if (flag) {
          p2pInHello.add();
        } else {
          p2pOutHello.add();
        }
        break;
      case P2P_PING:
        if (flag) {
          p2pInPing.add();
        } else {
          p2pOutPing.add();
        }
        break;
      case P2P_PONG:
        if (flag) {
          p2pInPong.add();
        } else {
          p2pOutPong.add();
        }
        break;
      case P2P_DISCONNECT:
        if (flag) {
          p2pInDisconnect.add();
        } else {
          p2pOutDisconnect.add();
        }
        break;
      case SYNC_BLOCK_CHAIN:
        if (flag) {
          unInSyncBlockChain.add();
        } else {
          unOutSyncBlockChain.add();
        }
        break;
      case BLOCK_CHAIN_INVENTORY:
        if (flag) {
          unInBlockChainInventory.add();
        } else {
          unOutBlockChainInventory.add();
        }
        break;
      case INVENTORY:
        InventoryMessage inventoryMessage = (InventoryMessage) msg;
        int inventorySize = inventoryMessage.getInventory().getIdsCount();
        if (flag) {
          if (inventoryMessage.getInvMessageType() == MessageTypes.LBE) {
            unInLbeInventory.add();
            unInLbeInventoryElement.add(inventorySize);
          } else {
            unInBlockInventory.add();
            unInBlockInventoryElement.add(inventorySize);
          }
        } else {
          if (inventoryMessage.getInvMessageType() == MessageTypes.LBE) {
            unOutLbeInventory.add();
            unOutLbeInventoryElement.add(inventorySize);
          } else {
            unOutBlockInventory.add();
            unOutBlockInventoryElement.add(inventorySize);
          }
        }
        break;
      case FETCH_INV_DATA:
        FetchInvDataMessage fetchInvDataMessage = (FetchInvDataMessage) msg;
        int fetchSize = fetchInvDataMessage.getInventory().getIdsCount();
        if (flag) {
          if (fetchInvDataMessage.getInvMessageType() == MessageTypes.LBE) {
            unInLbeFetchInvData.add();
            unInLbeFetchInvDataElement.add(fetchSize);
          } else {
            unInBlockFetchInvData.add();
            unInBlockFetchInvDataElement.add(fetchSize);
          }
        } else {
          if (fetchInvDataMessage.getInvMessageType() == MessageTypes.LBE) {
            unOutLbeFetchInvData.add();
            unOutLbeFetchInvDataElement.add(fetchSize);
          } else {
            unOutBlockFetchInvData.add();
            unOutBlockFetchInvDataElement.add(fetchSize);
          }
        }
        break;
      case LBES:
        TransactionsMessage transactionsMessage = (TransactionsMessage) msg;
        if (flag) {
          unInLbes.add();
          unInLbe.add(transactionsMessage.getTransactions().getTransactionsCount());
        } else {
          unOutLbes.add();
          unOutLbe.add(transactionsMessage.getTransactions().getTransactionsCount());
        }
        break;
      case LBE:
        if (flag) {
          unInMessage.add();
        } else {
          unOutMessage.add();
        }
        break;
      case BLOCK:
        if (flag) {
          unInBlock.add();
        }
        unOutBlock.add();
        break;
      default:
        break;
    }
  }

}
