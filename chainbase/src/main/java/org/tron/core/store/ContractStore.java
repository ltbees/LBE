package org.un.core.store;

import com.google.common.collect.Streams;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.un.core.capsule.ContractCapsule;
import org.un.core.db.LbeStoreWithRevoking;
import org.un.protos.contract.SmartContractOuterClass.SmartContract;

@Slf4j(topic = "DB")
@Component
public class ContractStore extends LbeStoreWithRevoking<ContractCapsule> {

  @Autowired
  private ContractStore(@Value("contract") String dbName) {
    super(dbName);
  }

  @Override
  public ContractCapsule get(byte[] key) {
    return getLbechecked(key);
  }

  /**
   * get total transaction.
   */
  public long getTotalContracts() {
    return Streams.stream(revokingDB.iterator()).count();
  }

  /**
   * find a transaction  by it's id.
   */
  public byte[] findContractByHash(byte[] trxHash) {
    return revokingDB.getLbechecked(trxHash);
  }

  /**
   *
   * @param contractAddress
   * @return
   */
  public SmartContract.ABI getABI(byte[] contractAddress) {
    byte[] value = revokingDB.getLbechecked(contractAddress);
    if (ArrayUtils.isEmpty(value)) {
      return null;
    }

    ContractCapsule contractCapsule = new ContractCapsule(value);
    SmartContract smartContract = contractCapsule.getInstance();
    if (smartContract == null) {
      return null;
    }

    return smartContract.getAbi();
  }

}
