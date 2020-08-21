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
import org.un.core.store.AccountStore;
import org.un.protos.Protocol.AccountType;

public class AccountStoreTest {

  private static final byte[] data = TransactionStoreTest.randomBytes(32);
  private static String dbPath = "output_AccountStore_test";
  private static String dbDirectory = "db_AccountStore_test";
  private static String indexDirectory = "index_AccountStore_test";
  private static LbeApplicationContext context;
  private static AccountStore accountStore;
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
    accountStore = context.getBean(AccountStore.class);
    AccountCapsule accountCapsule = new AccountCapsule(ByteString.copyFrom(address),
        ByteString.copyFrom(accountName),
        AccountType.forNumber(1));
    accountStore.put(data, accountCapsule);
  }

  @Test
  public void get() {
    //test get and has Method
    Assert
        .assertEquals(ByteArray.toHexString(address), ByteArray
            .toHexString(accountStore.get(data).getInstance().getAddress().toByteArray()))
    ;
    Assert
        .assertEquals(ByteArray.toHexString(accountName), ByteArray
            .toHexString(accountStore.get(data).getInstance().getAccountName().toByteArray()))
    ;
    Assert.assertTrue(accountStore.has(data));
  }
}