package io.vertx.webclient.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringEncoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.VertxException;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.Pump;
import io.vertx.core.streams.ReadStream;
import io.vertx.webclient.BodyCodec;
import io.vertx.webclient.HttpRequest;
import io.vertx.webclient.HttpResponse;
import io.vertx.webclient.spi.BodyStream;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class HttpContext<R> implements Handler<AsyncResult<HttpClientResponse>> {

  private static final Iterator<Handler<HttpContext<?>>> EMPTY_INTERCEPTORS = Collections.emptyIterator();

  final WebClientImpl client;
  private BodyCodec<R> codec;
  private Handler<AsyncResult<HttpResponse<R>>> responseHandler;
  private HttpRequest request;
  private String contentType;
  private Object body;
  private Iterator<Handler<HttpContext<?>>> it;
  private Map<String, Object> payload;

  public HttpContext(WebClientImpl client,
                     HttpRequest request,
                     String contentType,
                     Object body,
                     BodyCodec<R> codec,
                     Handler<AsyncResult<HttpResponse<R>>> responseHandler) {
    this.client = client;
    this.request = request;
    this.contentType = contentType;
    this.body = body;
    this.codec = codec;
    this.responseHandler = responseHandler;
  }

  public HttpRequest getRequest() {
    return request;
  }

  public void setRequest(HttpRequest request) {
    this.request = request;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public Object getBody() {
    return body;
  }

  public void setBody(Object body) {
    this.body = body;
  }

  public BodyCodec<R> getCodec() {
    return codec;
  }

  public void setCodec(BodyCodec<R> codec) {
    this.codec = codec;
  }

  public Handler<AsyncResult<HttpResponse<R>>> getResponseHandler() {
    return responseHandler;
  }

  public void setResponseHandler(Handler<AsyncResult<HttpResponse<R>>> responseHandler) {
    this.responseHandler = responseHandler;
  }

  @Override
  public void handle(AsyncResult<HttpClientResponse> ar) {
    if (ar.succeeded()) {
      HttpClientResponse resp = ar.result();
      Future<HttpResponse<R>> fut = Future.future();
      fut.setHandler(responseHandler);
      resp.exceptionHandler(err -> {
        if (!fut.isComplete()) {
          fut.fail(err);
        }
      });
      resp.pause();
      codec.create(ar2 -> {
        resp.resume();
        if (ar2.succeeded()) {
          BodyStream<R> stream = ar2.result();
          stream.exceptionHandler(err -> {
            if (!fut.isComplete()) {
              fut.fail(err);
            }
          });
          resp.endHandler(v -> {
            if (!fut.isComplete()) {
              stream.end();
              if (stream.result().succeeded()) {
                fut.complete(new HttpResponseImpl<>(resp, null, stream.result().result()));
              } else {
                fut.fail(stream.result().cause());
              }
            }
          });
          Pump responsePump = Pump.pump(resp, stream);
          responsePump.start();
        } else {
          responseHandler.handle(Future.failedFuture(ar2.cause()));
        }
      });
    } else {
      responseHandler.handle(Future.failedFuture(ar.cause()));
    }
  }

  /**
   * Send the HTTP request, the context will traverse all interceptors. Any interceptor chain on the context
   * will be reset.
   */
  public void send() {
    it = client.interceptors.size() > 0 ? client.interceptors.iterator() : EMPTY_INTERCEPTORS;
    next();
  }

  /**
   * Call the next interceptor in the chain or send the request when the end of the chain is reached.
   */
  public void next() {
    if (it.hasNext()) {
      Handler<HttpContext<?>> next = it.next();
      next.handle(this);
    } else {
      Future<HttpClientResponse> fut = Future
        .<HttpClientResponse>future()
        .setHandler(this);
      send(fut);
    }
  }

  private void send(Future<HttpClientResponse> responseFuture) {
    HttpRequestImpl bilto = (HttpRequestImpl) request;
    HttpClientRequest req;
    String requestURI;
    MultiMap params = bilto.params;
    MultiMap headers = bilto.headers;
    String uri = bilto.uri;
    int port = bilto.port;
    long timeout = bilto.timeout;
    String host = bilto.host;
    HttpMethod method = bilto.method;
    if (params != null && params.size() > 0) {
      QueryStringEncoder enc = new QueryStringEncoder(uri);
      params.forEach(param -> {
        enc.addParam(param.getKey(), param.getValue());
      });
      requestURI = enc.toString();
    } else {
      requestURI = uri;
    }
    if (port != -1) {
      if (host != null) {
        req = client.httpClient.request(method, port, host, requestURI);
      } else {
        throw new IllegalStateException("Both host and port must be set with an explicit port");
      }
    } else {
      if (host != null) {
        req = client.httpClient.request(method, host, requestURI);
      } else {
        req = client.httpClient.request(method, requestURI);
      }
    }
    if (headers != null) {
      req.headers().addAll(headers);
    }
    req.exceptionHandler(err -> {
      if (!responseFuture.isComplete()) {
        responseFuture.fail(err);
      }
    });
    req.handler(resp -> {
      if (!responseFuture.isComplete()) {
        responseFuture.complete(resp);
      }
    });
    if (timeout > 0) {
      req.setTimeout(timeout);
    }
    if (body != null) {
      if (contentType != null) {
        String prev = req.headers().get(HttpHeaders.CONTENT_TYPE);
        if (prev == null) {
          req.putHeader(HttpHeaders.CONTENT_TYPE, contentType);
        } else {
          contentType = prev;
        }
      }
      if (body instanceof ReadStream<?>) {
        ReadStream<Buffer> stream = (ReadStream<Buffer>) body;
        if (headers == null || !headers.contains(HttpHeaders.CONTENT_LENGTH)) {
          req.setChunked(true);
        }
        Pump pump = Pump.pump(stream, req);
        stream.exceptionHandler(err -> {
          req.reset();
          if (!responseFuture.isComplete()) {
            responseFuture.fail(err);
          }
        });
        stream.endHandler(v -> {
          pump.stop();
          req.end();
        });
        pump.start();
      } else {
        Buffer buffer;
        if (body instanceof Buffer) {
          buffer = (Buffer) body;
        } else if (body instanceof MultiMap) {
          try {
            MultiMap attributes = (MultiMap) body;
            boolean multipart = "multipart/form-data".equals(contentType);
            DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, io.netty.handler.codec.http.HttpMethod.POST, "/");
            HttpPostRequestEncoder encoder = new HttpPostRequestEncoder(request, multipart);
            for (Map.Entry<String, String> attribute : attributes) {
              encoder.addBodyAttribute(attribute.getKey(), attribute.getValue());
            }
            encoder.finalizeRequest();
            for (String headerName : request.headers().names()) {
              req.putHeader(headerName, request.headers().get(headerName));
            }
            if (encoder.isChunked()) {
              buffer = Buffer.buffer();
              while (true) {
                HttpContent chunk = encoder.readChunk(new UnpooledByteBufAllocator(false));
                ByteBuf content = chunk.content();
                if (content.readableBytes() == 0) {
                  break;
                }
                buffer.appendBuffer(Buffer.buffer(content));
              }
            } else {
              ByteBuf content = request.content();
              buffer = Buffer.buffer(content);
            }
          } catch (Exception e) {
            throw new VertxException(e);
          }
        } else if (body instanceof JsonObject) {
          buffer = Buffer.buffer(((JsonObject)body).encode());
        } else {
          buffer = Buffer.buffer(Json.encode(body));
        }
        req.end(buffer);
      }
    } else {
      req.end();
    }
  }

  public <T> T getPayload(String key) {
    return payload != null ? (T) payload.get(key) : null;
  }

  public void setPayload(String key, Object value) {
    if (value == null) {
      if (payload != null) {
        payload.remove(key);
      }
    } else {
      if (payload == null) {
        payload = new HashMap<>();
      }
      payload.put(key, value);
    }
  }
}
