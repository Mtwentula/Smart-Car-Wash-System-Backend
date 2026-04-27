package za.co.int216d.carwash.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import za.co.int216d.carwash.booking.membership.dto.MembershipDetailResponse;
import za.co.int216d.carwash.booking.membership.dto.MembershipPaymentRequest;
import za.co.int216d.carwash.booking.membership.dto.MembershipPlanResponse;
import za.co.int216d.carwash.booking.membership.dto.SubscribeMembershipRequest;
import za.co.int216d.carwash.booking.membership.service.MembershipService;
import za.co.int216d.carwash.booking.payment.dto.PaymentRequest;
import za.co.int216d.carwash.common.security.JwtAuthenticationFilter;
import za.co.int216d.carwash.common.security.SecurityUtils;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for Membership endpoints
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.flyway.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class MembershipControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MembershipService membershipService;

    @MockBean
    private SecurityUtils securityUtils;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JavaMailSender javaMailSender;

    private MembershipDetailResponse mockResponse;

    @BeforeEach
    void setUp() {
        MembershipPlanResponse planResponse = MembershipPlanResponse.builder()
            .id(1L)
            .name("Premium")
            .monthlyPrice(299.99)
            .creditsPerMonth(40)
            .freeWashes(5)
            .discountPercentage(10.0)
            .build();

        mockResponse = MembershipDetailResponse.builder()
            .id(1L)
            .clientId(100L)
            .plan(planResponse)
            .status("ACTIVE")
            .creditsRemaining(40)
            .washesUsedThisMonth(0)
            .autoRenew(true)
            .startDate(LocalDateTime.now())
            .expiryDate(LocalDateTime.now().plusMonths(1))
            .daysUntilExpiry(30)
            .build();

        when(securityUtils.getCurrentUserIdAsLong()).thenReturn(100L);
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void testSubscribeToPlan_Success() throws Exception {
        PaymentRequest payment = PaymentRequest.builder()
            .gateway(null)
            .paymentMethodToken("tok_test")
            .build();
        SubscribeMembershipRequest request = new SubscribeMembershipRequest(1L, true, payment);

        when(membershipService.subscribeToPlan(eq(100L), any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/membership/subscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.clientId").value(100))
            .andExpect(jsonPath("$.plan.name").value("Premium"));
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void testGetMembership_Success() throws Exception {
        when(membershipService.getClientMembership(100L)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/membership/"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.creditsRemaining").value(40));
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void testCancelMembership_Success() throws Exception {
        mockMvc.perform(post("/api/v1/membership/cancel"))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void testRenewMembership_Success() throws Exception {
        PaymentRequest payment = PaymentRequest.builder()
            .gateway(null)
            .paymentMethodToken("tok_test")
            .build();
        MembershipPaymentRequest request = MembershipPaymentRequest.builder()
            .payment(payment)
            .build();

        when(membershipService.renewMembership(eq(100L), any(MembershipPaymentRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/membership/renew")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}
