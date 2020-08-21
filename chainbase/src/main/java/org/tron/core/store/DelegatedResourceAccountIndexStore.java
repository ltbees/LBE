package org.un.core.store;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.un.core.capsule.DelegatedResourceAccountIndexCapsule;
import org.un.core.db.LbeStoreWithRevoking;

@Component
public class DelegatedResourceAccountIndexStore extends
    LbeStoreWithRevoking<DelegatedResourceAccountIndexCapsule> {

  @Autowired
  public DelegatedResourceAccountIndexStore(@Value("DelegatedResourceAccountIndex") String dbName) {
    super(dbName);
  }

  @Override
  public DelegatedResourceAccountIndexCapsule get(byte[] key) {

    byte[] value = revokingDB.getLbechecked(key);
    return ArrayUtils.isEmpty(value) ? null : new DelegatedResourceAccountIndexCapsule(value);
  }

}