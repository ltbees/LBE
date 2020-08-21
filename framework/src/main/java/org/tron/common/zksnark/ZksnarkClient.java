package org.un.common.zksnark;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannelBuilder;
import org.un.api.LbeZksnarkGrpc;
import org.un.api.ZksnarkGrpcAPI.ZksnarkRequest;
import org.un.api.ZksnarkGrpcAPI.ZksnarkResponse.Code;
import org.un.core.capsule.TransactionCapsule;
import org.un.protos.Protocol.Transaction;

public class ZksnarkClient {

  public static final ZksnarkClient instance = new ZksnarkClient();

  private LbeZksnarkGrpc.LbeZksnarkBlockingStub blockingStub;

  public ZksnarkClient() {
    blockingStub = LbeZksnarkGrpc.newBlockingStub(ManagedChannelBuilder
        .forTarget("127.0.0.1:60051")
        .usePlaintext()
        .build());
  }

  public static ZksnarkClient getInstance() {
    return instance;
  }

  public boolean checkZksnarkProof(Transaction transaction, byte[] sighash, long valueBalance) {
    String txId = new TransactionCapsule(transaction).getTransactionId().toString();
    ZksnarkRequest request = ZksnarkRequest.newBuilder()
        .setTransaction(transaction)
        .setTxId(txId)
        .setSighash(ByteString.copyFrom(sighash))
        .setValueBalance(valueBalance)
        .build();
    return blockingStub.checkZksnarkProof(request).getCode() == Code.SUCCESS;
  }
}
