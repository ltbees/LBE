package org.un.common.runtime.vm;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.spongycastle.util.encoders.Hex;
import org.un.common.application.ApplicationFactory;
import org.un.common.application.LbeApplicationContext;
import org.un.common.runtime.Runtime;
import org.un.common.storage.Deposit;
import org.un.common.storage.DepositImpl;
import org.un.common.utils.FileUtil;
import org.un.core.Constant;
import org.un.core.Wallet;
import org.un.core.config.DefaultConfig;
import org.un.core.config.args.Args;
import org.un.core.db.Manager;
import org.un.protos.Protocol.AccountType;

@Slf4j
public class VMTestBase {

  protected Manager manager;
  protected LbeApplicationContext context;
  protected String dbPath;
  protected Deposit rootDeposit;
  protected String OWNER_ADDRESS;
  protected Runtime runtime;

  @Before
  public void init() {
    dbPath = "output_" + this.getClass().getName();
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"}, Constant.TEST_CONF);
    context = new LbeApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    manager = context.getBean(Manager.class);
    rootDeposit = DepositImpl.createRoot(manager);
    rootDeposit.createAccount(Hex.decode(OWNER_ADDRESS), AccountType.Normal);
    rootDeposit.addBalance(Hex.decode(OWNER_ADDRESS), 30000000000000L);

    rootDeposit.commit();
  }

  @After
  public void destroy() {
    Args.clearParam();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.error("Release resources failure.");
    }
  }

}
