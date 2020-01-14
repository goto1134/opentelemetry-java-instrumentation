package io.opentelemetry.auto.instrumentation.aws.v0;

import static io.opentelemetry.auto.instrumentation.api.AgentTracer.activateSpan;
import static io.opentelemetry.auto.instrumentation.api.AgentTracer.startSpan;
import static io.opentelemetry.auto.instrumentation.aws.v0.AwsSdkClientDecorator.DECORATE;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.Request;
import com.amazonaws.Response;
import com.amazonaws.handlers.HandlerContextKey;
import com.amazonaws.handlers.RequestHandler2;
import io.opentelemetry.auto.instrumentation.api.AgentScope;
import io.opentelemetry.auto.instrumentation.api.AgentSpan;

/** Tracing Request Handler */
public class TracingRequestHandler extends RequestHandler2 {
  public static TracingRequestHandler INSTANCE = new TracingRequestHandler();

  // Note: aws1.x sdk doesn't have any truly async clients so we can store scope in request context
  // safely.
  public static final HandlerContextKey<AgentScope> SCOPE_CONTEXT_KEY =
      new HandlerContextKey<>("io.opentelemetry.auto.Scope");

  @Override
  public AmazonWebServiceRequest beforeMarshalling(final AmazonWebServiceRequest request) {
    return request;
  }

  @Override
  public void beforeRequest(final Request<?> request) {
    final AgentSpan span = startSpan("aws.http");
    DECORATE.afterStart(span);
    DECORATE.onRequest(span, request);
    request.addHandlerContext(SCOPE_CONTEXT_KEY, activateSpan(span, true));
  }

  @Override
  public void afterResponse(final Request<?> request, final Response<?> response) {
    final AgentScope scope = request.getHandlerContext(SCOPE_CONTEXT_KEY);
    if (scope != null) {
      request.addHandlerContext(SCOPE_CONTEXT_KEY, null);
      DECORATE.onResponse(scope.span(), response);
      DECORATE.beforeFinish(scope.span());
      scope.close();
    }
  }

  @Override
  public void afterError(final Request<?> request, final Response<?> response, final Exception e) {
    final AgentScope scope = request.getHandlerContext(SCOPE_CONTEXT_KEY);
    if (scope != null) {
      request.addHandlerContext(SCOPE_CONTEXT_KEY, null);
      DECORATE.onError(scope.span(), e);
      DECORATE.beforeFinish(scope.span());
      scope.close();
    }
  }
}