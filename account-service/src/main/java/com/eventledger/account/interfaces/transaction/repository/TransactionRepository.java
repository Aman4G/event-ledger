package com.eventledger.account.interfaces.transaction.repository;

import com.eventledger.account.common.enums.TransactionType;
import com.eventledger.account.interfaces.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, String> {

    List<Transaction> findByAccountIdOrderByEventTimestampAsc(String accountId);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.accountId = :accountId
            AND t.type = :type
            """)
    BigDecimal sumAmountByAccountIdAndType(String accountId, TransactionType type);
}
