/*
 *
 *  Copyright 2016-2017 Red Hat, Inc, and individual contributors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.redhat.summit18;

import io.restassured.RestAssured;
import org.arquillian.cube.openshift.impl.enricher.RouteURL;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.get;
import static org.awaitility.Awaitility.await;


@RunWith(Arquillian.class)
public class OpenShiftIT {

    private static final String PRICE_SERVICE_APP = "price-service";
    private static final String SHOPPING_SERVICE_APP = "shopping-service";

    @RouteURL(PRICE_SERVICE_APP)
    private URL priceServiceUrl;

    @RouteURL(SHOPPING_SERVICE_APP)
    private URL shoppingServiceUrl;

    @Before
    public void setup() {
        await().pollInterval(1, TimeUnit.SECONDS).atMost(5, TimeUnit.MINUTES).until(() -> {
            try {
                return get(shoppingServiceUrl.toExternalForm() + "health").getStatusCode() == 200
                        && get(priceServiceUrl.toExternalForm() + "health").getStatusCode() == 200;
            } catch (Exception ignored) {
                return false;
            }
        });
    }

    @Test
    public void testProductList() throws InterruptedException {
        RestAssured.when()
                .get(shoppingServiceUrl.toExternalForm() + "products")
                .then().statusCode(200);
    }

    @Test
    public void testShoppingRootURL() throws InterruptedException {
        RestAssured.when()
                .get(shoppingServiceUrl.toExternalForm())
                .then().statusCode(200);
    }
}