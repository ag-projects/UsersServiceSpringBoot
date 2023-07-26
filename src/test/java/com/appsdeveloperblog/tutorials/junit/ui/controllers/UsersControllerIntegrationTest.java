package com.appsdeveloperblog.tutorials.junit.ui.controllers;

import com.appsdeveloperblog.tutorials.junit.security.SecurityConstants;
import com.appsdeveloperblog.tutorials.junit.ui.response.UserRest;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.List;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
        // ,properties = "server.port=8020"
)
//@TestPropertySource(locations = "/application-test.properties"
//        , properties = "server.port=8040"
//)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UsersControllerIntegrationTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Value("${server.port}")
    private int serverPort;

    @LocalServerPort
    private int localServerPort;

    private String authorizationToken;

    @Order(1)
    @DisplayName("User can be created")
    @Test
    void testCreateUser_whenValidDetailsProvided_returnsUserDetails() throws JSONException {
        // Given
        JSONObject userDetailsJsonObject = new JSONObject();
        userDetailsJsonObject.put("firstName", "Tony");
        userDetailsJsonObject.put("lastName", "Stevens");
        userDetailsJsonObject.put("email", "test3@test.com");
        userDetailsJsonObject.put("password", "12345678");
        userDetailsJsonObject.put("repeatPassword", "12345678");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<String> request = new HttpEntity<>(userDetailsJsonObject.toString(), headers);

        // When
        ResponseEntity<UserRest> createdUserDetailsEntity = testRestTemplate.postForEntity("/users", request, UserRest.class);
        UserRest createdUserDetails = createdUserDetailsEntity.getBody();

        // Then
        Assertions.assertEquals(HttpStatus.OK, createdUserDetailsEntity.getStatusCode());
        Assertions.assertEquals(userDetailsJsonObject.getString("firstName"), createdUserDetails.getFirstName(),
                "Returned user firstName seems to be incorrect.");
        Assertions.assertEquals(userDetailsJsonObject.getString("lastName"), createdUserDetails.getLastName(),
                "Returned user lastName seems to be incorrect.");
        Assertions.assertEquals(userDetailsJsonObject.getString("email"), createdUserDetails.getEmail(),
                "Returned user email seems to be incorrect.");
        Assertions.assertFalse(createdUserDetails.getUserId().trim().isEmpty(),
                "User id should not be empty");
    }

    @Order(2)
    @DisplayName("GET /users requires JWT")
    @Test
    void testGetUsers_whenMissingJWT_returns403() {
        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");

        HttpEntity requestEntity = new HttpEntity<>(null, headers);

        // When
        ResponseEntity<List<UserRest>> response = testRestTemplate.exchange("/users",
                HttpMethod.GET,
                requestEntity,
                new ParameterizedTypeReference<>() {
                });

        // Then
        Assertions.assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(),
                "HTTP status code 403 should be returned.");
    }

    @Order(3)
    @DisplayName("/login works")
    @Test
    void testLoginUser_whenValidCredentialsProvided_returnsJWTinAuthorizationHeader() throws JSONException {
        // Given
        JSONObject loginCredentials = new JSONObject();
        loginCredentials.put("email", "test3@test.com");
        loginCredentials.put("password", "12345678");

        HttpEntity<String> request = new HttpEntity<>(loginCredentials.toString());

        // When
        ResponseEntity response = testRestTemplate.postForEntity("/users/login",
                request,
               null);

        // Then
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode(),
                "Http status code should be 200.");

        authorizationToken = response.getHeaders().getValuesAsList(SecurityConstants.HEADER_STRING).get(0);
        Assertions.assertNotNull(authorizationToken,
                "Response should contain Authorization header with JWT");

        Assertions.assertNotNull(response.getHeaders().getValuesAsList("UserID").get(0),
                "Response should contain UserID in response header");
    }

    @Order(4)
    @DisplayName("/login works with valid JWT")
    @Test
    void testGetUsers_whenValidJWPProvided_returnsUsers() {
        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(authorizationToken);
        HttpEntity requestEntity = new HttpEntity<>(headers);

        // When
        ResponseEntity<List<UserRest>> response = testRestTemplate.exchange("/users",
                HttpMethod.GET,
                requestEntity,
                new ParameterizedTypeReference<>() {
                });


        // Then
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode(),
                "Http status code should be 200.");
        Assertions.assertEquals(response.getBody().size(), 1,
                "There should be exactly one user in the list");

    }
}
