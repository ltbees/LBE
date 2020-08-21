package org.un.core.actuator;

import static org.testng.Assert.fail;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.un.common.application.LbeApplicationContext;
import org.un.common.utils.ByteArray;
import org.un.common.utils.FileUtil;
import org.un.common.utils.StringUtil;
import org.un.core.Constant;
import org.un.core.Wallet;
import org.un.core.capsule.AccountCapsule;
import org.un.core.capsule.TransactionResultCapsule;
import org.un.core.config.DefaultConfig;
import org.un.core.config.args.Args;
import org.un.core.db.Manager;
import org.un.core.exception.ContractExeException;
import org.un.core.exception.ContractValidateException;
import org.un.protos.Protocol.AccountType;
import org.un.protos.Protocol.Transaction.Result.code;
import org.un.protos.contract.AccountContract.AccountCreateContract;
import org.un.protos.contract.AssetIssueContractOuterClass;

@Slf4j
public class CreateAccountActuatorTest {

  private static final String dbPath = "output_CreateAccount_test";
  private static final String OWNER_ADDRESS_FIRST;
  private static final String ACCOLBET_NAME_SECOND = "ownerS";
  private static final String OWNER_ADDRESS_SECOND;
  private static final String INVALID_ACCOLBET_ADDRESS;
  private static LbeApplicationContext context;
  private static Manager dbManager;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new LbeApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS_FIRST =
        Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    OWNER_ADDRESS_SECOND =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    INVALID_ACCOLBET_ADDRESS = Wallet.getAddressPreFixString() + "12344500882809695a8a687866";
  }

  /**
   * 548794500882809695a8a687866e76d4271a1abc Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
    //    Args.setParam(new String[]{"--output-directory", dbPath},
    //        "config-junit.conf");
    //    dbManager = new Manager();
    //    dbManager.init();
  }

  /**
   * Release resources.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }

  /**
   * create temp Capsule test need.
   */
  @Before
  public void createCapsule() {
    AccountCapsule ownerCapsule =
        new AccountCapsule(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_SECOND)),
            ByteString.copyFromUtf8(ACCOLBET_NAME_SECOND),
            AccountType.AssetIssue);
    dbManager.getAccountStore().put(ownerCapsule.getAddress().toByteArray(), ownerCapsule);
    dbManager.getAccountStore().delete(ByteArray.fromHexString(OWNER_ADDRESS_FIRST));
  }

  private Any getContract(String ownerAddress, String accountAddress) {
    return Any.pack(
        AccountCreateContract.newBuilder()
            .setAccountAddress(ByteString.copyFrom(ByteArray.fromHexString(accountAddress)))
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(ownerAddress)))
            .build());
  }

  /**
   * Lbeit test.
   */
  @Test
  public void firstCreateAccount() {
    CreateAccountActuator actuator = new CreateAccountActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(OWNER_ADDRESS_SECOND, OWNER_ADDRESS_FIRST));
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule accountCapsule =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS_FIRST));
      Assert.assertNotNull(accountCapsule);
      Assert.assertEquals(
          StringUtil.createReadableString(accountCapsule.getAddress()),
          OWNER_ADDRESS_FIRST);
    } catch (ContractValidateException e) {
      logger.info(e.getMessage());
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * Lbeit test.
   */
  @Test
  public void secondCreateAccount() {
    CreateAccountActuator actuator = new CreateAccountActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(OWNER_ADDRESS_SECOND, OWNER_ADDRESS_SECOND));
    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Account has existed", e.getMessage());
      AccountCapsule accountCapsule =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS_SECOND));
      Assert.assertNotNull(accountCapsule);
      Assert.assertEquals(
          accountCapsule.getAddress(),
          ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_SECOND)));
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * create a account will take some fee, which change account balance after creatation
   */
  @Test
  public void balanceAfterCreate() {
    CreateAccountActuator actuator = new CreateAccountActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(OWNER_ADDRESS_SECOND, OWNER_ADDRESS_FIRST));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    long currentBalance = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS_SECOND)).getBalance();
    long blackholeBalance = dbManager.getAccountStore().getBlackhole().getBalance();

    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      Assert.assertEquals(currentBalance, dbManager.getAccountStore()
          .get(ByteArray.fromHexString(OWNER_ADDRESS_SECOND)).getBalance() - actuator.calcFee());
      Assert.assertEquals(blackholeBalance, dbManager.getAccountStore()
          .getBlackhole().getBalance() + actuator.calcFee());

    } catch (ContractValidateException e) {
      logger.info(e.getMessage());
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  /**
   * owner address not exit in DB
   */
  @Test
  public void noExitsAccount() {
    CreateAccountActuator actuator = new CreateAccountActuator();
    TransactionResultCapsule ret = new TransactionResultCapsule();

    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(OWNER_ADDRESS_SECOND, OWNER_ADDRESS_FIRST));
    byte[] ownerAddress = ByteArray.fromHexString(OWNER_ADDRESS_SECOND);
    String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);
    // delete account address, which just create
    dbManager.getAccountStore().delete(ByteArray.fromHexString(OWNER_ADDRESS_SECOND));

    processAndCheckInvalid(actuator, ret, "Account[",
        "Account[" + readableOwnerAddress + "] not exists");
  }

  /**
   * not have enough fee to create a account
   */
  @Test
  public void inSufficientFeeAccount() {
    CreateAccountActuator actuator = new CreateAccountActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(OWNER_ADDRESS_SECOND, OWNER_ADDRESS_FIRST));
    TransactionResultCapsule ret = new TransactionResultCapsule();
    AccountCapsule owner = dbManager.getAccountStore()
        .get(ByteArray.fromHexString(OWNER_ADDRESS_SECOND));
    owner.setBalance(-100);
    dbManager.getAccountStore().put(owner.createDbKey(), owner);

    processAndCheckInvalid(actuator, ret, "Validate CreateAccountActuator error, insufficient fee.",
        "Validate CreateAccountActuator error, insufficient fee.");
  }

  /**
   * Invalid account address
   */
  @Test
  public void invalidAccount() {
    CreateAccountActuator actuator = new CreateAccountActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(OWNER_ADDRESS_SECOND, INVALID_ACCOLBET_ADDRESS));
    TransactionResultCapsule ret = new TransactionResultCapsule();

    processAndCheckInvalid(actuator, ret, "Invalid account address", "Invalid account address");
  }


  @Test
  public void commonErrorCheck() {

    CreateAccountActuator actuator = new CreateAccountActuator();
    ActuatorTest actuatorTest = new ActuatorTest(actuator, dbManager);
    actuatorTest.noContract();

    Any invalidContractTypes = Any.pack(AssetIssueContractOuterClass.AssetIssueContract.newBuilder()
        .build());
    actuatorTest.setInvalidContract(invalidContractTypes);
    actuatorTest.setInvalidContractTypeMsg("contract type error",
        "contract type error,expected type [AccountCreateContract],real type[");
    actuatorTest.invalidContractType();

    actuatorTest.setContract(getContract(OWNER_ADDRESS_SECOND, OWNER_ADDRESS_FIRST));
    actuatorTest.nullTransationResult();

    actuatorTest.setNullDBManagerMsg("No account store or contract store!");
    actuatorTest.nullDBManger();

  }

  private void processAndCheckInvalid(CreateAccountActuator actuator, TransactionResultCapsule ret,
      String failMsg,
      String expectedMsg) {
    try {
      actuator.validate();
      actuator.execute(ret);

      fail(failMsg);
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals(expectedMsg, e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } catch (RuntimeException e) {
      Assert.assertTrue(e instanceof RuntimeException);
      Assert.assertEquals(expectedMsg, e.getMessage());
    }
  }


}
