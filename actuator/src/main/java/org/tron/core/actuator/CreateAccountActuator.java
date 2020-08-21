package org.un.core.actuator;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.un.common.utils.Commons;
import org.un.common.utils.DecodeUtil;
import org.un.common.utils.StringUtil;
import org.un.core.capsule.AccountCapsule;
import org.un.core.capsule.TransactionResultCapsule;
import org.un.core.exception.BalanceInsufficientException;
import org.un.core.exception.ContractExeException;
import org.un.core.exception.ContractValidateException;
import org.un.core.store.AccountStore;
import org.un.core.store.DynamicPropertiesStore;
import org.un.protos.Protocol.Transaction.Contract.ContractType;
import org.un.protos.Protocol.Transaction.Result.code;
import org.un.protos.contract.AccountContract.AccountCreateContract;

@Slf4j(topic = "actuator")
public class CreateAccountActuator extends AbstractActuator {

  public CreateAccountActuator() {
    super(ContractType.AccountCreateContract, AccountCreateContract.class);
  }

  @Override
  public boolean execute(Object result)
      throws ContractExeException {
    TransactionResultCapsule ret = (TransactionResultCapsule) result;
    if (Objects.isNull(ret)) {
      throw new RuntimeException(ActuatorConstant.TX_RESULT_NULL);
    }

    long fee = calcFee();
    DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
    AccountStore accountStore = chainBaseManager.getAccountStore();
    try {
      AccountCreateContract accountCreateContract = any.unpack(AccountCreateContract.class);
      boolean withDefaultPermission =
          dynamicStore.getAllowMultiSign() == 1;
      AccountCapsule accountCapsule = new AccountCapsule(accountCreateContract,
          dynamicStore.getLatestBlockHeaderTimestamp(), withDefaultPermission, dynamicStore);

      accountStore
          .put(accountCreateContract.getAccountAddress().toByteArray(), accountCapsule);

      Commons
          .adjustBalance(accountStore, accountCreateContract.getOwnerAddress().toByteArray(), -fee);
      // Add to blackhole address
      Commons.adjustBalance(accountStore, accountStore.getBlackhole().createDbKey(), fee);

      ret.setStatus(fee, code.SUCESS);
    } catch (BalanceInsufficientException | InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }

    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    if (this.any == null) {
      throw new ContractValidateException(ActuatorConstant.CONTRACT_NOT_EXIST);
    }
    if (chainBaseManager == null) {
      throw new ContractValidateException("No account store or contract store!");
    }
    AccountStore accountStore = chainBaseManager.getAccountStore();
    if (!any.is(AccountCreateContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [AccountCreateContract],real type[" + any
              .getClass() + "]");
    }
    final AccountCreateContract contract;
    try {
      contract = this.any.unpack(AccountCreateContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }
//    if (contract.getAccountName().isEmpty()) {
//      throw new ContractValidateException("AccountName is null");
//    }
    byte[] ownerAddress = contract.getOwnerAddress().toByteArray();
    if (!DecodeUtil.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid ownerAddress");
    }

    AccountCapsule accountCapsule = accountStore.get(ownerAddress);
    if (accountCapsule == null) {
      String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);
      throw new ContractValidateException(
          "Account[" + readableOwnerAddress + "] not exists");
    }

    final long fee = calcFee();
    if (accountCapsule.getBalance() < fee) {
      throw new ContractValidateException(
          "Validate CreateAccountActuator error, insufficient fee.");
    }

    byte[] accountAddress = contract.getAccountAddress().toByteArray();
    if (!DecodeUtil.addressValid(accountAddress)) {
      throw new ContractValidateException("Invalid account address");
    }

//    if (contract.getType() == null) {
//      throw new ContractValidateException("Type is null");
//    }

    if (accountStore.has(accountAddress)) {
      throw new ContractValidateException("Account has existed");
    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return any.unpack(AccountCreateContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return chainBaseManager.getDynamicPropertiesStore().getCreateNewAccountFeeInSystemContract();
  }
}
