package org.un.core.store;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.un.core.capsule.BytesCapsule;
import org.un.core.db.LbeStoreWithRevoking;

@Component
public class NullifierStore extends LbeStoreWithRevoking<BytesCapsule> {

  @Autowired
  public NullifierStore(@Value("nullifier") String dbName) {
    super(dbName);
  }

  public void put(BytesCapsule bytesCapsule) {
    put(bytesCapsule.getData(), new BytesCapsule(bytesCapsule.getData()));
  }

  @Override
  public BytesCapsule get(byte[] key) {
    byte[] value = revokingDB.getLbechecked(key);
    if (ArrayUtils.isEmpty(value)) {
      return null;
    }
    return new BytesCapsule(value);
  }

  @Override
  public boolean has(byte[] key) {
    byte[] value = revokingDB.getLbechecked(key);

    return !ArrayUtils.isEmpty(value);
  }
}