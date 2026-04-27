package za.co.int216d.carwash.booking.catalogue.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.int216d.carwash.booking.catalogue.domain.CatalogueAddon;

import java.util.List;

@Repository
public interface CatalogueAddonRepository extends JpaRepository<CatalogueAddon, Long> {
    List<CatalogueAddon> findAllByIsActiveTrueOrderByPriceAsc();
}
