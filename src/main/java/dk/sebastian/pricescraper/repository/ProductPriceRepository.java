package dk.sebastian.pricescraper.repository;

import dk.sebastian.pricescraper.entity.ProductPriceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ProductPriceRepository extends JpaRepository<ProductPriceEntity, String> {

    boolean existsByProductNumber(String productNumber);

    List<ProductPriceEntity> findTop100ByScrapedAtIsNotNullOrderByScrapedAtDesc();

    @Query("""
            select p
            from ProductPriceEntity p
            where p.productNumber in :productNumbers
               or p.eanNumber in :eanNumbers
            order by
                p.productNumber asc,
                case when p.scrapedAt is null then 1 else 0 end asc,
                p.scrapedAt desc
            """)
    List<ProductPriceEntity> findByProductNumberInOrEanNumberInOrderByBestSnapshotFirst(
            @Param("productNumbers")
            Collection<String> productNumbers,
            @Param("eanNumbers")
            Collection<String> eanNumbers
    );

    @Query("""
            select p
            from ProductPriceEntity p
            order by
                case when p.scrapedAt is null then 0 else 1 end asc,
                p.scrapedAt asc
            """)
    List<ProductPriceEntity> findAllKnownProductsByRefreshPriority();
}
