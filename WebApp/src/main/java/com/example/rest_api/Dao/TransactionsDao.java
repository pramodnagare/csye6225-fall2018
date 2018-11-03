package com.example.rest_api.Dao;

import com.example.rest_api.Entities.Transactions;
import com.example.rest_api.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionsDao extends JpaRepository<Transactions,String> {

    @Query("Select transaction from Transactions  transaction " +
            "where transaction.transaction_id = :id and transaction.user = :u")
    Transactions findTransactionAttachedToUser(
            @Param("id")String id,
            @Param("u") User u
    );

}
