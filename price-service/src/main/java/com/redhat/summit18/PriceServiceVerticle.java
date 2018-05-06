package com.redhat.summit18;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;


public class PriceServiceVerticle extends AbstractVerticle {

    private Map<String, Double> prices = new HashMap<>();
    private Random random = new Random();

    @Override
    public void start() {

        Router router = Router.router(vertx);

        router.get("/health").handler(rc -> rc.response().end("OK"));

        /**
         * Define REST endpoint for pricing
         */
        router.get("/prices/:name").handler(rc -> {
            String name = rc.pathParam("name");
            Double price = prices
                    .computeIfAbsent(name,
                            k -> (double) random.nextInt(50));
            System.out.println("Getting price for " + name + " : " + price);
            rc.response()
                    .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .end(new JsonObject().put("name", name)
                    .put("price", price).encodePrettily());
        });

        // Start listening
        vertx.createHttpServer()
            .requestHandler(router::accept)
            .listen(config().getInteger("http.port", 8080));
    }
}
