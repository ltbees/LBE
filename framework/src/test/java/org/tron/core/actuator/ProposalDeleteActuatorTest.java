package org.un.core.actuator;

import static junit.framework.TestCase.fail;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import java.util.HashMap;
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
import org.un.core.capsule.ProposalCapsule;
import org.un.core.capsule.TransactionResultCapsule;
import org.un.core.capsule.WitnessCapsule;
import org.un.core.config.DefaultConfig;
import org.un.core.config.args.Args;
import org.un.core.db.Manager;
import org.un.core.exception.ContractExeException;
import org.un.core.exception.ContractValidateException;
import org.un.core.exception.ItemNotFoundException;
import org.un.protos.Protocol.AccountType;
import org.un.protos.Protocol.Proposal.State;
import org.un.protos.Protocol.Transaction.Result.code;
import org.un.protos.contract.AssetIssueContractOuterClass;
import org.un.protos.contract.ProposalContract;

@Slf4j

public class ProposalDeleteActuatorTest {

  private static final String dbPath = "output_ProposalApprove_test";
  private static final String ACCOLBET_NAME_FIRST = "ownerF";
  private static final String OWNER_ADDRESS_FIRST;
  private static final String ACCOLBET_NAME_SECOND = "ownerS";
  private static final String OWNER_ADDRESS_SECOND;
  private static final String URL = "https://un.network";
  private static final String OWNER_ADDRESS_INVALID = "aaaa";
  private static final String OWNER_ADDRESS_NOACCOLBET;
  private static LbeApplicationContext context;
  private static Manager dbManager;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    context = new LbeApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS_FIRST =
        Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    OWNER_ADDRESS_SECOND =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";
    OWNER_ADDRESS_NOACCOLBET =
        Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1aed";
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
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
  public void initTest() {
    WitnessCapsule ownerWitnessFirstCapsule =
        new WitnessCapsule(
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)),
            10_000_000L,
            URL);
    AccountCapsule ownerAccountFirstCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8(ACCOLBET_NAME_FIRST),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_FIRST)),
            AccountType.Normal,
            300_000_000L);
    AccountCapsule ownerAccountSecondCapsule =
        new AccountCapsule(
            ByteString.copyFromUtf8(ACCOLBET_NAME_SECOND),
            ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_SECOND)),
            AccountType.Normal,
            200_000_000_000L);

    dbManager.getAccountStore()
        .put(ownerAccountFirstCapsule.getAddress().toByteArray(), ownerAccountFirstCapsule);
    dbManager.getAccountStore()
        .put(ownerAccountSecondCapsule.getAddress().toByteArray(), ownerAccountSecondCapsule);

    dbManager.getWitnessStore().put(ownerWitnessFirstCapsule.getAddress().toByteArray(),
        ownerWitnessFirstCapsule);

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1000000);
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(10);
    dbManager.getDynamicPropertiesStore().saveNextMaintenanceTime(2000000);
    dbManager.getDynamicPropertiesStore().saveLatestProposalNum(0);

    long id = 1;
    dbManager.getProposalStore().delete(ByteArray.fromLong(1));
    dbManager.getProposalStore().delete(ByteArray.fromLong(2));
    HashMap<Long, Long> paras = new HashMap<>();
    paras.put(0L, 3 * 27 * 1000L);
    ProposalCreateActuator actuator = new ProposalCreateActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setForkUtils(dbManager.getForkController())
        .setAny(getContract(OWNER_ADDRESS_FIRST, paras));
    TransactionResultCapsule ret = new TransactionResultCapsule();
    Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestProposalNum(), 0);
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      ProposalCapsule proposalCapsule = dbManager.getProposalStore().get(ByteArray.fromLong(id));
      Assert.assertNotNull(proposalCapsule);
      Assert.assertEquals(dbManager.getDynamicPropertiesStore().getLatestProposalNum(), 1);
      Assert.assertEquals(proposalCapsule.getApprovals().size(), 0);
      Assert.assertEquals(proposalCapsule.getCreateTime(), 1000000);
      Assert.assertEquals(proposalCapsule.getExpirationTime(),
          261200000); // 2000000 + 3 * 4 * 21600000
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
    }
  }

  private Any getContract(String address, HashMap<Long, Long> paras) {
    return Any.pack(
        ProposalContract.ProposalCreateContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
            .putAllParameters(paras)
            .build());
  }

  private Any getContract(String address, long id) {
    return Any.pack(
        ProposalContract.ProposalDeleteContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(address)))
            .setProposalId(id)
            .build());
  }

  /**
   * first deleteProposal, result is success.
   */
  @Test
  public void successDeleteApprove() {
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1000100);
    long id = 1;

    ProposalDeleteActuator actuator = new ProposalDeleteActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(OWNER_ADDRESS_FIRST, id));
    TransactionResultCapsule ret = new TransactionResultCapsule();
    ProposalCapsule proposalCapsule;
    try {
      proposalCapsule = dbManager.getProposalStore().get(ByteArray.fromLong(id));
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
      return;
    }
    Assert.assertEquals(proposalCapsule.getState(), State.PENDING);
    try {
      actuator.validate();
      actuator.execute(ret);
      Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      try {
        proposalCapsule = dbManager.getProposalStore().get(ByteArray.fromLong(id));
      } catch (ItemNotFoundException e) {
        Assert.assertFalse(e instanceof ItemNotFoundException);
        return;
      }
      Assert.assertEquals(proposalCapsule.getState(), State.CANCELED);
    } catch (ContractValidateException e) {
      Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }

  }

  /**
   * use Invalid Address, result is failed, exception is "Invalid address".
   */
  @Test
  public void invalidAddress() {
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1000100);
    long id = 1;
    ProposalDeleteActuator actuator = new ProposalDeleteActuator();

    ActuatorTest actuatorTest = new ActuatorTest(actuator, dbManager);
    actuatorTest.setContract(getContract(OWNER_ADDRESS_INVALID, id));
    actuatorTest.setMessage("Invalid address", "Invalid address");
    actuatorTest.invalidOwnerAddress();

  }

  /**
   * use Account not exists, result is failed, exception is "account not exists".
   */
  @Test
  public void noAccount() {
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1000100);
    long id = 1;

    ProposalDeleteActuator actuator = new ProposalDeleteActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(OWNER_ADDRESS_NOACCOLBET, id));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("account[+OWNER_ADDRESS_NOACCOLBET+] not exists");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Account[" + OWNER_ADDRESS_NOACCOLBET + "] not exists",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * Proposal is not proposed by witness, result is failed,exception is "witness not exists".
   */
  @Test
  public void notProposed() {
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1000100);
    long id = 1;

    ProposalDeleteActuator actuator = new ProposalDeleteActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(OWNER_ADDRESS_SECOND, id));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("witness[+OWNER_ADDRESS_NOWITNESS+] not exists");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Proposal[" + id + "] " + "is not proposed by "
              + StringUtil.createReadableString(
          ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS_SECOND))),
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * use Proposal not exists, result is failed, exception is "Proposal not exists".
   */
  @Test
  public void noProposal() {
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1000100);
    long id = 2;

    ProposalDeleteActuator actuator = new ProposalDeleteActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(OWNER_ADDRESS_FIRST, id));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("Proposal[" + id + "] not exists");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Proposal[" + id + "] not exists",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * Proposal expired, result is failed, exception is "Proposal expired".
   */
  @Test
  public void proposalExpired() {
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(261200100);
    long id = 1;

    ProposalDeleteActuator actuator = new ProposalDeleteActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(OWNER_ADDRESS_FIRST, id));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("Proposal[" + id + "] expired");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Proposal[" + id + "] expired",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }

  /**
   * Proposal canceled, result is failed, exception is "Proposal expired".
   */
  @Test
  public void proposalCanceled() {
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(100100);
    long id = 1;

    ProposalDeleteActuator actuator = new ProposalDeleteActuator();
    actuator.setChainBaseManager(dbManager.getChainBaseManager())
        .setAny(getContract(OWNER_ADDRESS_FIRST, id));

    TransactionResultCapsule ret = new TransactionResultCapsule();
    ProposalCapsule proposalCapsule;
    try {
      proposalCapsule = dbManager.getProposalStore().get(ByteArray.fromLong(id));
      proposalCapsule.setState(State.CANCELED);
      dbManager.getProposalStore().put(proposalCapsule.createDbKey(), proposalCapsule);
    } catch (ItemNotFoundException e) {
      Assert.assertFalse(e instanceof ItemNotFoundException);
      return;
    }
    Assert.assertEquals(proposalCapsule.getApprovals().size(), 0);
    try {
      actuator.validate();
      actuator.execute(ret);
      fail("Proposal[" + id + "] canceled");
    } catch (ContractValidateException e) {
      Assert.assertTrue(e instanceof ContractValidateException);
      Assert.assertEquals("Proposal[" + id + "] canceled",
          e.getMessage());
    } catch (ContractExeException e) {
      Assert.assertFalse(e instanceof ContractExeException);
    }
  }


  @Test
  public void commonErrorCheck() {

    ProposalDeleteActuator actuator = new ProposalDeleteActuator();
    ActuatorTest actuatorTest = new ActuatorTest(actuator, dbManager);
    actuatorTest.noContract();

    Any invalidContractTypes = Any.pack(AssetIssueContractOuterClass.AssetIssueContract.newBuilder()
        .build());
    actuatorTest.setInvalidContract(invalidContractTypes);
    actuatorTest.setInvalidContractTypeMsg("contract type error",
        "contract type error,expected type [ProposalDeleteContract],real type[");
    actuatorTest.invalidContractType();

    actuatorTest.setContract(getContract(OWNER_ADDRESS_FIRST, 1));
    actuatorTest.nullTransationResult();

    actuatorTest.setNullDBManagerMsg("No account store or dynamic store!");
    actuatorTest.nullDBManger();

  }


}