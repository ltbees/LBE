package org.un.core.services.ratelimiter.adapter;

import org.un.core.services.ratelimiter.RuntimeData;

public interface IRateLimiter {

  boolean acquire(RuntimeData data);

}
