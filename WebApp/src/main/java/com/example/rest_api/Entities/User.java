package com.example.rest_api.Entities;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Entity
@Table
public class User {

    @Id
    public String username;
    public String password;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    public List<Transactions> transactions;


    User(){
        transactions = new ArrayList<Transactions>();
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        transactions = new ArrayList<Transactions>();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<Transactions> getTransactions() {
        return transactions;
    }

    public Transactions addTransaction(Transactions transactionValue){
        try{
            transactions.add(transactionValue);
            return transactionValue;
        }catch (Exception e){
            return null;
        }
    }

    public Transactions updateTransaction(String id, Transactions updatedTransaction){

        Iterator it = transactions.iterator();

        while(it.hasNext()){

            Transactions transact = (Transactions)it.next();
            if(transact.getTransaction_id().equals(id)){
                transact.setAmount(updatedTransaction.getAmount());
                transact.setCategory(updatedTransaction.getCategory());
                transact.setDate(updatedTransaction.getDate());
                transact.setDescription(updatedTransaction.getDescription());
                transact.setMerchant(updatedTransaction.getMerchant());
                return transact;
            }
        }

        return null;
    }

    public void deleteTransaction(Transactions removeTransactions){

        try{
            transactions.remove(removeTransactions);
        }catch(Exception e){
            System.out.println(e.getMessage());
        }
    }

}
