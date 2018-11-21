package com.example.webapp.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table
public class Transactions {

    @Id
    private String id;
    private String description;
    private String merchant;
    private String amount;
    private String date;
    private String category;

    @ManyToOne
    @JsonIgnore
    private User user;

    @OneToMany(fetch = FetchType.LAZY,mappedBy = "transactions")
    private List<Attachments> attachmentsList;

    public Transactions() {
        attachmentsList = new ArrayList<Attachments>();
    }

    public Transactions(String id, String description, String merchant, String amount, String date, String category) {
        this.id = id;
        this.description = description;
        this.merchant = merchant;
        this.amount = amount;
        this.date = date;
        this.category = category;


    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMerchant() {
        return merchant;
    }

    public void setMerchant(String merchant) {
        this.merchant = merchant;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<Attachments> getAttachmentsList() {
        return attachmentsList;
    }

    public Attachments createAttachment(Attachments attachment){
        try{
            attachmentsList.add(attachment);
            return attachment;
        }catch(Exception e){
            return null;
        }
    }

    public boolean removeAttachment(Attachments attachment){
            attachmentsList.remove(attachment);
            return true;
    }

}
