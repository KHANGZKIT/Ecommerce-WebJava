package com.ecommerce.userservice.service;

import com.ecommerce.userservice.dto.UpdateProfileRequest;
import com.ecommerce.userservice.dto.UserProfileDto;
import com.ecommerce.userservice.exception.GlobalExceptionHandler;
import com.ecommerce.userservice.exception.UserAlreadyExistsException;
import com.ecommerce.userservice.exception.UserNotFoundException;
import com.ecommerce.userservice.model.User;
import com.ecommerce.userservice.model.UserRole;
import com.ecommerce.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserProfileUpdateTest {

    private UserRepository repository;
    private UserService service;
    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        repository = mock(UserRepository.class);
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);

        service = new UserService(repository, passwordEncoder, jwtService, authenticationManager);
        exceptionHandler = new GlobalExceptionHandler();
    }

    private User createSampleUser(Long id, String email, String phone) {
        return User.builder()
                .id(id)
                .firstName("Alice")
                .lastName("Tester")
                .email(email)
                .phone(phone)
                .role(UserRole.CUSTOMER)
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("Cập nhật sang số điện thoại đang thuộc tài khoản khác -> 409 Conflict")
    void updateProfile_duplicatePhoneFromAnotherUser_throwsUserAlreadyExistsException() {
        Long userId = 10L;
        User user = createSampleUser(userId, "alice@techgear.local", "+84901111111");
        when(repository.findById(userId)).thenReturn(Optional.of(user));

        String takenPhone = "+84902222222";
        when(repository.existsByPhoneAndIdNot(takenPhone, userId)).thenReturn(true);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPhone(takenPhone);

        UserAlreadyExistsException ex = assertThrows(
                UserAlreadyExistsException.class,
                () -> service.updateProfile(userId, request)
        );

        assertEquals("An account with this phone number already exists.", ex.getMessage());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());

        // Đảm bảo dữ liệu không bị lưu vào database
        verify(repository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Giữ nguyên số điện thoại của chính mình -> Thành công")
    void updateProfile_samePhone_successWithoutDuplicateCheck() {
        Long userId = 10L;
        User user = createSampleUser(userId, "alice@techgear.local", "+84901111111");
        when(repository.findById(userId)).thenReturn(Optional.of(user));
        when(repository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Alice-New");
        request.setPhone("+84901111111");

        UserProfileDto result = service.updateProfile(userId, request);

        assertNotNull(result);
        assertEquals("Alice-New", result.getFirstName());
        assertEquals("+84901111111", result.getPhone());

        // Không cần truy vấn existsByPhoneAndIdNot khi số điện thoại không đổi
        verify(repository, never()).existsByPhoneAndIdNot(anyString(), anyLong());
        verify(repository).save(user);
    }

    @Test
    @DisplayName("Cập nhật sang số điện thoại chưa tồn tại -> Thành công")
    void updateProfile_newUnusedPhone_success() {
        Long userId = 10L;
        User user = createSampleUser(userId, "alice@techgear.local", "+84901111111");
        when(repository.findById(userId)).thenReturn(Optional.of(user));
        when(repository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        String newPhone = "+84903333333";
        when(repository.existsByPhoneAndIdNot(newPhone, userId)).thenReturn(false);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPhone(newPhone);

        UserProfileDto result = service.updateProfile(userId, request);

        assertNotNull(result);
        assertEquals(newPhone, result.getPhone());
        verify(repository).existsByPhoneAndIdNot(newPhone, userId);
        verify(repository).save(user);
    }

    @Test
    @DisplayName("Cập nhật khi user chưa có số điện thoại ban đầu -> Thành công nếu số chưa dùng")
    void updateProfile_initialNullPhone_success() {
        Long userId = 10L;
        User user = createSampleUser(userId, "alice@techgear.local", null);
        when(repository.findById(userId)).thenReturn(Optional.of(user));
        when(repository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        String newPhone = "+84904444444";
        when(repository.existsByPhoneAndIdNot(newPhone, userId)).thenReturn(false);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPhone(newPhone);

        UserProfileDto result = service.updateProfile(userId, request);

        assertNotNull(result);
        assertEquals(newPhone, result.getPhone());
        verify(repository).save(user);
    }

    @Test
    @DisplayName("GlobalExceptionHandler map DataIntegrityViolationException (uk_users_phone) sang 409 Conflict và không leak phone vào log")
    void exceptionHandler_mapsPhoneUniqueConstraint_to409Conflict_andRedactsLog() {
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        try {
            String sensitivePhone = "+84988776655";
            DataIntegrityViolationException ex = new DataIntegrityViolationException(
                    "could not execute statement",
                    new org.postgresql.util.PSQLException(
                            "ERROR: duplicate key value violates unique constraint \"uk_users_phone\"\n Detail: Key (phone)=(" + sensitivePhone + ") already exists.",
                            org.postgresql.util.PSQLState.UNIQUE_VIOLATION
                    )
            );

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex);

            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(409, response.getBody().getStatus());
            assertEquals("An account with this phone number already exists.", response.getBody().getMessage());
            assertFalse(response.getBody().getMessage().contains("uk_users_phone"));
            assertFalse(response.getBody().getMessage().contains(sensitivePhone));

            // Kiểm tra log: không chứa phone number, không chứa raw detail, chỉ chứa thông báo cố định an toàn
            boolean hasExpectedWarning = listAppender.list.stream()
                    .anyMatch(event -> event.getLevel() == Level.WARN &&
                            event.getFormattedMessage().equals("Phone number unique constraint violation detected"));
            assertTrue(hasExpectedWarning, "Log phải chứa thông báo cố định an toàn");

            boolean containsSensitiveData = listAppender.list.stream()
                    .anyMatch(event -> event.getFormattedMessage().contains(sensitivePhone) ||
                            (event.getThrowableProxy() != null && event.getThrowableProxy().getMessage().contains(sensitivePhone)));
            assertFalse(containsSensitiveData, "Log tuyệt đối không được chứa số điện thoại nhạy cảm");

            boolean containsRawDetail = listAppender.list.stream()
                    .anyMatch(event -> event.getFormattedMessage().contains("Detail: Key (phone)"));
            assertFalse(containsRawDetail, "Log không được chứa raw detail của database");
        } finally {
            logger.detachAppender(listAppender);
        }
    }

    @Test
    @DisplayName("GlobalExceptionHandler map DataIntegrityViolationException (uk_users_email) sang 409 Conflict và không leak email vào log")
    void exceptionHandler_mapsEmailUniqueConstraint_to409Conflict_andRedactsLog() {
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        try {
            String sensitiveEmail = "victim.user@techgear.local";
            DataIntegrityViolationException ex = new DataIntegrityViolationException(
                    "could not execute statement",
                    new org.postgresql.util.PSQLException(
                            "ERROR: duplicate key value violates unique constraint \"uk_users_email\"\n Detail: Key (email)=(" + sensitiveEmail + ") already exists.",
                            org.postgresql.util.PSQLState.UNIQUE_VIOLATION
                    )
            );

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex);

            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(409, response.getBody().getStatus());
            assertEquals("An account with this email address already exists.", response.getBody().getMessage());
            assertFalse(response.getBody().getMessage().contains(sensitiveEmail));

            // Kiểm tra log: không chứa email, không chứa raw detail
            boolean hasExpectedWarning = listAppender.list.stream()
                    .anyMatch(event -> event.getLevel() == Level.WARN &&
                            event.getFormattedMessage().equals("Email unique constraint violation detected"));
            assertTrue(hasExpectedWarning, "Log phải chứa thông báo cố định an toàn cho email");

            boolean containsSensitiveData = listAppender.list.stream()
                    .anyMatch(event -> event.getFormattedMessage().contains(sensitiveEmail) ||
                            (event.getThrowableProxy() != null && event.getThrowableProxy().getMessage().contains(sensitiveEmail)));
            assertFalse(containsSensitiveData, "Log tuyệt đối không được chứa email nhạy cảm");

            boolean containsRawDetail = listAppender.list.stream()
                    .anyMatch(event -> event.getFormattedMessage().contains("Detail: Key (email)"));
            assertFalse(containsRawDetail, "Log không được chứa raw detail của database");
        } finally {
            logger.detachAppender(listAppender);
        }
    }

    @Test
    @DisplayName("GlobalExceptionHandler xử lý unhandled constraint an toàn, không dump raw SQL hay exception chứa PII")
    void exceptionHandler_otherDataIntegrityViolation_doesNotDumpRawSqlOrPii() {
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        try {
            String sensitivePiiInSql = "INSERT INTO user_audit VALUES ('sensitive_ssn_12345')";
            DataIntegrityViolationException ex = new DataIntegrityViolationException(
                    "could not execute statement [" + sensitivePiiInSql + "]",
                    new RuntimeException("ERROR: insert or update on table \"user_audit\" violates foreign key constraint \"fk_audit_user\"")
            );

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex);

            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(409, response.getBody().getStatus());
            assertEquals("Database constraint violation", response.getBody().getMessage());

            // Kiểm tra log: không dump raw SQL hay PII trong message
            boolean containsSensitivePii = listAppender.list.stream()
                    .anyMatch(event -> event.getFormattedMessage().contains("sensitive_ssn_12345") ||
                            (event.getThrowableProxy() != null && event.getThrowableProxy().getMessage().contains("sensitive_ssn_12345")));
            assertFalse(containsSensitivePii, "Log không được dump raw SQL hoặc PII trong exception message");

            boolean hasSafeErrorLog = listAppender.list.stream()
                    .anyMatch(event -> event.getLevel() == Level.ERROR &&
                            event.getFormattedMessage().contains("Unhandled data integrity violation"));
            assertTrue(hasSafeErrorLog, "Log phải ghi lỗi unhandled dạng an toàn");
        } finally {
            logger.detachAppender(listAppender);
        }
    }

    @Test
    @DisplayName("UserNotFoundException khi user không tồn tại -> 404")
    void updateProfile_userNotFound_throwsUserNotFoundException() {
        Long userId = 999L;
        when(repository.findById(userId)).thenReturn(Optional.empty());

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Ghost");

        assertThrows(UserNotFoundException.class, () -> service.updateProfile(userId, request));
    }
}
