package org.un.core.exception;

public class LbeRuntimeException extends RuntimeException {

  public LbeRuntimeException() {
    super();
  }

  public LbeRuntimeException(String message) {
    super(message);
  }

  public LbeRuntimeException(String message, Throwable cause) {
    super(message, cause);
  }

  public LbeRuntimeException(Throwable cause) {
    super(cause);
  }

  protected LbeRuntimeException(String message, Throwable cause,
      boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }


}
