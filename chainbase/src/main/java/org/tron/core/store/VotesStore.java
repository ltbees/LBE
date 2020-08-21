package org.un.core.store;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.un.core.capsule.VotesCapsule;
import org.un.core.db.LbeStoreWithRevoking;

@Component
public class VotesStore extends LbeStoreWithRevoking<VotesCapsule> {

  @Autowired
  public VotesStore(@Value("votes") String dbName) {
    super(dbName);
  }

  @Override
  public VotesCapsule get(byte[] key) {
    byte[] value = revokingDB.getLbechecked(key);
    return ArrayUtils.isEmpty(value) ? null : new VotesCapsule(value);
  }
}