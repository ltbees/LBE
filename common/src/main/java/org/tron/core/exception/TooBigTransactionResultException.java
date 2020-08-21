package org.un.core.exception;

public class TooBigTransactionResultException extends LbeException {

  public TooBigTransactionResultException() {
    super("too big transaction result");
  }

  public TooBigTransactionResultException(String message) {
    super(message);
  }
}
