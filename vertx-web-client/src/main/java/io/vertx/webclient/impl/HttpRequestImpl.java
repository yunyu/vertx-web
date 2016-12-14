/*
 * Copyright (c) 2011-2013 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 *     The Eclipse Public License is available at
 *     http://www.eclipse.org/legal/epl-v10.html
 *
 *     The Apache License v2.0 is available at
 *     http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */
package io.vertx.webclient.impl;

import io.netty.handler.codec.http.QueryStringDecoder;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import io.vertx.rxjava.webclient.WebClient;
import io.vertx.webclient.BodyCodec;
import io.vertx.webclient.HttpRequest;
import io.vertx.webclient.HttpResponse;

import java.util.Iterator;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
class HttpRequestImpl implements HttpRequest {

  final WebClientImpl client;
  MultiMap params;
  HttpMethod method;
  int port = -1;
  String host;
  String uri;
  MultiMap headers;
  long timeout = -1;

  HttpRequestImpl(WebClientImpl client, HttpMethod method) {
    this.client = client;
    this.method = method;
  }

  private HttpRequestImpl(HttpRequestImpl other) {
    this.client = other.client;
    this.method = other.method;
    this.port = other.port;
    this.host = other.host;
    this.timeout = other.timeout;
    this.uri = other.uri;
    this.headers = other.headers != null ? new CaseInsensitiveHeaders().addAll(other.headers) : null;
    this.params = other.params != null ? new CaseInsensitiveHeaders().addAll(other.params) : null;
  }

  @Override
  public HttpRequest method(HttpMethod value) {
    method = value;
    return this;
  }

  @Override
  public HttpRequest port(int value) {
    port = value;
    return this;
  }

  @Override
  public HttpRequest host(String value) {
    host = value;
    return this;
  }

  @Override
  public HttpRequest uri(String value) {
    params = null;
    uri = value;
    return this;
  }

  @Override
  public HttpRequest putHeader(String name, String value) {
    headers().set(name, value);
    return this;
  }

  @Override
  public MultiMap headers() {
    if (headers == null) {
      headers = new CaseInsensitiveHeaders();
    }
    return headers;
  }

  @Override
  public HttpRequest timeout(long value) {
    timeout = value;
    return this;
  }

  @Override
  public HttpRequest addQueryParam(String paramName, String paramValue) {
    queryParams().add(paramName, paramValue);
    return this;
  }

  @Override
  public HttpRequest setQueryParam(String paramName, String paramValue) {
    queryParams().set(paramName, paramValue);
    return this;
  }

  @Override
  public MultiMap queryParams() {
    if (params == null) {
      params = new CaseInsensitiveHeaders();
    }
    if (params.isEmpty()) {
      int idx = uri.indexOf('?');
      if (idx >= 0) {
        QueryStringDecoder dec = new QueryStringDecoder(uri);
        dec.parameters().forEach((name, value) -> params.add(name, value));
        uri = uri.substring(0, idx);
      }
    }
    return params;
  }

  @Override
  public HttpRequest copy() {
    return new HttpRequestImpl(this);
  }

  @Override
  public void sendStream(ReadStream<Buffer> body, Handler<AsyncResult<HttpResponse<Buffer>>> handler) {
    send(null, body, BodyCodec.buffer(), handler);
  }

  @Override
  public <R> void sendStream(ReadStream<Buffer> body, BodyCodec<R> responseCodec, Handler<AsyncResult<HttpResponse<R>>> handler) {
    send(null, body, responseCodec, handler);
  }

  @Override
  public void send(Handler<AsyncResult<HttpResponse<Buffer>>> handler) {
    send(null, null, BodyCodec.buffer(), handler);
  }

  @Override
  public <R> void send(BodyCodec<R> responseCodec, Handler<AsyncResult<HttpResponse<R>>> handler) {
    send(null, null, responseCodec, handler);
  }

  @Override
  public void sendBuffer(Buffer body, Handler<AsyncResult<HttpResponse<Buffer>>> handler) {
    send(null, body, BodyCodec.buffer(), handler);
  }

  @Override
  public <R> void sendBuffer(Buffer body, BodyCodec<R> responseCodec, Handler<AsyncResult<HttpResponse<R>>> handler) {
    send(null, body, responseCodec, handler);
  }

  @Override
  public void sendJsonObject(JsonObject body, Handler<AsyncResult<HttpResponse<Buffer>>> handler) {
    send("application/json", body, BodyCodec.buffer(), handler);
  }

  @Override
  public <R> void sendJsonObject(JsonObject body, BodyCodec<R> responseCodec, Handler<AsyncResult<HttpResponse<R>>> handler) {
    send("application/json", body, responseCodec, handler);
  }

  @Override
  public void sendJson(Object body, Handler<AsyncResult<HttpResponse<Buffer>>> handler) {
    send("application/json", body, BodyCodec.buffer(), handler);
  }

  @Override
  public <R> void sendJson(Object body, BodyCodec<R> responseCodec, Handler<AsyncResult<HttpResponse<R>>> handler) {
    send("application/json", body, responseCodec, handler);
  }

  @Override
  public void sendForm(MultiMap body, Handler<AsyncResult<HttpResponse<Buffer>>> handler) {
    send("application/x-www-form-urlencoded", body, BodyCodec.buffer(), handler);
  }

  @Override
  public <R> void sendForm(MultiMap body, BodyCodec<R> responseCodec, Handler<AsyncResult<HttpResponse<R>>> handler) {
    send("application/x-www-form-urlencoded", body, responseCodec, handler);
  }

  private <R> void send(String contentType, Object body, BodyCodec<R> codec, Handler<AsyncResult<HttpResponse<R>>> handler) {
    HttpContext<R> ex = new HttpContext<>(client, this, contentType, body, codec, handler);
    ex.send();
  }
}
