package org.un.core.capsule;

public interface ProtoCapsule<T> {

  byte[] getData();

  T getInstance();
}
