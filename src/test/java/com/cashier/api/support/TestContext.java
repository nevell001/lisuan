package com.cashier.api.support;

import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import io.javalin.validation.Validator;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Lightweight Context proxy for controller and middleware unit tests. */
public final class TestContext {
    private final Map<String, Object> attributes = new HashMap<>();
    private final Map<String, String> headers = new HashMap<>();
    private final Map<String, String> queryParams = new HashMap<>();
    private final Map<String, String> pathParams = new HashMap<>();
    private Object body;
    private RuntimeException bodyParseFailure;
    private HandlerType method = HandlerType.GET;
    private String path = "/";

    public HttpStatus status;
    public Object json;
    public boolean skipped;
    public final Context context;

    public TestContext() {
        context = (Context) Proxy.newProxyInstance(
            Context.class.getClassLoader(),
            new Class<?>[]{Context.class},
            (proxy, reflectedMethod, args) -> {
                String name = reflectedMethod.getName();
                if (name.equals("attribute")) {
                    if (args.length == 2) {
                        attributes.put((String) args[0], args[1]);
                        return null;
                    }
                    return attributes.get(args[0]);
                }
                if (name.equals("header") && args.length == 1) {
                    return headers.get(args[0]);
                }
                if (name.equals("queryParam") && args.length == 1) {
                    return queryParams.get(args[0]);
                }
                if (name.equals("queryParamAsClass") && args.length == 2) {
                    return validatorFor((String) args[0], (Class<?>) args[1], queryParams);
                }
                if (name.equals("pathParam") && args.length == 1) {
                    return pathParams.get(args[0]);
                }
                if (name.equals("pathParamAsClass") && args.length == 2) {
                    return validatorFor((String) args[0], (Class<?>) args[1], pathParams);
                }
                if (name.equals("bodyAsClass") && args.length == 1) {
                    if (bodyParseFailure != null) {
                        throw bodyParseFailure;
                    }
                    return body;
                }
                if (name.equals("method") || name.equals("handlerType")) {
                    return method;
                }
                if (name.equals("path")) {
                    return path;
                }
                if (name.equals("status") && args != null && args.length == 1) {
                    status = args[0] instanceof HttpStatus httpStatus
                        ? httpStatus
                        : HttpStatus.forStatus((Integer) args[0]);
                    return proxy;
                }
                if (name.equals("json")) {
                    if (status == null) {
                        status = HttpStatus.OK;
                    }
                    json = args[0];
                    return proxy;
                }
                if (name.equals("skipRemainingHandlers")) {
                    skipped = true;
                    return proxy;
                }
                if (name.equals("toString")) {
                    return "TestContext";
                }
                if (name.equals("hashCode")) {
                    return System.identityHashCode(proxy);
                }
                if (name.equals("equals")) {
                    return proxy == args[0];
                }
                return defaultValue(reflectedMethod.getReturnType());
            }
        );
    }

    public TestContext withAttribute(String name, Object value) {
        attributes.put(name, value);
        return this;
    }

    public TestContext withHeader(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public TestContext withQueryParam(String name, String value) {
        queryParams.put(name, value);
        return this;
    }

    public TestContext withPathParam(String name, String value) {
        pathParams.put(name, value);
        return this;
    }

    public TestContext withBody(Object value) {
        this.body = value;
        return this;
    }

    /**
     * 让 {@code ctx.bodyAsClass(...)} 抛出指定异常，用于验证"请求体写错"的响应码。
     *
     * <p>真实 Javalin/Jackson 遇到未知字段、类型不符、空请求体时就是这么抛的
     * （{@code UnrecognizedPropertyException} / {@code BadRequestResponse}）。</p>
     */
    public TestContext withBodyParseFailure(RuntimeException failure) {
        this.bodyParseFailure = failure;
        return this;
    }

    public TestContext withRequest(HandlerType method, String path) {
        this.method = method;
        this.path = path;
        return this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Validator<?> validatorFor(String name, Class<?> clazz, Map<String, String> source) {
        String raw = source.get(name);
        Validator validator = mock(Validator.class);
        when(validator.get()).thenAnswer(inv -> raw == null ? null : convert(raw, clazz));
        when(validator.getOrDefault(any())).thenAnswer(inv -> raw == null ? inv.getArgument(0) : convert(raw, clazz));
        return validator;
    }

    private static Object convert(String raw, Class<?> clazz) {
        if (clazz == Integer.class) return Integer.valueOf(raw);
        if (clazz == Long.class) return Long.valueOf(raw);
        if (clazz == String.class) return raw;
        throw new IllegalArgumentException("Unsupported param type: " + clazz);
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        if (type == char.class) return '\0';
        return null;
    }
}
