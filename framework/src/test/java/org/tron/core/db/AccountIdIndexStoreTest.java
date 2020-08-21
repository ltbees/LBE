package org.un.core.db;

import com.google.protobuf.ByteString;
import java.io.File;
import java.util.Random;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.un.common.application.LbeApplicationContext;
import org.un.common.utils.FileUtil;
import org.un.core.Constant;
import org.un.core.Wallet;
import org.un.core.capsule.AccountCapsule;
import org.un.core.config.DefaultConfig;
import org.un.core.config.args.Args;
import org.un.core.store.AccountIdIndexStore;
import org.un.protos.Protocol.AccountType;

public class AccountIdIndexStoreTest {

  private static final byte[] ACCOLBET_ADDRESS_ONE = randomBytes(16);
  private static final byte[] ACCOLBET_ADDRESS_TWO = randomBytes(16);
  private static final byte[] ACCOLBET_ADDRESS_THREE = randomBytes(16);
  private static final byte[] ACCOLBET_ADDRESS_FOUR = randomBytes(16);
  private static final byte[] ACCOLBET_NAME_ONE = randomBytes(6);
  private static final byte[] ACCOLBET_NAME_TWO = randomBytes(6);
  private static final byte[] ACCOLBET_NAME_THREE = randomBytes(6);
  private static final byte[] ACCOLBET_NAME_FOUR = randomBytes(6);
  private static final byte[] ACCOLBET_NAME_FIVE = randomBytes(6);
  private static String dbPath = "output_AccountIndexStore_test";
  private static LbeApplicationContext context;
  private static AccountIdIndexStore accountIdIndexStore;
  private static AccountCapsule accountCapsule1;
  private static AccountCapsule accountCapsule2;
  private static AccountCapsule accountCapsule3;
  private static AccountCapsule accountCapsule4;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath},
        Constant.TEST_CONF);
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
    accountIdIndexStore = context.getBean(AccountIdIndexStore.class);
    accountCapsule1 = new AccountCapsule(ByteString.copyFrom(ACCOLBET_ADDRESS_ONE),
        ByteString.copyFrom(ACCOLBET_NAME_ONE), AccountType.Normal);
    accountCapsule1.setAccountId(ByteString.copyFrom(ACCOLBET_NAME_ONE).toByteArray());
    accountCapsule2 = new AccountCapsule(ByteString.copyFrom(ACCOLBET_ADDRESS_TWO),
        ByteString.copyFrom(ACCOLBET_NAME_TWO), AccountType.Normal);
    accountCapsule2.setAccountId(ByteString.copyFrom(ACCOLBET_NAME_TWO).toByteArray());
    accountCapsule3 = new AccountCapsule(ByteString.copyFrom(ACCOLBET_ADDRESS_THREE),
        ByteString.copyFrom(ACCOLBET_NAME_THREE), AccountType.Normal);
    accountCapsule3.setAccountId(ByteString.copyFrom(ACCOLBET_NAME_THREE).toByteArray());
    accountCapsule4 = new AccountCapsule(ByteString.copyFrom(ACCOLBET_ADDRESS_FOUR),
        ByteString.copyFrom(ACCOLBET_NAME_FOUR), AccountType.Normal);
    accountCapsule4.setAccountId(ByteString.copyFrom(ACCOLBET_NAME_FOUR).toByteArray());
    accountIdIndexStore.put(accountCapsule1);
    accountIdIndexStore.put(accountCapsule2);
    accountIdIndexStore.put(accountCapsule3);
    accountIdIndexStore.put(accountCapsule4);
  }

  public static byte[] randomBytes(int length) {
    // generate the random number
    byte[] result = new byte[length];
    new Random().nextBytes(result);
    result[0] = Wallet.getAddressPreFixByte();
    return result;
  }

  @Test
  public void putAndGet() {
    byte[] address = accountIdIndexStore.get(ByteString.copyFrom(ACCOLBET_NAME_ONE));
    Assert.assertArrayEquals("putAndGet1", address, ACCOLBET_ADDRESS_ONE);
    address = accountIdIndexStore.get(ByteString.copyFrom(ACCOLBET_NAME_TWO));
    Assert.assertArrayEquals("putAndGet2", address, ACCOLBET_ADDRESS_TWO);
    address = accountIdIndexStore.get(ByteString.copyFrom(ACCOLBET_NAME_THREE));
    Assert.assertArrayEquals("putAndGet3", address, ACCOLBET_ADDRESS_THREE);
    address = accountIdIndexStore.get(ByteString.copyFrom(ACCOLBET_NAME_FOUR));
    Assert.assertArrayEquals("putAndGet4", address, ACCOLBET_ADDRESS_FOUR);
    address = accountIdIndexStore.get(ByteString.copyFrom(ACCOLBET_NAME_FIVE));
    Assert.assertNull("putAndGet4", address);

  }

  @Test
  public void putAndHas() {
    Boolean result = accountIdIndexStore.has(ACCOLBET_NAME_ONE);
    Assert.assertTrue("putAndGet1", result);
    result = accountIdIndexStore.has(ACCOLBET_NAME_TWO);
    Assert.assertTrue("putAndGet2", result);
    result = accountIdIndexStore.has(ACCOLBET_NAME_THREE);
    Assert.assertTrue("putAndGet3", result);
    result = accountIdIndexStore.has(ACCOLBET_NAME_FOUR);
    Assert.assertTrue("putAndGet4", result);
    result = accountIdIndexStore.has(ACCOLBET_NAME_FIVE);
    Assert.assertFalse("putAndGet4", result);
  }

  @Test
  public void testCaseInsensitive() {
    byte[] ACCOLBET_NAME = "aABbCcDd_ssd1234".getBytes();
    byte[] ACCOLBET_ADDRESS = randomBytes(16);

    AccountCapsule accountCapsule = new AccountCapsule(ByteString.copyFrom(ACCOLBET_ADDRESS),
        ByteString.copyFrom(ACCOLBET_NAME), AccountType.Normal);
    accountCapsule.setAccountId(ByteString.copyFrom(ACCOLBET_NAME).toByteArray());
    accountIdIndexStore.put(accountCapsule);

    Boolean result = accountIdIndexStore.has(ACCOLBET_NAME);
    Assert.assertTrue("fail", result);

    byte[] lowerCase = ByteString
        .copyFromUtf8(ByteString.copyFrom(ACCOLBET_NAME).toStringUtf8().toLowerCase())
        .toByteArray();
    result = accountIdIndexStore.has(lowerCase);
    Assert.assertTrue("lowerCase fail", result);

    byte[] upperCase = ByteString
        .copyFromUtf8(ByteString.copyFrom(ACCOLBET_NAME).toStringUtf8().toUpperCase())
        .toByteArray();
    result = accountIdIndexStore.has(upperCase);
    Assert.assertTrue("upperCase fail", result);

    Assert.assertNotNull("getLowerCase fail", accountIdIndexStore.get(upperCase));

  }
}