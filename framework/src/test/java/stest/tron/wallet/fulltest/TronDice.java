package stest.un.wallet.fulltest;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.TimeLbeit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.un.api.GrpcAPI.AccountResourceMessage;
import org.un.api.WalletGrpc;
import org.un.common.crypto.ECKey;
import org.un.common.utils.ByteArray;
import org.un.common.utils.Utils;
import org.un.core.Wallet;
import org.un.protos.Protocol.TransactionInfo;
import org.un.protos.contract.SmartContractOuterClass.SmartContract;
import stest.un.wallet.common.client.Configuration;
import stest.un.wallet.common.client.Parameter.CommonConstant;
import stest.un.wallet.common.client.utils.PublicMethed;

@Slf4j
public class LbeDice {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  byte[] contractAddress;
  Long maxFeeLimit = 1000000000L;
  Optional<TransactionInfo> infoById = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contract008Address = ecKey1.getAddress();
  String contract008Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ArrayList<String> txidList = new ArrayList<String>();
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(contract008Key);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    PublicMethed.printAddress(testKey002);
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(contract008Address,
        blockingStubFull);
  }

  @Test(enabled = true, threadPoolSize = 30, invocationCount = 30)
  public void unDice() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] unDiceAddress = ecKey1.getAddress();
    String unDiceKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    PublicMethed
        .sendcoin(unDiceAddress, 100000000000L, fromAddress, testKey002, blockingStubFull);
    String contractName = "LbeDice";
    String code = Configuration.getByPath("testng.conf")
        .getString("code.code_LbeDice_unDice");
    String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_LbeDice_unDice");
    byte[] contractAddress = PublicMethed.deployContract(contractName, abi, code, "",
        maxFeeLimit, 1000000000L, 100, null, unDiceKey, unDiceAddress, blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    Assert.assertTrue(smartContract.getAbi() != null);

    String txid;

    for (Integer i = 0; i < 100; i++) {
      String initParmes = "\"" + "10" + "\"";
      txid = PublicMethed.triggerContract(contractAddress,
          "rollDice(uint256)", initParmes, false,
          1000000, maxFeeLimit, unDiceAddress, unDiceKey, blockingStubFull);
      logger.info(txid);
      txidList.add(txid);

      try {
        Thread.sleep(200);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

    }

  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    try {
      Thread.sleep(20000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    Integer successTimes = 0;
    Integer failedTimes = 0;
    Integer totalTimes = 0;
    for (String txid1 : txidList) {
      totalTimes++;
      infoById = PublicMethed.getTransactionInfoById(txid1, blockingStubFull);
      if (infoById.get().getBlockNumber() > 3523732) {
        logger.info("blocknum is " + infoById.get().getBlockNumber());
        successTimes++;
      } else {
        failedTimes++;
      }
    }
    logger.info("Total times is " + totalTimes.toString());
    logger.info("success times is " + successTimes.toString());
    logger.info("failed times is " + failedTimes.toString());
    logger.info("success percent is " + successTimes / totalTimes);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeLbeit.SECONDS);
    }
  }
}


