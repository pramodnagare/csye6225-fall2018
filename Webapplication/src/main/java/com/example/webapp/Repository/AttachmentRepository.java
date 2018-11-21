package com.example.webapp.Repository;


import com.example.webapp.Model.Attachments;
import com.example.webapp.Model.Transactions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttachmentRepository extends JpaRepository<Attachments,String> {

    @Query("Select attachments from Attachments attachments where attachments.id = :i and attachments.transactions = :t ")
    Attachments findTransactionAttachmentById(
      @Param("i")String id,
      @Param("t")Transactions transactions
    );

}
