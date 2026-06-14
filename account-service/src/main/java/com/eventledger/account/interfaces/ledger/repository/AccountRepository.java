package com.eventledger.account.interfaces.ledger.repository;

import com.eventledger.account.interfaces.ledger.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, String> {
}