package org.un.core.db;

import com.google.protobuf.ByteString;
import java.io.File;
import java.util.Random;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.un.common.application.Application;
import org.un.common.application.ApplicationFactory;
import org.un.common.application.LbeApplicationContext;
import org.un.common.crypto.ECKey;
import org.un.common.utils.ByteArray;
import org.un.common.utils.FileUtil;
import org.un.common.utils.Sha256Hash;
import org.un.core.ChainBaseManager;
import org.un.core.Constant;
import org.un.core.Wallet;
import org.un.core.capsule.AccountCapsule;
import org.un.core.capsule.BlockCapsule;
import org.un.core.capsule.TransactionCapsule;
import org.un.core.config.DefaultConfig;
import org.un.core.config.args.Args;
import org.un.core.exception.BadItemException;
import org.un.core.exception.ItemNotFoundException;
import org.un.protos.Protocol.AccountType;
import org.un.protos.Protocol.Transaction.Contract.ContractType;
import org.un.protos.contract.AccountContract.AccountCreateContract;
import org.un.protos.contract.BalanceContract.TransferContract;
import org.un.protos.contract.WitnessContract.VoteWitnessContract;
import org.un.protos.contract.WitnessContract.VoteWitnessContract.Vote;
import org.un.protos.contract.WitnessContract.WitnessCreateContract;

public class TransactionStoreTest {

  private static final byte[] key1 = TransactionStoreTest.randomBytes(21);
  private static final byte[] key2 = TransactionStoreTest.randomBytes(21);
  private static final String URL = "https://un.network";
  private static final String ACCOLBET_NAME = "ownerF";
  private static final String OWNER_ADDRESS =
      Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
  private static final String TO_ADDRESS =
      Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
  private static final long AMOLBET = 100;
  private static final String WITNESS_ADDRESS =
      Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
  private static String dbPath = "output_TransactionStore_test";
  private static String dbDirectory = "db_TransactionStore_test";
  private static String indexDirectory = "index_TransactionStore_test";
  private static TransactionStore transactionStore;
  private static LbeApplicationContext context;
  private static Application AppT;
  private static ChainBaseManager chainBaseManager;

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"--output-directory", dbPath, "--storage-db-directory",
        dbDirectory, "--storage-index-directory", indexDirectory, "-w"}, Constant.TEST_CONF);
    context = new LbeApplicationContext(DefaultConfig.class);
    AppT = ApplicationFactory.create(context);
    chainBaseManager = context.getBean(ChainBaseManager.class);
    transactionStore = chainBaseManager.getTransactionStore();
  }

  /**
   * release resources.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  /**
   * genarate random bytes.
   */
  public static byte[] randomBytes(int length) {
    // generate the random number
    byte[] result = new byte[length];
    new Random().nextBytes(result);
    result[0] = Wallet.getAddressPreFixByte();
    return result;
  }

  /**
   * get AccountCreateContract.
   */
  private AccountCreateContract getContract(String name, String address) {
    return AccountCreateContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
        .build();
  }

  /**
   * get TransferContract.
   */
  private TransferContract getContract(long count, String owneraddress, String toaddress) {
    return TransferContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(owneraddress)))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(toaddress)))
        .setAmount(count)
        .build();
  }

  /**
   * get WitnessCreateContract.
   */
  private WitnessCreateContract getWitnessContract(String address, String url) {
    return WitnessCreateContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
        .setUrl(ByteString.copyFrom(ByteArray.fromString(url)))
        .build();
  }

  /**
   * get VoteWitnessContract.
   */
  private VoteWitnessContract getVoteWitnessContract(String address, String voteaddress,
      Long value) {
    return
        VoteWitnessContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
            .addVotes(Vote.newBuilder()
                .setVoteAddress(ByteString.copyFrom(ByteArray.fromHexString(voteaddress)))
                .setVoteCount(value).build())
            .build();
  }

  @Test
  public void getTransactionTest() throws BadItemException, ItemNotFoundException {
    final BlockStore blockStore = chainBaseManager.getBlockStore();
    final TransactionStore trxStore = chainBaseManager.getTransactionStore();
    String key = "f31db24bfbd1a2ef19beddca0a0fa37632eded9ac666a05d3bd925f01dde1f62";

    BlockCapsule blockCapsule =
        new BlockCapsule(
            1,
            Sha256Hash.wrap(chainBaseManager.getGenesisBlockId().getByteString()),
            1,
            ByteString.copyFrom(
                ECKey.fromPrivate(
                    ByteArray.fromHexString(key)).getAddress()));

    // save in database with block number
    TransferContract tc =
        TransferContract.newBuilder()
            .setAmount(10)
            .setOwnerAddress(ByteString.copyFromUtf8("aaa"))
            .setToAddress(ByteString.copyFromUtf8("bbb"))
            .build();
    TransactionCapsule trx = new TransactionCapsule(tc, ContractType.TransferContract);
    blockCapsule.addTransaction(trx);
    trx.setBlockNum(blockCapsule.getNum());
    blockStore.put(blockCapsule.getBlockId().getBytes(), blockCapsule);
    trxStore.put(trx.getTransactionId().getBytes(), trx);
    Assert.assertEquals("Get transaction is error",
        trxStore.get(trx.getTransactionId().getBytes()).getInstance(), trx.getInstance());

    // no found in transaction store database
    tc =
        TransferContract.newBuilder()
            .setAmount(1000)
            .setOwnerAddress(ByteString.copyFromUtf8("aaa"))
            .setToAddress(ByteString.copyFromUtf8("bbb"))
            .build();
    trx = new TransactionCapsule(tc, ContractType.TransferContract);
    Assert.assertNull(trxStore.get(trx.getTransactionId().getBytes()));

    // no block number, directly save in database
    tc =
        TransferContract.newBuilder()
            .setAmount(10000)
            .setOwnerAddress(ByteString.copyFromUtf8("aaa"))
            .setToAddress(ByteString.copyFromUtf8("bbb"))
            .build();
    trx = new TransactionCapsule(tc, ContractType.TransferContract);
    trxStore.put(trx.getTransactionId().getBytes(), trx);
    Assert.assertEquals("Get transaction is error",
        trxStore.get(trx.getTransactionId().getBytes()).getInstance(), trx.getInstance());
  }

  /**
   * put and get CreateAccountTransaction.
   */
  @Test
  public void createAccountTransactionStoreTest() throws BadItemException {
    AccountCreateContract accountCreateContract = getContract(ACCOLBET_NAME,
        OWNER_ADDRESS);
    TransactionCapsule ret = new TransactionCapsule(accountCreateContract,
        chainBaseManager.getAccountStore());
    transactionStore.put(key1, ret);
    Assert.assertEquals("Store CreateAccountTransaction is error",
        transactionStore.get(key1).getInstance(),
        ret.getInstance());
    Assert.assertTrue(transactionStore.has(key1));
  }

  @Test
  public void getLbecheckedTransactionTest() {
    final BlockStore blockStore = chainBaseManager.getBlockStore();
    final TransactionStore trxStore = chainBaseManager.getTransactionStore();
    String key = "f31db24bfbd1a2ef19beddca0a0fa37632eded9ac666a05d3bd925f01dde1f62";

    BlockCapsule blockCapsule =
        new BlockCapsule(
            1,
            Sha256Hash.wrap(chainBaseManager.getGenesisBlockId().getByteString()),
            1,
            ByteString.copyFrom(
                ECKey.fromPrivate(
                    ByteArray.fromHexString(key)).getAddress()));

    // save in database with block number
    TransferContract tc =
        TransferContract.newBuilder()
            .setAmount(10)
            .setOwnerAddress(ByteString.copyFromUtf8("aaa"))
            .setToAddress(ByteString.copyFromUtf8("bbb"))
            .build();
    TransactionCapsule trx = new TransactionCapsule(tc, ContractType.TransferContract);
    blockCapsule.addTransaction(trx);
    trx.setBlockNum(blockCapsule.getNum());
    blockStore.put(blockCapsule.getBlockId().getBytes(), blockCapsule);
    trxStore.put(trx.getTransactionId().getBytes(), trx);
    Assert.assertEquals("Get transaction is error",
        trxStore.getLbechecked(trx.getTransactionId().getBytes()).getInstance(), trx.getInstance());

    // no found in transaction store database
    tc =
        TransferContract.newBuilder()
            .setAmount(1000)
            .setOwnerAddress(ByteString.copyFromUtf8("aaa"))
            .setToAddress(ByteString.copyFromUtf8("bbb"))
            .build();
    trx = new TransactionCapsule(tc, ContractType.TransferContract);
    Assert.assertNull(trxStore.getLbechecked(trx.getTransactionId().getBytes()));

    // no block number, directly save in database
    tc =
        TransferContract.newBuilder()
            .setAmount(10000)
            .setOwnerAddress(ByteString.copyFromUtf8("aaa"))
            .setToAddress(ByteString.copyFromUtf8("bbb"))
            .build();
    trx = new TransactionCapsule(tc, ContractType.TransferContract);
    trxStore.put(trx.getTransactionId().getBytes(), trx);
    Assert.assertEquals("Get transaction is error",
        trxStore.getLbechecked(trx.getTransactionId().getBytes()).getInstance(), trx.getInstance());
  }

  /**
   * put and get CreateWitnessTransaction.
   */
  @Test
  public void createWitnessTransactionStoreTest() throws BadItemException {
    WitnessCreateContract witnessContract = getWitnessContract(OWNER_ADDRESS, URL);
    TransactionCapsule transactionCapsule = new TransactionCapsule(witnessContract);
    transactionStore.put(key1, transactionCapsule);
    Assert.assertEquals("Store CreateWitnessTransaction is error",
        transactionStore.get(key1).getInstance(),
        transactionCapsule.getInstance());
  }

  /**
   * put and get TransferTransaction.
   */
  @Test
  public void transferTransactionStorenTest() throws BadItemException {
    AccountCapsule ownerCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8(ACCOLBET_NAME),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            AccountType.AssetIssue,
            1000000L
        );
    chainBaseManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
    TransferContract transferContract = getContract(AMOLBET, OWNER_ADDRESS, TO_ADDRESS);
    TransactionCapsule transactionCapsule = new TransactionCapsule(transferContract,
        chainBaseManager.getAccountStore());
    transactionStore.put(key1, transactionCapsule);
    Assert.assertEquals("Store TransferTransaction is error",
        transactionStore.get(key1).getInstance(),
        transactionCapsule.getInstance());
  }

  /**
   * put and get VoteWitnessTransaction.
   */

  @Test
  public void voteWitnessTransactionTest() throws BadItemException {

    AccountCapsule ownerAccountFirstCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8(ACCOLBET_NAME),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)),
            AccountType.Normal,
            1_000_000_000_000L);
    long frozenBalance = 1_000_000_000_000L;
    long duration = 3;
    ownerAccountFirstCapsule.setFrozen(frozenBalance, duration);
    chainBaseManager.getAccountStore()
        .put(ownerAccountFirstCapsule.getAddress().toByteArray(), ownerAccountFirstCapsule);
    VoteWitnessContract actuator = getVoteWitnessContract(OWNER_ADDRESS, WITNESS_ADDRESS, 1L);
    TransactionCapsule transactionCapsule = new TransactionCapsule(actuator);
    transactionStore.put(key1, transactionCapsule);
    Assert.assertEquals("Store VoteWitnessTransaction is error",
        transactionStore.get(key1).getInstance(),
        transactionCapsule.getInstance());
  }

  /**
   * put value is null and get it.
   */
  @Test
  public void transactionValueNullTest() throws BadItemException {
    TransactionCapsule transactionCapsule = null;
    transactionStore.put(key2, transactionCapsule);
    Assert.assertNull("put value is null", transactionStore.get(key2));

  }

  /**
   * put key is null and get it.
   */
  @Test
  public void transactionKeyNullTest() throws BadItemException {
    AccountCreateContract accountCreateContract = getContract(ACCOLBET_NAME,
        OWNER_ADDRESS);
    TransactionCapsule ret = new TransactionCapsule(accountCreateContract,
        chainBaseManager.getAccountStore());
    byte[] key = null;
    transactionStore.put(key, ret);
    try {
      transactionStore.get(key);
    } catch (RuntimeException e) {
      Assert.assertEquals("The key argument cannot be null", e.getMessage());
    }
  }
}
