package za.co.int216d.carwash.booking.catalogue.dto;

import lombok.Builder;

@Builder
public record CatalogueServiceResponse(
        Long id,
        String code,
        String name,
        String description,
        String serviceType,
        Double basePrice,
        Integer durationMinutes,
        Boolean isActive
) {
}
