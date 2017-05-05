package io.vertx.ext.web.handler.impl;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class TimeoutBodyEndHandler implements Handler<Void> {
  private RoutingContext ctx;
  private long tid;

  public TimeoutBodyEndHandler(RoutingContext ctx, long tid) {
    this.ctx = ctx;
    this.tid = tid;
  }

  @Override
  public void handle(Void v) {
    ctx.vertx().cancelTimer(tid);
  }
}
