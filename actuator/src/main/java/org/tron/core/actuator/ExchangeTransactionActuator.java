package org.un.core.actuator;

import static org.un.core.config.Parameter.ChainSymbol.LBE_SYMBOL_BYTES;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Arrays;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.un.common.utils.ByteArray;
import org.un.common.utils.Commons;
import org.un.common.utils.DecodeUtil;
import org.un.common.utils.StringUtil;
import org.un.core.capsule.AccountCapsule;
import org.un.core.capsule.ExchangeCapsule;
import org.un.core.capsule.TransactionResultCapsule;
import org.un.core.exception.ContractExeException;
import org.un.core.exception.ContractValidateException;
import org.un.core.exception.ItemNotFoundException;
import org.un.core.store.AccountStore;
import org.un.core.store.AssetIssueStore;
import org.un.core.store.DynamicPropertiesStore;
import org.un.core.store.ExchangeStore;
import org.un.core.store.ExchangeV2Store;
import org.un.core.utils.TransactionUtil;
import org.un.protos.Protocol.Transaction.Contract.ContractType;
import org.un.protos.Protocol.Transaction.Result.code;
import org.un.protos.contract.ExchangeContract.ExchangeTransactionContract;

@Slf4j(topic = "actuator")
public class ExchangeTransactionActuator extends AbstractActuator {

  public ExchangeTransactionActuator() {
    super(ContractType.ExchangeTransactionContract, ExchangeTransactionContract.class);
  }

  @Override
  public boolean execute(Object object) throws ContractExeException {
    TransactionResultCapsule ret = (TransactionResultCapsule) object;
    if (Objects.isNull(ret)) {
      throw new RuntimeException(ActuatorConstant.TX_RESULT_NULL);
    }

    long fee = calcFee();
    AccountStore accountStore = chainBaseManager.getAccountStore();
    DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
    ExchangeStore exchangeStore = chainBaseManager.getExchangeStore();
    ExchangeV2Store exchangeV2Store = chainBaseManager.getExchangeV2Store();
    AssetIssueStore assetIssueStore = chainBaseManager.getAssetIssueStore();
    try {
      final ExchangeTransactionContract exchangeTransactionContract = this.any
          .unpack(ExchangeTransactionContract.class);
      AccountCapsule accountCapsule = accountStore
          .get(exchangeTransactionContract.getOwnerAddress().toByteArray());

      ExchangeCapsule exchangeCapsule = Commons
          .getExchangeStoreFinal(dynamicStore, exchangeStore, exchangeV2Store).
              get(ByteArray.fromLong(exchangeTransactionContract.getExchangeId()));

      byte[] firstTokenID = exchangeCapsule.getFirstTokenId();
      byte[] secondTokenID = exchangeCapsule.getSecondTokenId();

      byte[] tokenID = exchangeTransactionContract.getTokenId().toByteArray();
      long tokenQuant = exchangeTransactionContract.getQuant();

      byte[] anotherTokenID;
      long anotherTokenQuant = exchangeCapsule.transaction(tokenID, tokenQuant);

      if (Arrays.equals(tokenID, firstTokenID)) {
        anotherTokenID = secondTokenID;
      } else {
        anotherTokenID = firstTokenID;
      }

      long newBalance = accountCapsule.getBalance() - calcFee();
      accountCapsule.setBalance(newBalance);

      if (Arrays.equals(tokenID, LBE_SYMBOL_BYTES)) {
        accountCapsule.setBalance(newBalance - tokenQuant);
      } else {
        accountCapsule.reduceAssetAmountV2(tokenID, tokenQuant, dynamicStore, assetIssueStore);
      }

      if (Arrays.equals(anotherTokenID, LBE_SYMBOL_BYTES)) {
        accountCapsule.setBalance(newBalance + anotherTokenQuant);
      } else {
        accountCapsule
            .addAssetAmountV2(anotherTokenID, anotherTokenQuant, dynamicStore, assetIssueStore);
      }

      accountStore.put(accountCapsule.createDbKey(), accountCapsule);

      Commons.putExchangeCapsule(exchangeCapsule, dynamicStore, exchangeStore, exchangeV2Store,
          assetIssueStore);

      ret.setExchangeReceivedAmount(anotherTokenQuant);
      ret.setStatus(fee, code.SUCESS);
    } catch (ItemNotFoundException | InvalidProtocolBufferException e) {
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
      throw new ContractValidateException(ActuatorConstant.STORE_NOT_EXIST);
    }
    AccountStore accountStore = chainBaseManager.getAccountStore();
    DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
    ExchangeStore exchangeStore = chainBaseManager.getExchangeStore();
    ExchangeV2Store exchangeV2Store = chainBaseManager.getExchangeV2Store();
    if (!this.any.is(ExchangeTransactionContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [ExchangeTransactionContract],real type[" + any
              .getClass() + "]");
    }
    final ExchangeTransactionContract contract;
    try {
      contract = this.any.unpack(ExchangeTransactionContract.class);
    } catch (InvalidProtocolBufferException e) {
      throw new ContractValidateException(e.getMessage());
    }

    byte[] ownerAddress = contract.getOwnerAddress().toByteArray();
    String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);

    if (!DecodeUtil.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid address");
    }

    if (!accountStore.has(ownerAddress)) {
      throw new ContractValidateException("account[" + readableOwnerAddress + "] not exists");
    }

    AccountCapsule accountCapsule = accountStore.get(ownerAddress);

    if (accountCapsule.getBalance() < calcFee()) {
      throw new ContractValidateException("No enough balance for exchange transaction fee!");
    }

    ExchangeCapsule exchangeCapsule;
    try {
      exchangeCapsule = Commons.getExchangeStoreFinal(dynamicStore, exchangeStore, exchangeV2Store).
          get(ByteArray.fromLong(contract.getExchangeId()));
    } catch (ItemNotFoundException ex) {
      throw new ContractValidateException("Exchange[" + contract.getExchangeId() + "] not exists");
    }

    byte[] firstTokenID = exchangeCapsule.getFirstTokenId();
    byte[] secondTokenID = exchangeCapsule.getSecondTokenId();
    long firstTokenBalance = exchangeCapsule.getFirstTokenBalance();
    long secondTokenBalance = exchangeCapsule.getSecondTokenBalance();

    byte[] tokenID = contract.getTokenId().toByteArray();
    long tokenQuant = contract.getQuant();
    long tokenExpected = contract.getExpected();

    if (dynamicStore.getAllowSameTokenName() == 1 && 
        !Arrays.equals(tokenID, LBE_SYMBOL_BYTES) &&
        !TransactionUtil.isNumber(tokenID)) {
      throw new ContractValidateException("token id is not a valid number");
    }
    if (!Arrays.equals(tokenID, firstTokenID) && !Arrays.equals(tokenID, secondTokenID)) {
      throw new ContractValidateException("token is not in exchange");
    }

    if (tokenQuant <= 0) {
      throw new ContractValidateException("token quant must greater than zero");
    }

    if (tokenExpected <= 0) {
      throw new ContractValidateException("token expected must greater than zero");
    }

    if (firstTokenBalance == 0 || secondTokenBalance == 0) {
      throw new ContractValidateException("Token balance in exchange is equal with 0,"
          + "the exchange has been closed");
    }

    long balanceLimit = dynamicStore.getExchangeBalanceLimit();
    long tokenBalance = (Arrays.equals(tokenID, firstTokenID) ? firstTokenBalance
        : secondTokenBalance);
    tokenBalance += tokenQuant;
    if (tokenBalance > balanceLimit) {
      throw new ContractValidateException("token balance must less than " + balanceLimit);
    }

    if (Arrays.equals(tokenID, LBE_SYMBOL_BYTES)) {
      if (accountCapsule.getBalance() < (tokenQuant + calcFee())) {
        throw new ContractValidateException("balance is not enough");
      }
    } else {
      if (!accountCapsule.assetBalanceEnoughV2(tokenID, tokenQuant, dynamicStore)) {
        throw new ContractValidateException("token balance is not enough");
      }
    }

    long anotherTokenQuant = exchangeCapsule.transaction(tokenID, tokenQuant);
    if (anotherTokenQuant < tokenExpected) {
      throw new ContractValidateException("token required must greater than expected");
    }

    return true;
  }


  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return any.unpack(ExchangeTransactionContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }

}
