package org.un.core.db;

import lombok.Data;
import org.un.common.runtime.ProgramResult;
import org.un.core.capsule.BlockCapsule;
import org.un.core.capsule.TransactionCapsule;
import org.un.core.store.StoreFactory;

@Data
public class TransactionContext {

  private BlockCapsule blockCap;
  private TransactionCapsule trxCap;
  private StoreFactory storeFactory;
  private ProgramResult programResult = new ProgramResult();
  private boolean isStatic;
  private boolean eventPluginLoaded;

  public TransactionContext(BlockCapsule blockCap, TransactionCapsule trxCap,
      StoreFactory storeFactory,
      boolean isStatic,
      boolean eventPluginLoaded) {
    this.blockCap = blockCap;
    this.trxCap = trxCap;
    this.storeFactory = storeFactory;
    this.isStatic = isStatic;
    this.eventPluginLoaded = eventPluginLoaded;
  }
}
