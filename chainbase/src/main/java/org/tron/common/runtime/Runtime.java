package org.un.common.runtime;

import org.un.core.db.TransactionContext;
import org.un.core.exception.ContractExeException;
import org.un.core.exception.ContractValidateException;


public interface Runtime {

  void execute(TransactionContext context)
      throws ContractValidateException, ContractExeException;

  ProgramResult getResult();

  String getRuntimeError();

}
