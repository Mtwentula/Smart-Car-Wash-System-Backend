package za.co.int216d.carwash.booking.catalogue.dto;

import lombok.Builder;

@Builder
public record CatalogueAddonResponse(
        Long id,
        String code,
        String name,
        String description,
        Double price,
        Boolean isActive
) {
}
