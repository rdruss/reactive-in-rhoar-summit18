package com.redhat.summit18;

import io.reactivex.Single;
import io.vertx.core.json.Json;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.eventbus.Message;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import io.vertx.reactivex.ext.web.handler.StaticHandler;

import static com.redhat.summit18.SockJsHelper.getSockJsHandler;

public class ShoppingServiceVerticle extends AbstractVerticle {

    private Database database;
    private WebClient pricer;

    @Override
    public void start() throws Exception {
        vertx.deployVerticle(AuditVerticle.class.getName());

        pricer = WebClient.create(vertx, new WebClientOptions()
            .setDefaultHost("price-service")
            .setDefaultPort(config().getInteger("http.port", 8080)));

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        router.get("/health").handler(rc -> rc.response().end("OK"));
        router.get("/eventbus/*").handler(getSockJsHandler(vertx));

        // Reactive Rest API
        router.get("/products").handler(this::list);
        router.post("/products").handler(this::add);
        router.get("/*").handler(StaticHandler.create());

        // Initialization
        Database.initialize(vertx)
                .flatMap(db -> {
                    database = db;
                    return vertx.createHttpServer()
                            .requestHandler(router::accept)
                            .rxListen(config().getInteger("http.port", 8080));
                }).subscribe();

    }

    private void add(RoutingContext rc) {
        String name = rc.getBodyAsString().trim();
        database.insert(name)
                .flatMap(p -> {
                    Single<Product> price = getPriceForProduct(p);
                    Single<Integer> audit = sendActionToAudit(p);
                    return Single.zip(price, audit, (pr, a) -> pr);
                })
                .subscribe(
                        p -> {
                            String json = Json.encode(p);
                            rc.response().setStatusCode(201).end(json);
                            vertx.eventBus().publish("products", json);
                        },
                        rc::fail);
    }

    private Single<Integer> sendActionToAudit(Product product) {
        return vertx.eventBus()
                .<Integer>rxSend("audit",
                        "Adding " + product.getName())
                .map(Message::body);
    }

    private Single<Product> getPriceForProduct(Product p) {
        return pricer.get("/prices/" + p.getName()).rxSend()
                .map(HttpResponse::bodyAsJsonObject)
                .map(json -> p.setPrice(json.getDouble("price")));
    }

    private void list(RoutingContext rc) {
        HttpServerResponse response = rc.response().setChunked(true);
        database.retrieve()
                .flatMapSingle(p ->
                        pricer.get("/prices/" + p.getName())
                                .rxSend()
                                .map(HttpResponse::bodyAsJsonObject)
                                .map(json ->
                                        p.setPrice(json.getDouble("price")))
                )
                .subscribe(
                        p -> response.write(Json.encode(p) + " \n\n"),
                        rc::fail,
                        response::end);
    }

}
