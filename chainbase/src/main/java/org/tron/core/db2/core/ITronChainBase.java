package org.un.core.db2.core;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Map.Entry;
import org.un.common.utils.Quitable;
import org.un.core.exception.BadItemException;
import org.un.core.exception.ItemNotFoundException;

public interface ILbeChainBase<T> extends Iterable<Entry<byte[], T>>, Quitable {

  /**
   * reset the database.
   */
  void reset();

  /**
   * close the database.
   */
  void close();

  void put(byte[] key, T item);

  void delete(byte[] key);

  T get(byte[] key) throws InvalidProtocolBufferException, ItemNotFoundException, BadItemException;

  T getLbechecked(byte[] key);

  boolean has(byte[] key);

  String getName();

  String getDbName();

}
