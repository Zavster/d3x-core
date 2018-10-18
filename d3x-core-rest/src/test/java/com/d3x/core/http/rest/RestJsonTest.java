/*
 * Copyright 2018, D3X Systems - All Rights Reserved
 *
 * Licensed under a proprietary end-user agreement issued by D3X Systems.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.d3xsystems.com/terms/license.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.d3x.core.http.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.d3x.core.http.server.HttpServer;
import com.d3x.core.http.server.HttpTestUtils;
import com.d3x.core.json.Json;
import com.d3x.core.util.Generic;
import com.d3x.core.util.IO;
import com.d3x.core.util.Modules;
import com.d3x.core.util.Option;
import com.google.gson.Gson;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests JSON input/output through the REST api
 *
 * @author Xavier Witdouck
 */
@Path("/json")
public class RestJsonTest {

    private HttpServer server;
    private Gson gson = Json.createGsonBuilder(Option.empty()).create();


    @BeforeClass()
    public void startup() throws Exception {
        this.server = RestServers.start("doej", Modules.of(m -> {
            m.register(Gson.class, () -> gson);
            m.register(RestJsonTest.class, RestJsonTest::new);
        }));
    }

    @AfterClass
    public void shutdown() {
        if (server != null) {
            server.stop();
        }
    }

    @DataProvider(name="success")
    public Object[][] success() {
        return new Object[][] {
                { "/json/listOfInts", 200, List.of(1,2,3,4) },
                { "/json/listOfStrings", 200, List.of("1","2","3","4") },
                { "/json/listOfBooleans", 200, List.of(true, false, true) },
                { "/json/listOfDates", 200, List.of(LocalDate.parse("2014-01-01"), LocalDate.parse("2014-02-02")) },
                { "/json/listOfCustom", 200, IntStream.range(0, 20).mapToObj(i -> Custom.random()).collect(Collectors.toList()) },
        };
    }


    @Test(dataProvider= "success")
    public <T> void success(String path, int status, List<T> value) throws IOException {
        final int port = server.getPort();
        final String jsonString = gson.toJson(value);
        final HttpURLConnection conn = HttpTestUtils.doPost(port, path, Option.of("doej"), "application/json", jsonString);
        Assert.assertEquals(conn.getResponseCode(), status);
        final String responseJson = IO.readText(conn.getInputStream());
        final Type type = Generic.of(List.class, value.iterator().next().getClass());
        final List<T> result = gson.fromJson(responseJson, type);
        Assert.assertEquals(value, result);
    }


    @POST
    @Path("/listOfStrings")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public List<String> listOfStrings(List<String> values) {
        return values;
    }

    @POST
    @Path("/listOfInts")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public List<Integer> listOfIntegers(List<Integer> values) {
        return values;
    }

    @POST
    @Path("/listOfBooleans")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public List<Boolean> listOfBooleans(List<Boolean> values) {
        return values;
    }

    @POST
    @Path("/listOfDates")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public List<LocalDate> listOfDates(List<LocalDate> values) {
        return values;
    }

    @POST
    @Path("/listOfCustom")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public List<Custom> listOfCustom(List<Custom> values) {
        return values;
    }


    @lombok.ToString
    @lombok.EqualsAndHashCode()
    public static class Custom  {
        private int id;
        private String name;
        private LocalDate date;
        private ZoneId zoneId;
        private Currency currency;

        static Custom random() {
            Custom custom = new Custom();
            custom.id = new Random().nextInt();
            custom.name = String.valueOf(System.nanoTime());
            custom.date = LocalDate.now().plusDays(new Random().nextInt(1000));
            custom.currency = Currency.getInstance("GBP");
            return custom;
        }
    }

}
