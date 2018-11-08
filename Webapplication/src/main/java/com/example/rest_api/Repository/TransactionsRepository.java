package com.example.rest_api.Repository;

import com.example.rest_api.Entities.Transactions;
import com.example.rest_api.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionsRepository extends JpaRepository<Transactions,String> {

    @Query("Select transaction from Transactions  transaction where transaction.transaction_id = :i and transaction.user = :u")
    Transactions findTransactionAttachedToUser(
            @Param("i")String id,
            @Param("u") User u
    );

}
