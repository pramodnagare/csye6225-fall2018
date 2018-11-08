package com.example.rest_api.Entities;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Table
public class Attachments {

    @Id
    private String id;
    private String url;

    @ManyToOne
    @JsonIgnore
    private Transactions transactions;

    public Attachments(){
        id = UUID.randomUUID().toString();
    }

    public Attachments(String id, String url, Transactions transactions){
        this.id = id;
        this.url = url;
        this.transactions = transactions;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Transactions getTransactions() {
        return transactions;
    }

    public void setTransactions(Transactions transactions) {
        this.transactions = transactions;
    }

}
