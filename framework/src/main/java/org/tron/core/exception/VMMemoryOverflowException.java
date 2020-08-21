package org.un.core.exception;

public class VMMemoryOverflowException extends LbeException {

  public VMMemoryOverflowException() {
    super("VM memory overflow");
  }

  public VMMemoryOverflowException(String message) {
    super(message);
  }

}
