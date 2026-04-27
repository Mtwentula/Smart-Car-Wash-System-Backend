package za.co.int216d.carwash.booking.membership.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import za.co.int216d.carwash.booking.payment.dto.PaymentRequest;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembershipPaymentRequest {

    @NotNull(message = "Payment details are required")
    @Valid
    private PaymentRequest payment;
}
