package org.un.core.actuator;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Arrays;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.un.common.utils.Commons;
import org.un.common.utils.DecodeUtil;
import org.un.common.utils.StringUtil;
import org.un.core.capsule.AccountCapsule;
import org.un.core.capsule.ContractCapsule;
import org.un.core.capsule.TransactionResultCapsule;
import org.un.core.exception.ContractExeException;
import org.un.core.exception.ContractValidateException;
import org.un.core.store.AccountStore;
import org.un.core.store.ContractStore;
import org.un.core.vm.config.VMConfig;
import org.un.protos.Protocol.Transaction.Contract.ContractType;
import org.un.protos.Protocol.Transaction.Result.code;
import org.un.protos.contract.SmartContractOuterClass.ClearABIContract;

@Slf4j(topic = "actuator")
public class ClearABIContractActuator extends AbstractActuator {

  public ClearABIContractActuator() {
    super(ContractType.ClearABIContract, ClearABIContract.class);
  }

  @Override
  public boolean execute(Object result) throws ContractExeException {
    TransactionResultCapsule ret = (TransactionResultCapsule) result;
    if (Objects.isNull(ret)) {
      throw new RuntimeException(ActuatorConstant.TX_RESULT_NULL);
    }

    long fee = calcFee();
    ContractStore contractStore = chainBaseManager.getContractStore();
    try {
      ClearABIContract usContract = any.unpack(ClearABIContract.class);

      byte[] contractAddress = usContract.getContractAddress().toByteArray();
      ContractCapsule deployedContract = contractStore.get(contractAddress);

      deployedContract.clearABI();
      contractStore.put(contractAddress, deployedContract);

      ret.setStatus(fee, code.SUCESS);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }
    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    if (!VMConfig.allowTvmConstantinople()) {
      throw new ContractValidateException(
          "contract type error,unexpected type [ClearABIContract]");
    }

    if (this.any == null) {
      throw new ContractValidateException(ActuatorConstant.CONTRACT_NOT_EXIST);
    }
    if (chainBaseManager == null) {
      throw new ContractValidateException("No account store or contract store!");
    }
    AccountStore accountStore = chainBaseManager.getAccountStore();
    ContractStore contractStore = chainBaseManager.getContractStore();
    if (!this.any.is(ClearABIContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [ClearABIContract],real type["
              + any.getClass() + "]");
    }
    final ClearABIContract contract;
    try {
      contract = this.any.unpack(ClearABIContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }
    if (!DecodeUtil.addressValid(contract.getOwnerAddress().toByteArray())) {
      throw new ContractValidateException("Invalid address");
    }
    byte[] ownerAddress = contract.getOwnerAddress().toByteArray();
    String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);

    AccountCapsule accountCapsule = accountStore.get(ownerAddress);
    if (accountCapsule == null) {
      throw new ContractValidateException(
          "Account[" + readableOwnerAddress + "] not exists");
    }

    byte[] contractAddress = contract.getContractAddress().toByteArray();
    ContractCapsule deployedContract = contractStore.get(contractAddress);

    if (deployedContract == null) {
      throw new ContractValidateException(
          "Contract not exists");
    }

    byte[] deployedContractOwnerAddress = deployedContract.getInstance().getOriginAddress()
        .toByteArray();

    if (!Arrays.equals(ownerAddress, deployedContractOwnerAddress)) {
      throw new ContractValidateException(
          "Account[" + readableOwnerAddress + "] is not the owner of the contract");
    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return any.unpack(ClearABIContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }

}
