package com.openfeign;

import feign.*;
import feign.codec.DecodeException;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.jaxb.JAXBContextFactory;
import feign.jaxb.JAXBDecoder;
import feign.jaxb.JAXBEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.*;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientFactory {
    private static final Logger logger = LoggerFactory.getLogger(ClientFactory.class);
    private Integer connectTimeoutMillis;
    private Integer readTimeoutMillis;
    private String defaultBaseUrl;
    private ConcurrentHashMap<String, Object> clientMap;
    private boolean allowRequestLog;
    private boolean allowResponseLog;
    private Decoder decoder;
    private Encoder encoder;

    public static class Builder {
        private Integer connectTimeoutMillis;
        private Integer readTimeoutMillis;
        private String defaultBaseUrl;
        private boolean allowRequestLog;
        private boolean allowResponseLog;

        public static Builder getInstance() {
            return new Builder();
        }

        public Builder() {
            this.connectTimeoutMillis = 10 * 1000;
            this.readTimeoutMillis = 60 * 1000;
            this.defaultBaseUrl = System.getProperty("service.url");
        }

        public Builder connectTimeoutMillis(int connectTimeoutMillis) {
            this.connectTimeoutMillis = connectTimeoutMillis;
            return this;
        }

        public Builder readTimeoutMillis(int readTimeoutMillis) {
            this.readTimeoutMillis = readTimeoutMillis;
            return this;
        }

        public Builder defaultBaseUrl(String defaultBaseUrl) {
            this.defaultBaseUrl = defaultBaseUrl;
            return this;
        }

        public Builder allowRequestLog(boolean allowRequestLog) {
            this.allowRequestLog = allowRequestLog;
            return this;
        }

        public Builder allowResponseLog(boolean allowResponseLog) {
            this.allowResponseLog = allowResponseLog;
            return this;
        }

        public ClientFactory build() {
            return new ClientFactory(connectTimeoutMillis, readTimeoutMillis, defaultBaseUrl, allowRequestLog, allowResponseLog);
        }
    }

    public class ClientBuilder extends Feign.Builder {
        @Override
        public <T> T target(Target<T> target) {
            init();
            return (T) createProxyClient(super.target(target));
        }

        @Override
        public <T> T target(Class<T> apiType, String url) {
            if (url == null) {
                url = defaultBaseUrl;
            }
            init();
            return (T) createProxyClient(super.target(apiType, url));
        }

        private void init() {
            if (connectTimeoutMillis != null || readTimeoutMillis != null) {
                Request.Options options = new Request.Options();
                super.options(new Request.Options(connectTimeoutMillis == null ? options.connectTimeoutMillis() : connectTimeoutMillis,
                        readTimeoutMillis == null ? options.readTimeoutMillis() : readTimeoutMillis));
            }

            if (decoder == null) {
                decoder = new Decoder.Default();
                decoder(decoder);
            }

            super.errorDecoder(createErrorDecode());
        }

        @Override
        public Feign.Builder decoder(Decoder decoder) {
            return super.decoder(createDecoder(decoder));
        }
    }

    public static Builder Builder() {
        return new Builder();
    }

    private ClientFactory(int connectTimeoutMillis, int readTimeoutMillis, String defaultBaseUrl, boolean allowRequestLog, boolean allowResponseLog) {
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
        this.defaultBaseUrl = defaultBaseUrl;
        this.allowRequestLog = allowRequestLog;
        this.allowResponseLog = allowResponseLog;
        this.clientMap = new ConcurrentHashMap<>();
    }

    public Feign.Builder clientBuilder() {
        return new ClientBuilder();
    }

    public <T> T createJsonClient(Class<T> tClass, String baseUrl) {
        return createClient(tClass, baseUrl, new GsonEncoder(), new GsonDecoder());
    }

    public <T> T createXmlClient(Class<T> tClass, String baseUrl) {
        JAXBContextFactory jaxbFactory = new JAXBContextFactory.Builder()
                .withMarshallerJAXBEncoding("UTF-8")
                .withMarshallerSchemaLocation("http://apihost http://apihost/schema.xsd")
                .build();
        return createClient(tClass, baseUrl, new JAXBEncoder(jaxbFactory), new JAXBDecoder(jaxbFactory));
    }

    public <T> T createClient(Class<T> tClass, String baseUrl, Encoder encoder, Decoder decoder) {
        if (tClass == null || encoder == null || decoder == null) {
            logger.error("[tClass:" + tClass + ",encoder:" + encoder + ",decoder:" + decoder + "]");
            throw new NullPointerException("[tClass:" + tClass + ",encoder:" + encoder + ",decoder:" + decoder + "]");
        }

        String key = tClass.getTypeName() + ";" + baseUrl + ";" + encoder.getClass() + ";" + decoder.getClass();
        if (clientMap.contains(key)) {
            return (T) clientMap.get(key);
        }

        this.encoder = encoder;
        this.decoder = decoder;

        T client = clientBuilder()
                .encoder(encoder)
                .decoder(decoder)
                .target(tClass, baseUrl);
        clientMap.put(key, client);

        return client;
    }

    private ErrorDecoder createErrorDecode() {
        return new ErrorDecoder() {
            @Override
            public Exception decode(String methodKey, Response response) {
                Request request = response.request();
                String resBody = decoderToString(response.body());
                response = Response.builder()
                        .request(response.request())
                        .status(response.status())
                        .headers(response.headers())
                        .reason(response.reason())
                        .body(resBody, Charset.forName("utf-8"))
                        .build();

                httpLog(response, resBody, true);

                return new CallApiException(response);
            }
        };
    }

    private Decoder createDecoder(Decoder decoder) {
        return new Decoder() {
            @Override
            public Object decode(Response response, Type type) throws IOException, DecodeException, FeignException {
                Object result = null;
                Object body = null;
                try {
                    Class clazz;
                    if (type instanceof ParameterizedType) {
                        clazz = (Class) ((ParameterizedType) type).getRawType();
                    } else {
                        clazz = Class.forName(type.getTypeName());
                    }
                    if (BaseResponse.class.isAssignableFrom(clazz)) {
                        result = decoderToBaseResponse(response, type, clazz, false);
                        body = ((BaseResponse) result).getData();
                    } else if (String.class.equals(type)) {
                        body = result = decoderToString(response.body());
                    } else {
                        body = result = decoder.decode(response, type);
                    }
                } catch (Throwable throwable) {
                    logger.error(throwable.getMessage());
                }

                httpLog(response, body, false);

                return result;
            }
        };
    }

    private Object createProxyClient(Object client) {
        return Proxy.newProxyInstance(getClass().getClassLoader(), client.getClass().getInterfaces(),
                new InvocationHandler() {

                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        Object result = null;
                        Throwable throwable = null;
                        try {
                            result = method.invoke(client, args);
                        } catch (InvocationTargetException e) {
                            throwable = e.getTargetException();
                        }
                        return handleResult(result, method, throwable);
                    }

                    private Object handleResult(Object result, Method method, Throwable throwable) throws Throwable {
                        Class returnType = method.getReturnType();
                        if (throwable != null) {
                            if (throwable instanceof CallApiException && BaseResponse.class.isAssignableFrom(returnType)) {
                                Response response = ((CallApiException) throwable).getCopyResponse();
                                try {
                                    return decoderToBaseResponse(response, method.getGenericReturnType(), returnType, true);
                                } finally {
                                    response.close();
                                }
                            }
                            throw throwable;
                        }

                        if (result == null && BaseResponse.class.isAssignableFrom(returnType)) {
                            result = returnType.newInstance();
                            ((BaseResponse) result).setStatus(200);
                        }
                        return result;
                    }

                });
    }

    private Type getActualTypeArgument(Type type, int index) {
        if (type instanceof ParameterizedType) {
            Type[] types = ((ParameterizedType) type).getActualTypeArguments();
            if (index < types.length) {
                return types[index];
            }
        }
        return String.class;
    }

    private String decoderToString(Response.Body body) {
        try {
            StringBuilder stringBuilder = new StringBuilder();
            BufferedReader bufferedReader = new BufferedReader(body.asReader());
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line).append(System.lineSeparator());
            }
            return stringBuilder.toString();
        } catch (IOException e) {
            throw new DecodeException(e.getMessage(), e);
        }
    }

    private Object decoderToBaseResponse(Response response, Type type, Class<BaseResponse> clazz, boolean onError) {
        try {
            Object body = decoder.decode(response, getActualTypeArgument(type, onError ? 1 : 0));
            BaseResponse returnObject = clazz.newInstance();
            returnObject.setStatus(response.status());
            returnObject.setMessage(response.reason());
            if (onError) {
                returnObject.setError(body);
            } else {
                returnObject.setData(body);
            }
            return returnObject;
        } catch (Throwable throwable) {
            throw new DecodeException(throwable.getMessage(), throwable);
        }

    }

    private String headersToString(Map<String, Collection<String>> headers) {
        StringBuilder sBuilder = new StringBuilder();
        for (String key : headers.keySet()) {
            if (headers.get(key).isEmpty()) {
                continue;
            }
            sBuilder.append(key).append(": ").append(String.join(", ", headers.get(key))).append(System.lineSeparator());
        }
        sBuilder.delete(sBuilder.length() - System.lineSeparator().length(), sBuilder.length());
        return sBuilder.toString();
    }

    private String buildRequestLog(Request request, Map<String, Collection<String>> headers, byte[] reqBody) {
        String log = String.format("***\nREQUEST:\n%s %s", request.method(), request.url());
        if (headers != null && !headers.isEmpty()) {
            log = String.format("%s\nheaders: \n%s", log, headersToString(headers));
        }
        if (reqBody != null) {
            log = String.format("%s\nbody: \n%s", log, new String(reqBody));
        }
        return log;
    }

    private String buildResponseLog(Response response, Map<String, Collection<String>> headers, String resBody) {
        String log = String.format("RESPONSE:\nstatus: %d(%s)", response.status(), response.reason());
        if (headers != null && !headers.isEmpty()) {
            log = String.format("%s\nheaders:\n%s", log, headersToString(headers));
        }
        if (resBody != null && !resBody.isEmpty()) {
            log = String.format("%s\nbody:\n%s", log, resBody);
        }
        log += "\n***\n";
        return log;
    }

    private void httpLog(Response response, Object body, boolean onError) {
        Request request = response.request();
        if (onError) {
            logger.error(String.format("\n%s\n\n%s",
                    buildRequestLog(request, request.headers(), request.body()),
                    buildResponseLog(response, response.headers(), body.toString())));
            return;
        }

        String log = "";
        if (allowRequestLog) {
            log = String.format("\n%s%s", buildRequestLog(request, request.headers(), request.body()),
                    allowRequestLog && body != null ? "\n" : String.format("%s\n\nRESPONSE: %s %s\n***\n", log, response.status(), response.reason()));
        }
        if (allowResponseLog && body != null) {
            RequestTemplate template = new RequestTemplate();
            encoder.encode(body, body.getClass().getGenericSuperclass(), template);
            log = String.format("\n%s\n%s", log, buildResponseLog(response, response.headers(), new String(template.body())));
        }
        if (log.isEmpty()) {
            log = String.format("%s:%s\n%s", request.method(), request.url(), response.status(), response.reason());
        }
        logger.info(log);
    }
}
