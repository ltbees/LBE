package org.un.core.store;

import com.google.common.collect.Streams;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.un.core.capsule.WitnessCapsule;
import org.un.core.db.LbeStoreWithRevoking;

@Slf4j(topic = "DB")
@Component
public class WitnessStore extends LbeStoreWithRevoking<WitnessCapsule> {

  @Autowired
  protected WitnessStore(@Value("witness") String dbName) {
    super(dbName);
  }

  /**
   * get all witnesses.
   */
  public List<WitnessCapsule> getAllWitnesses() {
    return Streams.stream(iterator())
        .map(Entry::getValue)
        .collect(Collectors.toList());
  }

  @Override
  public WitnessCapsule get(byte[] key) {
    byte[] value = revokingDB.getLbechecked(key);
    return ArrayUtils.isEmpty(value) ? null : new WitnessCapsule(value);
  }
}
