package org.un.core.db;

import com.google.protobuf.ByteString;
import java.io.File;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.un.common.application.LbeApplicationContext;
import org.un.common.utils.ByteArray;
import org.un.common.utils.FileUtil;
import org.un.core.Constant;
import org.un.core.capsule.AccountCapsule;
import org.un.core.config.DefaultConfig;
import org.un.core.config.args.Args;
import org.un.core.store.AccountIndexStore;
import org.un.protos.Protocol.AccountType;

public class AccountIndexStoreTest {

  private static String dbPath = "output_AccountIndexStore_test";
  private static String dbDirectory = "db_AccountIndexStore_test";
  private static String indexDirectory = "index_AccountIndexStore_test";
  private static LbeApplicationContext context;
  private static AccountIndexStore accountIndexStore;
  private static byte[] address = TransactionStoreTest.randomBytes(32);
  private static byte[] accountName = TransactionStoreTest.randomBytes(32);

  static {
    Args.setParam(
        new String[]{
            "--output-directory", dbPath,
            "--storage-db-directory", dbDirectory,
            "--storage-index-directory", indexDirectory
        },
        Constant.TEST_CONF
    );
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
    accountIndexStore = context.getBean(AccountIndexStore.class);
    AccountCapsule accountCapsule = new AccountCapsule(ByteString.copyFrom(address),
        ByteString.copyFrom(accountName),
        AccountType.forNumber(1));
    accountIndexStore.put(accountCapsule);
  }

  @Test
  public void get() {
    //test get(ByteString name)
    Assert
        .assertEquals(ByteArray.toHexString(address), ByteArray
            .toHexString(accountIndexStore.get(ByteString.copyFrom(accountName))))
    ;
    //test get(byte[] key)
    Assert
        .assertEquals(ByteArray.toHexString(address), ByteArray
            .toHexString(accountIndexStore.get(accountName).getData()))
    ;
    Assert.assertTrue(accountIndexStore.has(accountName));
  }
}