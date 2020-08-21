package org.un.core.store;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.un.core.capsule.StorageRowCapsule;
import org.un.core.db.LbeStoreWithRevoking;

@Slf4j(topic = "DB")
@Component
public class StorageRowStore extends LbeStoreWithRevoking<StorageRowCapsule> {

  @Autowired
  private StorageRowStore(@Value("storage-row") String dbName) {
    super(dbName);
  }

  @Override
  public StorageRowCapsule get(byte[] key) {
    StorageRowCapsule row = getLbechecked(key);
    row.setRowKey(key);
    return row;
  }
}
