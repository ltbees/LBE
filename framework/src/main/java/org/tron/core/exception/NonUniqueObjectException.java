package org.un.core.exception;

public class NonLbeiqueObjectException extends LbeException {

  public NonLbeiqueObjectException() {
    super();
  }

  public NonLbeiqueObjectException(String s) {
    super(s);
  }

  public NonLbeiqueObjectException(String message, Throwable cause) {
    super(message, cause);
  }

  public NonLbeiqueObjectException(Throwable cause) {
    super("", cause);
  }
}
