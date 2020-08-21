package org.un.common.net.udp.message.discover;

import static org.un.common.net.udp.message.UdpMessageTypeEnum.DISCOVER_FIND_NODE;

import com.google.protobuf.ByteString;
import org.un.common.net.udp.message.Message;
import org.un.common.overlay.discover.node.Node;
import org.un.common.utils.ByteArray;
import org.un.protos.Discover;
import org.un.protos.Discover.Endpoint;
import org.un.protos.Discover.FindNeighbours;

public class FindNodeMessage extends Message {

  private Discover.FindNeighbours findNeighbours;

  public FindNodeMessage(byte[] data) throws Exception {
    super(DISCOVER_FIND_NODE, data);
    this.findNeighbours = Discover.FindNeighbours.parseFrom(data);
  }

  public FindNodeMessage(Node from, byte[] targetId) {
    super(DISCOVER_FIND_NODE, null);
    Endpoint fromEndpoint = Endpoint.newBuilder()
        .setAddress(ByteString.copyFrom(ByteArray.fromString(from.getHost())))
        .setPort(from.getPort())
        .setNodeId(ByteString.copyFrom(from.getId()))
        .build();
    this.findNeighbours = FindNeighbours.newBuilder()
        .setFrom(fromEndpoint)
        .setTargetId(ByteString.copyFrom(targetId))
        .setTimestamp(System.currentTimeMillis())
        .build();
    this.data = this.findNeighbours.toByteArray();
  }

  public byte[] getTargetId() {
    return this.findNeighbours.getTargetId().toByteArray();
  }

  @Override
  public long getTimestamp() {
    return this.findNeighbours.getTimestamp();
  }

  @Override
  public Node getFrom() {
    return Message.getNode(findNeighbours.getFrom());
  }

  @Override
  public String toString() {
    return "[findNeighbours: " + findNeighbours;
  }
}
