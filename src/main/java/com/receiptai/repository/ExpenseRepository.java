package com.receiptai.repository;

import com.receiptai.dto.BestPriceProduct;
import com.receiptai.dto.basket.BestDealItem;
import com.receiptai.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    @Query("""
        SELECT new com.receiptai.dto.BestPriceProduct(
            r.storeName,
            MIN(e.price),
            AVG(e.price)
        )
        FROM Expense e
        JOIN e.receipt r
        WHERE LOWER(e.name) LIKE LOWER(CONCAT('%', :productName, '%'))
          AND r.date >= :startDate
        GROUP BY r.storeName
        ORDER BY MIN(e.price) ASC
    """)
    List<BestPriceProduct> findBestPricesForProduct(
            @Param("productName") String productName,@Param("startDate") LocalDate startDate
    );

    @Query("SELECT new com.receiptai.dto.basket.BestDealItem(e.id, e.name, r.storeName, e.price) " +
            "FROM Expense e JOIN e.receipt r " +
            "WHERE r.date >= :date")
    List<BestDealItem> findProductsSince(@Param("date") LocalDate date);

    @Query("SELECT r.storeName FROM Expense e JOIN e.receipt r WHERE e.id = :id")
    String findStoreNameByExpenseId(@Param("id") Long id);
}
