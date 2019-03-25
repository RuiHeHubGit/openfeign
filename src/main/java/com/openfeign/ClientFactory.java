package com.openfeign;

import feign.Feign;
import feign.Logger;
import feign.Request;
import feign.Response;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.jaxb.JAXBContextFactory;
import feign.jaxb.JAXBDecoder;
import feign.jaxb.JAXBEncoder;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientFactory {
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    private final String defaultBaseUrl;
    private final Type errorType;
    private final ConcurrentHashMap<String, ApiClient> clientMap;

    public static class Builder {
        private int connectTimeoutMillis;
        private int readTimeoutMillis;
        private String defaultBaseUrl;
        private Type errorType;

        public static Builder getInstance() {
            return new Builder();
        }

        public Builder() {
            this.connectTimeoutMillis = 10 * 1000;
            this.readTimeoutMillis = 60 * 1000;
            this.defaultBaseUrl = System.getProperty("service.url");
            this.errorType = Map.class;
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

        public Builder errorType(Type errorType) {
            this.errorType = errorType;
            return this;
        }

        public ClientFactory build() {
            return new ClientFactory(connectTimeoutMillis, readTimeoutMillis, defaultBaseUrl, errorType);
        }
    }

    public static Builder Builder() {
        return new Builder();
    }

    private ClientFactory(int connectTimeoutMillis, int readTimeoutMillis, String defaultBaseUrl, Type errorType) {
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
        this.defaultBaseUrl = defaultBaseUrl;
        this.errorType = errorType;
        this.clientMap = new ConcurrentHashMap<>();
    }

    public <T extends ApiClient> T createJsonClient(Class<T> tClass, String baseUrl) {
        return createClient(tClass, baseUrl, new GsonEncoder(), new GsonDecoder());
    }

    public <T extends ApiClient> T createXmlClient(Class<T> tClass, String baseUrl) {
        JAXBContextFactory jaxbFactory = new JAXBContextFactory.Builder()
                .withMarshallerJAXBEncoding("UTF-8")
                .withMarshallerSchemaLocation("http://apihost http://apihost/schema.xsd")
                .build();
        return createClient(tClass, baseUrl, new JAXBEncoder(jaxbFactory), new JAXBDecoder(jaxbFactory));
    }

    public <T extends ApiClient> T createClient(Class<T> tClass, String baseUrl, Encoder encoder, Decoder decoder) {
        if(tClass == null || encoder == null || decoder == null) {
            throw new NullPointerException("[tClass:"+tClass+",encoder:"+encoder+",decoder:"+decoder+"]");
        }
        if(baseUrl == null) {
            baseUrl = defaultBaseUrl;
        }
        String key = tClass.getTypeName() + ";" + baseUrl + ";" + encoder.getClass() +";"+ decoder.getClass();
        if(clientMap.contains(key)) {
            return (T) clientMap.get(key);
        }

        T client = Feign.builder()
                .encoder(encoder)
                .decoder(decoder)
                .options(new Request.Options(connectTimeoutMillis, readTimeoutMillis))
                .errorDecoder((methodKey, response) -> {
                    Object error = null;
                    try {
                        error = decoder.decode(response, errorType);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return new CallApiException(response.status(), error);
                })
                .logLevel(Logger.Level.FULL)
                .target(tClass, baseUrl);
        client= (T) createProxyClient(client);
        clientMap.put(key, client);

        return client;
    }

    private <T extends ApiClient> ApiClient createProxyClient(T client) {
        return (ApiClient) Proxy.newProxyInstance(getClass().getClassLoader(), client.getClass().getInterfaces(),
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
                        if(throwable != null) {
                            if(throwable instanceof CallApiException && BaseResponse.class.isAssignableFrom(returnType)) {
                                CallApiException exception = (CallApiException) throwable;
                                return  ((BaseResponse) returnType.newInstance()).init(exception.getHttpStatus(), exception.getData());
                            }
                            throw throwable;
                        }

                        if(BaseResponse.class.isAssignableFrom(returnType)) {
                            if(result == null) {
                                return ((BaseResponse) returnType.newInstance()).init(200, null);
                            }
                            ((BaseResponse) result).setHttpStatus(200);
                        }

                        return result;
                    }

                });
    }
}
