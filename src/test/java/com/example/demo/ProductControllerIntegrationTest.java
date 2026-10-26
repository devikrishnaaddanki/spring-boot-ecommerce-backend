
package com.example.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User createTestUserIfAbsent() {

        return userRepository.findByUsername("testuser")
                .orElseGet(() -> {
                    User user = new User(
                            "testuser",
                            passwordEncoder.encode("Test@123"),
                            "USER"
                    );

                    return userRepository.saveAndFlush(user);
                });
    }



    @Test
    void getProducts_withoutToken_shouldReturn401()
            throws Exception {

        mockMvc.perform(get("/api/products"))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isUnauthorized());
    }



    @Test
    void getProducts_withValidToken_shouldReturn200() throws Exception {


        User user = createTestUserIfAbsent();

        String token = jwtService.generateToken(user.getUsername());
        mockMvc.perform(
                get("/api/products")
                        .header("Authorization", "Bearer " + token)
        )
        .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
        .andExpect(status().isOk());
    }

    @Test
    void createProduct_withNegativePrice_shouldReturn400() throws Exception {

        // Generate a JWT for an existing test user
        User user = createTestUserIfAbsent();
        String token = jwtService.generateToken(user.getUsername());

        // Send invalid product data
        String requestBody = """
            {
                "name": "Gaming Keyboard",
                "description": "Mechanical keyboard",
                "price": -100,
                "stockQuantity": 10
            }
            """;

        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .post("/api/products")
                    .header("Authorization", "Bearer " + token)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
            .andExpect(status().isBadRequest());
    }

    @Test
    void createProduct_withoutName_shouldReturn400() throws Exception {

        User user = createTestUserIfAbsent();
        String token = jwtService.generateToken(user.getUsername());

        String requestBody = """
            {
                "description": "Mechanical keyboard",
                "price": 1500.00,
                "stockQuantity": 10
            }
            """;

        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .post("/api/products")
                    .header("Authorization", "Bearer " + token)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
            .andExpect(status().isBadRequest());
    }

    @Test
    void createProduct_withValidData_shouldReturn200() throws Exception {

        User user = createTestUserIfAbsent();
        String token = jwtService.generateToken(user.getUsername());

        String requestBody = """
            {
                "name": "Integration Test Keyboard",
                "description": "Keyboard created during MockMvc testing",
                "price": 1500.00,
                "stockQuantity": 10
            }
            """;

        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .post("/api/products")
                    .header("Authorization", "Bearer " + token)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
            .andExpect(status().isOk())
            .andExpect(
                org.springframework.test.web.servlet.result.MockMvcResultMatchers
                    .jsonPath("$.name").value("Integration Test Keyboard")
            )
            .andExpect(
                org.springframework.test.web.servlet.result.MockMvcResultMatchers
                    .jsonPath("$.price").value(1500.00)
            )
            .andExpect(
                org.springframework.test.web.servlet.result.MockMvcResultMatchers
                    .jsonPath("$.stockQuantity").value(10)
            );
    }
    @Test
    void getProductById_withValidToken_shouldReturn200() throws Exception {


        User user = createTestUserIfAbsent();

        String token = jwtService.generateToken(user.getUsername());



        // First, create a product using the POST endpoint
        String requestBody = """
            {
                "name": "Test Mouse",
                "description": "Wireless mouse",
                "price": 750.00,
                "stockQuantity": 5
            }
            """;

        String response = mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .post("/api/products")
                    .header("Authorization", "Bearer " + token)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        // Extract the generated product ID from the POST response
        long productId = com.fasterxml.jackson.databind.json.JsonMapper
                .builder()
                .build()
                .readTree(response)
                .get("id")
                .asLong();

        // Now retrieve that product using its ID
        mockMvc.perform(
                get("/api/products/" + productId)
                    .header("Authorization", "Bearer " + token)
            )
            .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Test Mouse"))
            .andExpect(jsonPath("$.description").value("Wireless mouse"))
            .andExpect(jsonPath("$.stockQuantity").value(5));
    }
    @Test
    void getProductById_productDoesNotExist_shouldReturn404() throws Exception {

        User user = createTestUserIfAbsent();
        String token = jwtService.generateToken(user.getUsername());

        mockMvc.perform(
                get("/api/products/999999")
                    .header("Authorization", "Bearer " + token)
            )
            .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
            .andExpect(status().isNotFound());
    }

}
