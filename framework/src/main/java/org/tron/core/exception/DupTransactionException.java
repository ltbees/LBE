package org.un.core.exception;

public class DupTransactionException extends LbeException {

  public DupTransactionException() {
    super();
  }

  public DupTransactionException(String message) {
    super(message);
  }
}
