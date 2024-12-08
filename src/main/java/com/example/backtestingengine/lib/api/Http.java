package com.example.backtestingengine.lib.api;

import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.functions.Function;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.io.IOException;
import java.util.function.Consumer;

public class Http {
    private static final Logger log = LoggerFactory.getLogger(Http.class);

    private static CloseableHttpClient client;

    public synchronized static HttpClient getDefaultHttpClient() {
        if (client == null) {
            client = HttpClients.createDefault();
        }
        return client;
    }

    public static Observable<HttpResponse> get(String url, Consumer<HttpGet> configureRequest) {
        HttpGet request = new HttpGet(url);
        configureRequest.accept(request);

        return Observable.create((ObservableOnSubscribe<HttpResponse>) emitter -> {
            try {
                log.debug("GET {}", url);
                HttpResponse response = getDefaultHttpClient().execute(request);
                emitter.onNext(response);
                emitter.onComplete();
            } catch (IOException e) {
                emitter.onError(e);
            }
        }).subscribeOn(Schedulers.io());
    }

    public static Observable<HttpResponse> get(String url) {
        return get(url, x -> {
        });
    }

    public static Function<? super HttpResponse, ? extends Observable<String>> asString() {
        return t -> {
            try {
                if (t instanceof ClassicHttpResponse) {
                    return Observable.just(EntityUtils.toString(((ClassicHttpResponse) t).getEntity()));
                } else {
                    return Observable.error(new IllegalArgumentException("Invalid HttpResponse type"));
                }
            } catch (IOException e) {
                return Observable.error(e);
            }
        };
    }
}