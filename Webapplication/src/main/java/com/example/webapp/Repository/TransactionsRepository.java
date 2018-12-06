package com.example.webapp.Repository;

import com.example.webapp.Model.Transactions;
import com.example.webapp.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionsRepository extends JpaRepository<Transactions,String> {

    @Query("Select transaction from Transactions transaction where transaction.id = :i and transaction.user = :u")
    Transactions findUserTransactionById(
            @Param("i")String id,
            @Param("u")User u
    );

}
