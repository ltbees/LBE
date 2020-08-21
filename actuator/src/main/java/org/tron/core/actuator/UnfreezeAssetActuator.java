package org.un.core.actuator;

import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.un.common.utils.DecodeUtil;
import org.un.common.utils.StringUtil;
import org.un.core.capsule.AccountCapsule;
import org.un.core.capsule.TransactionResultCapsule;
import org.un.core.exception.ContractExeException;
import org.un.core.exception.ContractValidateException;
import org.un.core.store.AccountStore;
import org.un.core.store.AssetIssueStore;
import org.un.core.store.DynamicPropertiesStore;
import org.un.protos.Protocol.Account.Frozen;
import org.un.protos.Protocol.Transaction.Contract.ContractType;
import org.un.protos.Protocol.Transaction.Result.code;
import org.un.protos.contract.AssetIssueContractOuterClass.LbefreezeAssetContract;

@Slf4j(topic = "actuator")
public class LbefreezeAssetActuator extends AbstractActuator {

  public LbefreezeAssetActuator() {
    super(ContractType.LbefreezeAssetContract, LbefreezeAssetContract.class);
  }

  @Override
  public boolean execute(Object object) throws ContractExeException {
    TransactionResultCapsule ret = (TransactionResultCapsule) object;
    if (Objects.isNull(ret)) {
      throw new RuntimeException(ActuatorConstant.TX_RESULT_NULL);
    }

    long fee = calcFee();
    AccountStore accountStore = chainBaseManager.getAccountStore();
    AssetIssueStore assetIssueStore = chainBaseManager.getAssetIssueStore();
    DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
    try {
      final LbefreezeAssetContract unfreezeAssetContract = any.unpack(LbefreezeAssetContract.class);
      byte[] ownerAddress = unfreezeAssetContract.getOwnerAddress().toByteArray();

      AccountCapsule accountCapsule = accountStore.get(ownerAddress);
      long unfreezeAsset = 0L;
      List<Frozen> frozenList = Lists.newArrayList();
      frozenList.addAll(accountCapsule.getFrozenSupplyList());
      Iterator<Frozen> iterator = frozenList.iterator();
      long now = dynamicStore.getLatestBlockHeaderTimestamp();
      while (iterator.hasNext()) {
        Frozen next = iterator.next();
        if (next.getExpireTime() <= now) {
          unfreezeAsset += next.getFrozenBalance();
          iterator.remove();
        }
      }

      if (dynamicStore.getAllowSameTokenName() == 0) {
        accountCapsule
            .addAssetAmountV2(accountCapsule.getAssetIssuedName().toByteArray(), unfreezeAsset,
                dynamicStore, assetIssueStore);
      } else {
        accountCapsule
            .addAssetAmountV2(accountCapsule.getAssetIssuedID().toByteArray(), unfreezeAsset,
                dynamicStore, assetIssueStore);
      }

      accountCapsule.setInstance(accountCapsule.getInstance().toBuilder()
          .clearFrozenSupply().addAllFrozenSupply(frozenList).build());

      accountStore.put(ownerAddress, accountCapsule);
      ret.setStatus(fee, code.SUCESS);
    } catch (InvalidProtocolBufferException | ArithmeticException e) {
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
    if (!this.any.is(LbefreezeAssetContract.class)) {
      throw new ContractValidateException(
          "contract type error, expected type [LbefreezeAssetContract], real type[" + any
              .getClass() + "]");
    }
    final LbefreezeAssetContract unfreezeAssetContract;
    try {
      unfreezeAssetContract = this.any.unpack(LbefreezeAssetContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }
    byte[] ownerAddress = unfreezeAssetContract.getOwnerAddress().toByteArray();
    if (!DecodeUtil.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid address");
    }

    AccountCapsule accountCapsule = accountStore.get(ownerAddress);
    if (accountCapsule == null) {
      String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);
      throw new ContractValidateException(
          "Account[" + readableOwnerAddress + "] does not exist");
    }

    if (accountCapsule.getFrozenSupplyCount() <= 0) {
      throw new ContractValidateException("no frozen supply balance");
    }

    if (dynamicStore.getAllowSameTokenName() == 0) {
      if (accountCapsule.getAssetIssuedName().isEmpty()) {
        throw new ContractValidateException("this account has not issued any asset");
      }
    } else {
      if (accountCapsule.getAssetIssuedID().isEmpty()) {
        throw new ContractValidateException("this account has not issued any asset");
      }
    }

    long now = dynamicStore.getLatestBlockHeaderTimestamp();
    long allowedLbefreezeCount = accountCapsule.getFrozenSupplyList().stream()
        .filter(frozen -> frozen.getExpireTime() <= now).count();
    if (allowedLbefreezeCount <= 0) {
      throw new ContractValidateException("It's not time to unfreeze asset supply");
    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return any.unpack(LbefreezeAssetContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }

}
