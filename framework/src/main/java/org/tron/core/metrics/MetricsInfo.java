package org.un.core.metrics;

import org.un.core.metrics.blockchain.BlockChainInfo;
import org.un.core.metrics.net.NetInfo;
import org.un.core.metrics.node.NodeInfo;

public class MetricsInfo {
  private long interval;

  private NodeInfo node;

  private BlockChainInfo blockchain;

  private NetInfo net;

  public long getInterval() {
    return interval;
  }

  public void setInterval(long interval) {
    this.interval = interval;
  }

  public NodeInfo getNode() {
    return this.node;
  }

  public void setNode(NodeInfo node) {
    this.node = node;
  }

  public BlockChainInfo getBlockchain() {
    return this.blockchain;
  }

  public void setBlockchain(BlockChainInfo blockChain) {
    this.blockchain = blockChain;
  }
  
  public NetInfo getNet() {
    return net;
  }

  public void setNet(NetInfo net) {
    this.net = net;
  }
}
