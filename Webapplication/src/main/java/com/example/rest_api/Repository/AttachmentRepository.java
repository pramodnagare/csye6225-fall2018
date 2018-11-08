package com.example.rest_api.Repository;


import com.example.rest_api.Entities.Attachments;
import com.example.rest_api.Entities.Transactions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttachmentRepository extends JpaRepository<Attachments,String> {

    @Query("Select attachments from Attachments attachments where attachments.id = :i and attachments.transactions = :t ")
    Attachments findAttachmentAttachedToTransaction(
      @Param("i")String id,
      @Param("t") Transactions transactions
    );

}
