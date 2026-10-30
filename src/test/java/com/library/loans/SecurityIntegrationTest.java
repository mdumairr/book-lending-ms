package com.library.loans;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
class SecurityIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("booklending")
            .withUsername("booklending")
            .withPassword("booklending");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/books")).andExpect(status().isUnauthorized());
    }

    @Test
    void memberCanListBooks() throws Exception {
        mockMvc.perform(get("/api/books").with(httpBasic("member@library.com", "member")))
                .andExpect(status().isOk());
    }

    @Test
    void memberCannotCreateBook() throws Exception {
        mockMvc.perform(post("/api/books").with(httpBasic("member@library.com", "member"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"T\",\"author\":\"A\",\"isbn\":\"999\",\"totalCopies\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void librarianCanCreateBook() throws Exception {
        mockMvc.perform(post("/api/books").with(httpBasic("librarian@library.com", "librarian"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"T\",\"author\":\"A\",\"isbn\":\"999\",\"totalCopies\":1}"))
                .andExpect(status().isCreated());
    }

    @Test
    void librarianCannotBorrow() throws Exception {
        mockMvc.perform(post("/api/loans/borrow/00000000-0000-0000-0000-000000000000")
                .with(httpBasic("librarian@library.com", "librarian")))
                .andExpect(status().isForbidden());
    }

    @Test
    void memberCanAccessOwnLoans() throws Exception {
        mockMvc.perform(get("/api/loans").with(httpBasic("member@library.com", "member")))
                .andExpect(status().isOk());
    }

    @Test
    void memberCannotListAllMembers() throws Exception {
        mockMvc.perform(get("/api/members").with(httpBasic("member@library.com", "member")))
                .andExpect(status().isForbidden());
    }

    @Test
    void validationErrorReturns400() throws Exception {
        mockMvc.perform(post("/api/books").with(httpBasic("librarian@library.com", "librarian"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"\",\"author\":\"A\",\"isbn\":\"999\",\"totalCopies\":1}"))
                .andExpect(status().isBadRequest());
    }
}
