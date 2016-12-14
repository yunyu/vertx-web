package io.vertx.webclient.impl;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.core.Handler;
import io.vertx.webclient.WebClient;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface WebClientInternal extends WebClient {

  @GenIgnore
  void addInterceptor(Handler<HttpContext<?>> interceptor);

}
