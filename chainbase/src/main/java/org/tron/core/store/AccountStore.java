package org.un.core.store;

import com.typesafe.config.ConfigObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.un.common.utils.Commons;
import org.un.core.capsule.AccountCapsule;
import org.un.core.db.LbeStoreWithRevoking;
import org.un.core.db.accountstate.AccountStateCallBackUtils;

@Slf4j(topic = "DB")
@Component
public class AccountStore extends LbeStoreWithRevoking<AccountCapsule> {

  private static Map<String, byte[]> assertsAddress = new HashMap<>(); // key = name , value = address

  @Autowired
  private AccountStateCallBackUtils accountStateCallBackUtils;

  @Autowired
  private AccountStore(@Value("account") String dbName) {
    super(dbName);
  }

  public static void setAccount(com.typesafe.config.Config config) {
    List list = config.getObjectList("genesis.block.assets");
    for (int i = 0; i < list.size(); i++) {
      ConfigObject obj = (ConfigObject) list.get(i);
      String accountName = obj.get("accountName").unwrapped().toString();
      byte[] address = Commons.decodeFromBase58Check(obj.get("address").unwrapped().toString());
      assertsAddress.put(accountName, address);
    }
  }

  @Override
  public AccountCapsule get(byte[] key) {
    byte[] value = revokingDB.getLbechecked(key);
    return ArrayUtils.isEmpty(value) ? null : new AccountCapsule(value);
  }

  @Override
  public void put(byte[] key, AccountCapsule item) {
    super.put(key, item);
    accountStateCallBackUtils.accountCallBack(key, item);
  }

  /**
   * Max LBE account.
   */
  public AccountCapsule getSun() {
    return getLbechecked(assertsAddress.get("Sun"));
  }

  /**
   * Min LBE account.
   */
  public AccountCapsule getBlackhole() {
    return getLbechecked(assertsAddress.get("Blackhole"));
  }

  /**
   * Get foundation account info.
   */
  public AccountCapsule getZion() {
    return getLbechecked(assertsAddress.get("Zion"));
  }

  @Override
  public void close() {
    super.close();
  }
}
