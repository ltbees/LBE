package stest.un.wallet.other;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Optional;
import java.util.concurrent.TimeLbeit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.un.api.WalletGrpc;
import org.un.api.WalletSolidityGrpc;
import org.un.common.crypto.ECKey;
import org.un.common.utils.ByteArray;
import org.un.common.utils.Utils;
import org.un.core.Wallet;
import org.un.protos.Protocol.Account;
import org.un.protos.Protocol.TransactionInfo;
import org.un.protos.contract.SmartContractOuterClass.SmartContract;
import stest.un.wallet.common.client.Configuration;
import stest.un.wallet.common.client.Parameter.CommonConstant;
import stest.un.wallet.common.client.WalletClient;
import stest.un.wallet.common.client.utils.Base58;
import stest.un.wallet.common.client.utils.PublicMethed;

@Slf4j
public class deployMainGateway {


  private final String testDepositLbe = "324a2052e491e99026442d81df4d2777292840c1b3949e20696c49096"
      + "c6bacb7";
  private final byte[] testDepositAddress = PublicMethed.getFinalAddress(testDepositLbe);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] depositAddress = ecKey1.getAddress();
  String testKeyFordeposit = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);

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
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = true, description = "deploy Main Chain Gateway")
  public void test1DepositTrc20001() {

    PublicMethed.printAddress(testKeyFordeposit);

    Assert.assertTrue(PublicMethed
        .sendcoin(depositAddress, 1000_000_000L, testDepositAddress, testDepositLbe,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account accountOralce = PublicMethed.queryAccount(depositAddress, blockingStubFull);
    long OralceBalance = accountOralce.getBalance();
    logger.info("OralceBalance: " + OralceBalance);

    String contractName = "gateWayContract";
    String code = Configuration.getByPath("testng.conf")
        .getString("code.code_MainGateway");
    String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_MainGateway");
    String parame = "\"" + Base58.encode58Check(testDepositAddress) + "\"";

    String deployTxid = PublicMethed
        .deployContractWithConstantParame(contractName, abi, code, "constructor(address)",
            parame, "",
            maxFeeLimit,
            0L, 100, null, testKeyFordeposit, depositAddress,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(deployTxid, blockingStubFull);
    byte[] mainChainGateway = infoById.get().getContractAddress().toByteArray();
    String mainChainGatewayAddress = WalletClient.encode58Check(mainChainGateway);
    Assert.assertEquals(0, infoById.get().getResultValue());
    Assert.assertNotNull(mainChainGateway);

    SmartContract smartContract = PublicMethed.getContract(mainChainGateway,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    String outputPath = "./src/test/resources/mainChainGatewayAddress";
    try {
      File mainChainFile = new File(outputPath);
      Boolean cun = mainChainFile.createNewFile();
      FileWriter writer = new FileWriter(mainChainFile);
      BufferedWriter out = new BufferedWriter(writer);
      out.write(mainChainGatewayAddress);

      out.close();
      writer.close();
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeLbeit.SECONDS);
    }
  }

}
