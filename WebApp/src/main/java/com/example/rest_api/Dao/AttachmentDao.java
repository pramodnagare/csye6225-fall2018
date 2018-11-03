package com.example.rest_api.Dao;


import com.example.rest_api.Entities.Attachments;
import com.example.rest_api.Entities.Transactions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttachmentDao extends JpaRepository<Attachments,String> {

    @Query("Select attachments from Attachments attachments " +
            "where attachments.id = :id and attachments.transactions = :transactions ")
    Attachments findAttachmentAttachedToTransaction(
      @Param("id")String id,
      @Param("transactions") Transactions transactions
    );

}
