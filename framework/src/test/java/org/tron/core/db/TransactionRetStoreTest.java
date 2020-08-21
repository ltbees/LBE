package org.un.core.db;

import java.io.File;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.un.common.application.LbeApplicationContext;
import org.un.common.utils.ByteArray;
import org.un.common.utils.FileUtil;
import org.un.core.Constant;
import org.un.core.capsule.TransactionCapsule;
import org.un.core.capsule.TransactionInfoCapsule;
import org.un.core.capsule.TransactionRetCapsule;
import org.un.core.config.DefaultConfig;
import org.un.core.config.args.Args;
import org.un.core.exception.BadItemException;
import org.un.core.store.TransactionRetStore;
import org.un.protos.Protocol.Transaction;

public class TransactionRetStoreTest {

  private static final byte[] transactionId = TransactionStoreTest.randomBytes(32);
  private static final byte[] blockNum = ByteArray.fromLong(1);
  private static String dbPath = "output_TransactionRetStore_test";
  private static String dbDirectory = "db_TransactionRetStore_test";
  private static String indexDirectory = "index_TransactionRetStore_test";
  private static LbeApplicationContext context;
  private static TransactionRetStore transactionRetStore;
  private static Transaction transaction;
  private static TransactionStore transactionStore;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath, "--storage-db-directory", dbDirectory,
        "--storage-index-directory", indexDirectory}, Constant.TEST_CONF);
    context = new LbeApplicationContext(DefaultConfig.class);
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  @BeforeClass
  public static void init() {
    transactionRetStore = context.getBean(TransactionRetStore.class);
    transactionStore = context.getBean(TransactionStore.class);
    TransactionInfoCapsule transactionInfoCapsule = new TransactionInfoCapsule();

    transactionInfoCapsule.setId(transactionId);
    transactionInfoCapsule.setFee(1000L);
    transactionInfoCapsule.setBlockNumber(100L);
    transactionInfoCapsule.setBlockTimeStamp(200L);

    TransactionRetCapsule transactionRetCapsule = new TransactionRetCapsule();
    transactionRetCapsule.addTransactionInfo(transactionInfoCapsule.getInstance());
    transactionRetStore.put(blockNum, transactionRetCapsule);
    transaction = Transaction.newBuilder().build();
    TransactionCapsule transactionCapsule = new TransactionCapsule(transaction);
    transactionCapsule.setBlockNum(1);
    transactionStore.put(transactionId, transactionCapsule);
  }

  @Test
  public void get() throws BadItemException {
    TransactionInfoCapsule resultCapsule = transactionRetStore.getTransactionInfo(transactionId);
    Assert.assertNotNull("get transaction ret store", resultCapsule);
  }

  @Test
  public void put() {
    TransactionInfoCapsule transactionInfoCapsule = new TransactionInfoCapsule();
    transactionInfoCapsule.setId(transactionId);
    transactionInfoCapsule.setFee(1000L);
    transactionInfoCapsule.setBlockNumber(100L);
    transactionInfoCapsule.setBlockTimeStamp(200L);

    TransactionRetCapsule transactionRetCapsule = new TransactionRetCapsule();
    transactionRetCapsule.addTransactionInfo(transactionInfoCapsule.getInstance());
    Assert.assertNull("put transaction info error",
        transactionRetStore.getLbechecked(transactionInfoCapsule.getId()));
    transactionRetStore.put(transactionInfoCapsule.getId(), transactionRetCapsule);
    Assert.assertNotNull("get transaction info error",
        transactionRetStore.getLbechecked(transactionInfoCapsule.getId()));
  }
}