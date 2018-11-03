package com.example.rest_api.Entities;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Entity
@Table
public class Transactions {

    @Id
    private String transaction_id;
    private String description;
    private String merchant;
    private String amount;
    private String date;
    private String category;

    @ManyToOne
    @JsonIgnore
    private User user;

    @OneToMany(fetch = FetchType.LAZY,mappedBy = "transactions")
    @JsonIgnore
    private List<Attachments> attachmentsList;

    public Transactions() {
        attachmentsList = new ArrayList<Attachments>();
    }

    public Transactions(String transaction_id, String description, String merchant, String amount, String date, String category) {
        this.transaction_id = transaction_id;
        this.description = description;
        this.merchant = merchant;
        this.amount = amount;
        this.date = date;
        this.category = category;


    }

    public String getTransaction_id() {
        return transaction_id;
    }

    public void setTransaction_id(String transaction_id) {
        this.transaction_id = transaction_id;
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

    public Attachments addAttachment(Attachments newAttachments){
        try{
            attachmentsList.add(newAttachments);
            return newAttachments;
        }catch(Exception e){
            return null;
        }
    }

//    public Attachments getAttachment(String previousAttachmentId){
//        Iterator it = attachmentsList.iterator();
//        while(it.hasNext()){
//            Attachments attachments = (Attachments) it.next();
//            if(attachments.getId().equals(previousAttachmentId)){
//                return attachments;
//            }
//        }
//        return null;
//    }

    public boolean deleteAttachment(Attachments deleteAttachment){
        try{
            attachmentsList.remove(deleteAttachment);
            return true;
        }catch (Exception e){
            return false;
        }
    }

//    public Attachments getPreviousAttachment(String previousAttachmentId){
//
//        Iterator it = attachmentsList.iterator();
//        while(it.hasNext()){
//            Attachments attachments = (Attachments) it.next();
//            if(attachments.getId().equals(previousAttachmentId)){
//                return attachments;
//            }
//        }
//
//        return null;
//    }
}
