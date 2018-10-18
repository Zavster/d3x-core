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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ParamConverter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

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
 * A unit test for path parameter types
 *
 * @author Xavier Witdouck
 */
@Path("/params")
public class RestParamTest {

    private HttpServer server;


    @BeforeClass()
    public void startup() throws Exception {
        this.server = RestServers.start("doej", Modules.of(m -> {
            m.register(Gson.class, () -> Json.createGsonBuilder(Option.empty()).create());
            m.register(RestParamTest.class, RestParamTest::new);
            m.register(RestPaths1.class, RestPaths1::new);
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
        final String instant = String.valueOf(Instant.now().toEpochMilli());
        final String zonedDate = DateTimeFormatter.ISO_ZONED_DATE_TIME.format(ZonedDateTime.now());
        return new Object[][] {
                { "/params/boolean?param=true", "doej", 200, "true"},
                { "/params/int?param=124", "doej", 200, "124"},
                { "/params/double?param=2.3456", "doej", 200, "2.3456"},
                { "/params/zoneId?param=GMT", "doej", 200, "GMT"},
                { "/params/timeZone?param=GMT", "doej", 200, "GMT"},
                { "/params/month?param=JANUARY", "doej", 200, "JANUARY"},
                { "/params/currency?param=GBP", "doej", 200, "GBP"},
                { "/params/localDate?param=2014-01-01", "doej", 200, "2014-01-01"},
                { "/params/localTime?param=22:15:08", "doej", 200, "22:15:08"},
                { "/params/localDateTime?param=2014-01-01T22:15:08", "doej", 200, "2014-01-01T22:15:08"},
                { "/params/zonedDateTime?param=" + zonedDate, "doej", 200, zonedDate},
                { "/params/instant?param=" + instant, "doej", 200, instant },
                { "/params/listOfInts?param=1,2,3,4", "doej", 200, "1,2,3,4" },
                { "/params/listOfStrings?param=1,2,3,4", "doej", 200, "1,2,3,4" },
                { "/params/listOfBooleans?param=true,false,true", "doej", 200, "true,false,true" },
                { "/params/listOfDates?param=2014-01-01,2014-02-02", "doej", 200, "2014-01-01,2014-02-02" },
                { "/params/setOfInts?param=1,2,3,4", "doej", 200, "1,2,3,4" },
                { "/params/setOfLongs?param=1,2,3,4", "doej", 200, "1,2,3,4" },
                { "/params/setOfDoubles?param=1,2,3,4", "doej", 200, "1.0,2.0,4.0,3.0" },
                { "/params/setOfStrings?param=1,2,3,4", "doej", 200, "1,2,3,4" },
                { "/params/setOfDates?param=2014-01-01,2014-02-02", "doej", 200, "2014-02-02,2014-01-01" },
        };
    }

    @DataProvider(name="malformed")
    public Object[][] malformed() {
        return new Object[][] {
                { "/params/int?param=xxx" },
                { "/params/double?param=xxx" },
                { "/params/zoneId?param=xxx" },
                { "/params/month?param=xxxx" },
                { "/params/currency?param=xxx" },
                { "/params/localDate?param=xxxx" },
                { "/params/localTime?param=xxxx" },
                { "/params/localDateTime?param=xxxx" },
                { "/params/zonedDateTime?param=xxxx" },
                { "/params/instant?param=xxxxx" }
        };
    }


    @DataProvider(name="permissions")
    public Object[][] permissions() {
        return new Object[][] {
                { "/params/int?param=xxx" },
                { "/params/double?param=xxx" },
                { "/params/zoneId?param=xxx" },
                { "/params/month?param=xxxx" },
                { "/params/currency?param=xxx" },
                { "/params/localDate?param=xxxx" },
                { "/params/localTime?param=xxxx" },
                { "/params/localDateTime?param=xxxx" },
                { "/params/zonedDateTime?param=xxxx" },
                { "/params/instant?param=xxxxx" }
        };
    }


    @DataProvider(name="types")
    public Object[][] types() {
        return new Object[][] {
                { 1000 },
                { 1000L },
                { 1000d },
                { false },
                { "hello" },
                { LocalDate.now() },
                { LocalTime.now() },
                { LocalDateTime.now() },
                { ZonedDateTime.now() },
                { TimeZone.getDefault() },
                { ZoneId.systemDefault() },
                { Currency.getInstance("GBP") },
                { List.of(1, 2, 3, 4) },
                { List.of(1L, 2L, 3L, 4L) },
                { List.of(1d, 2d, 3d, 4d) },
                { List.of(LocalDate.now(), LocalDate.now().plusDays(1)) },
                { Set.of(1, 2, 3, 4) },
                { Set.of(1L, 2L, 3L, 4L) },
                { Set.of(1d, 2d, 3d, 4d) },
                { Set.of(LocalDate.now(), LocalDate.now().plusDays(1)) },
        };
    }



    @Test(dataProvider= "success")
    public void success(String path, String user, int status, String response) throws IOException {
        final HttpURLConnection conn = HttpTestUtils.doGet(server.getPort(), path, Option.of(user));
        Assert.assertEquals(conn.getResponseCode(), status);
        final String actual = IO.readText(conn.getInputStream());
        Assert.assertEquals(actual, response);
    }



    @Test(dataProvider= "malformed")
    public void malformed(String path) throws IOException {
        final HttpURLConnection conn = HttpTestUtils.doGet(server.getPort(), path, Option.of("doej"));
        Assert.assertEquals(conn.getResponseCode(), 500);
    }



    @Test(dataProvider= "permissions")
    public void permissions(String path) throws IOException {
        final HttpURLConnection conn = HttpTestUtils.doGet(server.getPort(), path, Option.of("xxxx"));
        Assert.assertEquals(conn.getResponseCode(), 401);
    }


    @Test(dataProvider="types")
    @SuppressWarnings("unchecked")
    public <T> void converters(T value) {
        final Class<T> rawType = value instanceof List ? (Class<T>)List.class : value instanceof Set ? (Class<T>)Set.class : (Class<T>)value.getClass();
        final Type type = value instanceof Collection ? Generic.of(rawType, ((Collection)value).iterator().next().getClass()) : rawType;
        final RestApp.RestParameterConverterProvider provider = new RestApp.RestParameterConverterProvider();
        final ParamConverter<T> converter = provider.getConverter(rawType, type, new Annotation[0]);
        Assert.assertNotNull(converter);
        Assert.assertNull(converter.toString(null));
        final String string = converter.toString(value);
        Assert.assertNotNull(converter);
        final T result = converter.fromString(string);
        Assert.assertEquals(result, value);
        if (value instanceof List) {
            Assert.assertEquals(converter.fromString(null), Collections.emptyList());
        } else if (value instanceof Set) {
            Assert.assertEquals(converter.fromString(null), Collections.emptySet());
        } else {
            Assert.assertNull(converter.fromString(null));
        }
    }



    @GET
    @Path("/boolean")
    @Produces({MediaType.TEXT_PLAIN})
    public String booleanParam(@QueryParam("param") boolean value) {
        return String.valueOf(value);
    }

    @GET
    @Path("/int")
    @Produces({MediaType.TEXT_PLAIN})
    public String intParam(@QueryParam("param") int value) {
        return String.valueOf(value);
    }

    @GET
    @Path("/double")
    @Produces({MediaType.TEXT_PLAIN})
    public String doubleParam(@QueryParam("param") double value) {
        return String.valueOf(value);
    }

    @GET
    @Path("/zoneId")
    @Produces({MediaType.TEXT_PLAIN})
    public String zoneIdParam(@QueryParam("param") ZoneId value) {
        return value.getId();
    }

    @GET
    @Path("/timeZone")
    @Produces({MediaType.TEXT_PLAIN})
    public String timeZoneParam(@QueryParam("param") TimeZone value) {
        return value.getID();
    }

    @GET
    @Path("/month")
    @Produces({MediaType.TEXT_PLAIN})
    public String enumParam(@QueryParam("param") Month value) {
        return value.name();
    }

    @GET
    @Path("/currency")
    @Produces({MediaType.TEXT_PLAIN})
    public String currencyParam(@QueryParam("param") Currency value) {
        return value.getCurrencyCode();
    }

    @GET
    @Path("/localDate")
    @Produces({MediaType.TEXT_PLAIN})
    public String localDateParam(@QueryParam("param") LocalDate value) {
        return DateTimeFormatter.ISO_LOCAL_DATE.format(value);
    }

    @GET
    @Path("/localTime")
    @Produces({MediaType.TEXT_PLAIN})
    public String localTimeParam(@QueryParam("param") LocalTime value) {
        return DateTimeFormatter.ISO_LOCAL_TIME.format(value);
    }

    @GET
    @Path("/localDateTime")
    @Produces({MediaType.TEXT_PLAIN})
    public String localDateTimeParam(@QueryParam("param") LocalDateTime value) {
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(value);
    }

    @GET
    @Path("/zonedDateTime")
    @Produces({MediaType.TEXT_PLAIN})
    public String zonedDateTimeParam(@QueryParam("param") ZonedDateTime value) {
        return DateTimeFormatter.ISO_ZONED_DATE_TIME.format(value);
    }

    @GET
    @Path("/instant")
    @Produces({MediaType.TEXT_PLAIN})
    public String instantParam(@QueryParam("param") Instant value) {
        return String.valueOf(value.toEpochMilli());
    }

    @GET
    @Path("/listOfStrings")
    @Produces({MediaType.TEXT_PLAIN})
    public String listOfStrings(@QueryParam("param") List<String> values) {
        return String.join(",", values);
    }

    @GET
    @Path("/listOfInts")
    @Produces({MediaType.TEXT_PLAIN})
    public String listOfIntegers(@QueryParam("param") List<Integer> values) {
        return String.join(",", values.stream().map(Objects::toString).collect(Collectors.toList()));
    }

    @GET
    @Path("/listOfBooleans")
    @Produces({MediaType.TEXT_PLAIN})
    public String listOfBooleans(@QueryParam("param") List<Boolean> values) {
        return String.join(",", values.stream().map(Objects::toString).collect(Collectors.toList()));
    }

    @GET
    @Path("/listOfDates")
    @Produces({MediaType.TEXT_PLAIN})
    public String listOfDates(@QueryParam("param") List<LocalDate> values) {
        return String.join(",", values.stream().map(Objects::toString).collect(Collectors.toList()));
    }

    @GET
    @Path("/setOfStrings")
    @Produces({MediaType.TEXT_PLAIN})
    public String setOfStrings(@QueryParam("param") Set<String> values) {
        return String.join(",", values);
    }

    @GET
    @Path("/setOfInts")
    @Produces({MediaType.TEXT_PLAIN})
    public String setOfIntegers(@QueryParam("param") Set<Integer> values) {
        return String.join(",", values.stream().map(Objects::toString).collect(Collectors.toList()));
    }

    @GET
    @Path("/setOfLongs")
    @Produces({MediaType.TEXT_PLAIN})
    public String setOfLongs(@QueryParam("param") Set<Long> values) {
        return String.join(",", values.stream().map(Objects::toString).collect(Collectors.toList()));
    }

    @GET
    @Path("/setOfDoubles")
    @Produces({MediaType.TEXT_PLAIN})
    public String setOfDouble(@QueryParam("param") Set<Double> values) {
        return String.join(",", values.stream().map(Objects::toString).collect(Collectors.toList()));
    }

    @GET
    @Path("/setOfDates")
    @Produces({MediaType.TEXT_PLAIN})
    public String setOfDates(@QueryParam("param") Set<LocalDate> values) {
        return String.join(",", values.stream().map(Objects::toString).collect(Collectors.toList()));
    }

}
