package dk.sebastian.pricescraper.repository;

import dk.sebastian.pricescraper.entity.ProductLookupFailureEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductLookupFailureRepository extends JpaRepository<ProductLookupFailureEntity, String> {

    List<ProductLookupFailureEntity> findAllByOrderByLastFailedAtDesc();
}
