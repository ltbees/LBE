package org.un.core.net.messagehandler;

import org.un.core.exception.P2pException;
import org.un.core.net.message.LbeMessage;
import org.un.core.net.peer.PeerConnection;

public interface LbeMsgHandler {

  void processMessage(PeerConnection peer, LbeMessage msg) throws P2pException;

}
