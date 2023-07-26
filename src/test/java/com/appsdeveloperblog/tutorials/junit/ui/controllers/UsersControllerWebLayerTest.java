package com.appsdeveloperblog.tutorials.junit.ui.controllers;

import com.appsdeveloperblog.tutorials.junit.service.UsersService;
import com.appsdeveloperblog.tutorials.junit.service.UsersServiceImpl;
import com.appsdeveloperblog.tutorials.junit.shared.UserDto;
import com.appsdeveloperblog.tutorials.junit.ui.request.UserDetailsRequestModel;
import com.appsdeveloperblog.tutorials.junit.ui.response.UserRest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebMvcTest(controllers = {UsersController.class},
            excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@AutoConfigureMockMvc
//@AutoConfigureMockMvc(addFilters = false)
@MockBean({UsersServiceImpl.class})
public class UsersControllerWebLayerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsersService usersService;

    private UserDetailsRequestModel userDetailsRequestModel;

    @BeforeEach
    void setUp() {
        userDetailsRequestModel = new UserDetailsRequestModel();
        userDetailsRequestModel.setFirstName("Tony");
        userDetailsRequestModel.setLastName("Stevens");
        userDetailsRequestModel.setEmail("tony.stevens@gmail.com");
        userDetailsRequestModel.setPassword("12345678");
    }

    @DisplayName("Create a user")
    @Test
    void testCreateUser_whenValidParamProvided_returnsUserDetails() throws Exception {
        // Given
        UserDto userDto = new ModelMapper().map(userDetailsRequestModel, UserDto.class);
        userDto.setUserId(UUID.randomUUID().toString());

        when(usersService.createUser(any(UserDto.class))).thenReturn(userDto);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.post("/users")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(userDetailsRequestModel));


        // When
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();
        String responseBody = mvcResult.getResponse().getContentAsString();
        UserRest createdUser = new ObjectMapper().readValue(responseBody, UserRest.class);

        // Then
        Assertions.assertEquals(userDetailsRequestModel.getFirstName(), createdUser.getFirstName(),
                "The returned user firstName is most likely incorrect.");
        Assertions.assertEquals(userDetailsRequestModel.getLastName(), createdUser.getLastName(),
                "The returned user lastName is most likely incorrect.");
        Assertions.assertEquals(userDetailsRequestModel.getEmail(), createdUser.getEmail(),
                "The returned user email is most likely incorrect.");
        Assertions.assertFalse(createdUser.getUserId().isEmpty(), "The user ID should not be empty.");

    }

    @DisplayName("First name is not empty.")
    @Test
    void testCreateUser_whenFirstNameIsNotEmpty_returns400StatusCode() throws Exception {
        // Given
        userDetailsRequestModel.setFirstName("");

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.post("/users")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(userDetailsRequestModel));

        // When
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();

        // Then
        Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), mvcResult.getResponse().getStatus(),
                "Incorrect HTTP status code returned.");
    }

    @DisplayName("Create user with fistName only one character")
    @Test
    void testCreateUser_whenFirstNameIsOnlyOneCharacter_returns400StatusCode() throws Exception {
        // Given
        userDetailsRequestModel.setFirstName("T");
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.post("/users")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(userDetailsRequestModel));

        // When
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andReturn();

        // Then
        Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), mvcResult.getResponse().getStatus(),
                "Incorrect HTTP status code returned.");
    }
}
