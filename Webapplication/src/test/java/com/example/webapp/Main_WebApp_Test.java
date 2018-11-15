package com.example.webapp;

import org.apache.tomcat.util.codec.binary.Base64;
import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.nio.charset.Charset;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Main_WebApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class Main_WebApp_Test {

    @LocalServerPort
    private int port;

    TestRestTemplate testRest = new TestRestTemplate();

    @Test
    public void testLoginNoHeader() throws JSONException{

        HttpHeaders header = createHeaders("pramod@gmail.com","passowrd@123");

        HttpEntity<String> entity = new HttpEntity<String>(null,null);

        ResponseEntity<String> response = testRest.exchange(
                            createUrlWithPort("/time"),
                            HttpMethod.GET,entity,String.class);

        String expectedString = "{\"Response\":\"You are not logged in! Please login and then try!\"}";

        JSONAssert.assertEquals(expectedString, response.getBody(),false);

    }

    @Test
    public void testLoginFailed() throws JSONException{
        HttpHeaders header = createHeaders("random@gmail.com","rand@123");

        HttpEntity<String> entity = new HttpEntity<String>(null,header);

        ResponseEntity<String> response = testRest.exchange(
                createUrlWithPort("/time"),
                HttpMethod.GET,entity,String.class);

        String expectedString = "{\"Response\":\"No such account in the system! Please register!\"}";

        JSONAssert.assertEquals(expectedString, response.getBody(),false);
    }

    public HttpHeaders createHeaders(final String uname, final String pswd){
        return new HttpHeaders(){{
            String auth = uname + ":" + pswd;
            byte byteCode[] = Base64.encodeBase64(auth.getBytes(Charset.forName("US-ASCII")));
            String authHeader = "Basic " + new String(byteCode);
            set("Authorization",authHeader);
        }};
    }

    public String createUrlWithPort(String uri){
        return "http://localhost:"+port+uri;
    }

}
