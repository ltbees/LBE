package org.un.core.store;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.un.common.parameter.CommonParameter;
import org.un.core.capsule.TransactionInfoCapsule;
import org.un.core.db.LbeStoreWithRevoking;
import org.un.core.exception.BadItemException;

@Component
public class TransactionHistoryStore extends LbeStoreWithRevoking<TransactionInfoCapsule> {

  @Autowired
  public TransactionHistoryStore(@Value("transactionHistoryStore") String dbName) {
    super(dbName);
  }

  @Override
  public TransactionInfoCapsule get(byte[] key) throws BadItemException {
    byte[] value = revokingDB.getLbechecked(key);
    return ArrayUtils.isEmpty(value) ? null : new TransactionInfoCapsule(value);
  }

  @Override
  public void put(byte[] key, TransactionInfoCapsule item) {
    if (BooleanUtils.toBoolean(CommonParameter.getInstance()
        .getStorage().getTransactionHistorySwitch())) {
      super.put(key, item);
    }
  }
}
