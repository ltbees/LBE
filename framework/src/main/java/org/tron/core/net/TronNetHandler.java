package org.un.core.net;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.un.common.overlay.server.Channel;
import org.un.common.overlay.server.MessageQueue;
import org.un.core.net.message.LbeMessage;
import org.un.core.net.peer.PeerConnection;

@Component
@Scope("prototype")
public class LbeNetHandler extends SimpleChannelInboundHandler<LbeMessage> {

  protected PeerConnection peer;

  private MessageQueue msgQueue;

  @Autowired
  private LbeNetService unNetService;

  @Override
  public void channelRead0(final ChannelHandlerContext ctx, LbeMessage msg) throws Exception {
    msgQueue.receivedMessage(msg);
    unNetService.onMessage(peer, msg);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    peer.processException(cause);
  }

  public void setMsgQueue(MessageQueue msgQueue) {
    this.msgQueue = msgQueue;
  }

  public void setChannel(Channel channel) {
    this.peer = (PeerConnection) channel;
  }

}