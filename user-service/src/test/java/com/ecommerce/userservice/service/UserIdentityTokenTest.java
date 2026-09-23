package com.ecommerce.userservice.service;

import com.ecommerce.userservice.dto.LoginRequest;
import com.ecommerce.userservice.dto.RegisterRequest;
import com.ecommerce.userservice.model.User;
import com.ecommerce.userservice.model.UserRole;
import com.ecommerce.userservice.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import java.security.Key;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserIdentityTokenTest {
    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private UserRepository repository;
    private AuthenticationManager authentication;
    private UserService service;
    private JwtService jwt;

    @BeforeEach
    void setUp() {
        repository = mock(UserRepository.class);
        authentication = mock(AuthenticationManager.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode(anyString())).thenReturn("encoded");
        jwt = new JwtService();
        ReflectionTestUtils.setField(jwt, "secretKey", Encoders.BASE64.encode(key.getEncoded()));
        ReflectionTestUtils.setField(jwt, "jwtExpiration", 1800000L);
        service = new UserService(repository, encoder, jwt, authentication);
    }

    private void assertIdentity(String token, long id) {
        assertIdentity(token, id, "CUSTOMER");
    }

    private void assertIdentity(String token, long id, String expectedRole) {
        var claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        assertEquals("a@example.com", claims.getSubject());
        assertEquals(id, ((Number) claims.get("userId")).longValue());
        assertEquals(expectedRole, claims.get("role"));
        assertEquals(1800000L, claims.getExpiration().getTime() - claims.getIssuedAt().getTime());
        var details = org.springframework.security.core.userdetails.User.withUsername("a@example.com")
                .password("encoded").roles(expectedRole).build();
        assertTrue(jwt.isTokenValid(token, details));
    }

    @Test
    void registrationUsesIdAssignedByRepository() {
        when(repository.save(any(User.class))).thenAnswer(call -> {
            User saved = call.getArgument(0);
            saved.setId(41L);
            return saved;
        });
        RegisterRequest request = new RegisterRequest();
        request.setEmail("A@EXAMPLE.COM");
        request.setPassword("Password1!");
        request.setFirstName("Alice");
        request.setLastName("Example");
        var response = service.register(request);
        assertIdentity(response.getAccessToken(), 41L);
        assertEquals(41L, response.getUser().getId());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(1800000L, response.getExpiresIn());
    }

    @Test
    void loginAuthenticatesThenIssuesDatabaseIdAndKeepsEmailSubject() {
        when(repository.findByEmail("a@example.com")).thenReturn(Optional.of(User.builder()
                .id(42L).email("a@example.com").password("encoded").role(UserRole.CUSTOMER).build()));
        LoginRequest request = new LoginRequest();
        request.setEmail("A@EXAMPLE.COM");
        request.setPassword("Password1!");
        var response = service.login(request);
        var order = inOrder(authentication, repository);
        order.verify(authentication).authenticate(argThat(auth ->
                "a@example.com".equals(auth.getName()) && "Password1!".equals(auth.getCredentials())));
        order.verify(repository).findByEmail("a@example.com");
        assertIdentity(response.getAccessToken(), 42L);
        assertEquals(42L, response.getUser().getId());
    }

    @Test
    void loginWithAdminRoleIssuesAdminRoleClaim() {
        when(repository.findByEmail("a@example.com")).thenReturn(Optional.of(User.builder()
                .id(99L).email("a@example.com").password("encoded").role(UserRole.ADMIN).build()));
        LoginRequest request = new LoginRequest();
        request.setEmail("A@EXAMPLE.COM");
        request.setPassword("Password1!");
        var response = service.login(request);
        assertIdentity(response.getAccessToken(), 99L, "ADMIN");
        assertEquals(99L, response.getUser().getId());
    }

    @Test
    void rejectedCredentialsCannotIssueToken() {
        when(authentication.authenticate(any())).thenThrow(new BadCredentialsException("Invalid"));
        LoginRequest request = new LoginRequest();
        request.setEmail("a@example.com");
        request.setPassword("wrong");
        assertThrows(BadCredentialsException.class, () -> service.login(request));
        verifyNoInteractions(repository);
    }
}