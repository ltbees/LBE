package org.un.core.actuator;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.un.common.utils.DecodeUtil;
import org.un.core.capsule.AccountCapsule;
import org.un.core.capsule.TransactionResultCapsule;
import org.un.core.exception.ContractExeException;
import org.un.core.exception.ContractValidateException;
import org.un.core.store.AccountIdIndexStore;
import org.un.core.store.AccountStore;
import org.un.core.utils.TransactionUtil;
import org.un.protos.Protocol.Transaction.Contract.ContractType;
import org.un.protos.Protocol.Transaction.Result.code;
import org.un.protos.contract.AccountContract.SetAccountIdContract;

@Slf4j(topic = "actuator")
public class SetAccountIdActuator extends AbstractActuator {

  public SetAccountIdActuator() {
    super(ContractType.SetAccountIdContract, SetAccountIdContract.class);
  }

  @Override
  public boolean execute(Object result) throws ContractExeException {
    TransactionResultCapsule ret = (TransactionResultCapsule) result;
    if (Objects.isNull(ret)) {
      throw new RuntimeException(ActuatorConstant.TX_RESULT_NULL);
    }

    final SetAccountIdContract setAccountIdContract;
    final long fee = calcFee();
    AccountStore accountStore = chainBaseManager.getAccountStore();
    AccountIdIndexStore accountIdIndexStore = chainBaseManager.getAccountIdIndexStore();
    try {
      setAccountIdContract = any.unpack(SetAccountIdContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }

    byte[] ownerAddress = setAccountIdContract.getOwnerAddress().toByteArray();
    AccountCapsule account = accountStore.get(ownerAddress);

    account.setAccountId(setAccountIdContract.getAccountId().toByteArray());
    accountStore.put(ownerAddress, account);
    accountIdIndexStore.put(account);
    ret.setStatus(fee, code.SUCESS);

    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    if (this.any == null) {
      throw new ContractValidateException(ActuatorConstant.CONTRACT_NOT_EXIST);
    }
    if (chainBaseManager == null) {
      throw new ContractValidateException("No account store or account id index store!");
    }
    AccountStore accountStore = chainBaseManager.getAccountStore();
    AccountIdIndexStore accountIdIndexStore = chainBaseManager.getAccountIdIndexStore();
    if (!this.any.is(SetAccountIdContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [SetAccountIdContract],real type[" + any
              .getClass() + "]");
    }
    final SetAccountIdContract setAccountIdContract;
    try {
      setAccountIdContract = any.unpack(SetAccountIdContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }
    byte[] ownerAddress = setAccountIdContract.getOwnerAddress().toByteArray();
    byte[] accountId = setAccountIdContract.getAccountId().toByteArray();
    if (!TransactionUtil.validAccountId(accountId)) {
      throw new ContractValidateException("Invalid accountId");
    }
    if (!DecodeUtil.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid ownerAddress");
    }

    AccountCapsule account = accountStore.get(ownerAddress);
    if (account == null) {
      throw new ContractValidateException("Account has not existed");
    }
    if (account.getAccountId() != null && !account.getAccountId().isEmpty()) {
      throw new ContractValidateException("This account id already set");
    }
    if (accountIdIndexStore.has(accountId)) {
      throw new ContractValidateException("This id has existed");
    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return any.unpack(SetAccountIdContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }
}

