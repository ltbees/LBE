package org.un.core.actuator;

import org.un.core.exception.ContractExeException;
import org.un.core.exception.ContractValidateException;

public interface Actuator2 {

  void execute(Object object) throws ContractExeException;

  void validate(Object object) throws ContractValidateException;
}