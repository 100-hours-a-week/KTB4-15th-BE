package com.ktb.lookddak.domain.product.repository;

import com.ktb.lookddak.domain.product.entity.Product;
import com.ktb.lookddak.domain.product.entity.ProductItemType;
import com.ktb.lookddak.domain.product.service.ProductService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.datasource.url=jdbc:h2:mem:click_receipt;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ProductService.class)
class ProductClickReceiptRepositoryTest {

    @Autowired
    private ProductClickReceiptRepository receipts;
    @Autowired
    private ProductRepository products;
    @Autowired
    private ProductService service;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private final LocalDateTime now = LocalDateTime.of(2026, 11, 7, 12, 0);

    private int claim(long memberId, long productId, LocalDateTime time) {
        receipts.ensureReceipt(memberId, productId);
        return receipts.claimClick(memberId, productId, time, time.minusHours(24));
    }

    @Test
    @DisplayName("중복 클릭은 기간을 연장하지 않고 정확히 24시간 이후 다시 집계한다")
    void countAgainAt24Hours() {
        assertThat(claim(1, 1, now)).isEqualTo(1);
        assertThat(claim(1, 1, now.plusHours(23))).isZero();
        assertThat(claim(1, 1, now.plusHours(24).minusNanos(1000))).isZero();
        assertThat(claim(1, 1, now.plusHours(24))).isEqualTo(1);
        assertThat(claim(1, 1, now.plusHours(24))).isZero();
        assertThat(receipts.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("회원과 상품이 다르면 각각 독립적으로 집계한다")
    void independentWindows() {
        assertThat(claim(1, 1, now)).isEqualTo(1);
        assertThat(claim(2, 1, now)).isEqualTo(1);
        assertThat(claim(1, 2, now)).isEqualTo(1);
    }

    @Test
    @DisplayName("서비스의 중복 클릭은 URL만 반환하고 회원별 최초 클릭만 누적한다")
    void serviceDeduplicatesClicks() {
        Long id = products.saveAndFlush(createProduct("service-test")).getId();
        service.recordPurchaseLinkClick(1L, id);
        assertThat(service.recordPurchaseLinkClick(1L, id).getPurchaseUrl())
                .isEqualTo("https://example.com/buy");
        service.recordPurchaseLinkClick(2L, id);
        entityManager.clear();
        assertThat(products.findById(id).orElseThrow().getClickCount()).isEqualTo(2L);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("동일 회원의 최초 클릭 8개가 동시에 요청되어도 한 번만 누적한다")
    void concurrentFirstClicks() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Long id = tx.execute(status -> products.saveAndFlush(createProduct("concurrent-test")).getId());
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < 8; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    service.recordPurchaseLinkClick(100L, id);
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) future.get(20, TimeUnit.SECONDS);
            tx.executeWithoutResult(status -> {
                assertThat(products.findById(id).orElseThrow().getClickCount()).isEqualTo(1L);
                assertThat(receipts.count()).isEqualTo(1);
            });
        } finally {
            pool.shutdownNow();
            pool.awaitTermination(5, TimeUnit.SECONDS);
            tx.executeWithoutResult(status -> {
                receipts.deleteAllInBatch();
                products.deleteById(id);
            });
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("롤백하면 집계 기록과 누적 클릭 수가 함께 취소되어 재시도할 수 있다")
    void rollbackAndRetry() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Long id = tx.execute(status -> products.saveAndFlush(createProduct("rollback-test")).getId());
        try {
            tx.executeWithoutResult(status -> {
                service.recordPurchaseLinkClick(200L, id);
                status.setRollbackOnly();
            });
            tx.executeWithoutResult(status -> {
                assertThat(products.findById(id).orElseThrow().getClickCount()).isZero();
                assertThat(receipts.count()).isZero();
                service.recordPurchaseLinkClick(200L, id);
                entityManager.clear();
                assertThat(products.findById(id).orElseThrow().getClickCount()).isEqualTo(1L);
                status.setRollbackOnly();
            });
        } finally {
            tx.executeWithoutResult(status -> products.deleteById(id));
        }
    }

    private Product createProduct(String code) {
        return Product.create(code, "테스트 상품", "https://example.com/image", 1000,
                "NAVY", ProductItemType.TOP, "https://example.com/buy");
    }
}
