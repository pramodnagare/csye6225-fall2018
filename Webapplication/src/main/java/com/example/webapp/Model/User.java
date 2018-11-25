package com.example.webapp.Model;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Entity
@Table
public class User {

    @Id
    public String email;
    public String password;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    public List<Transactions> transactions;


    User(){
        transactions = new ArrayList<Transactions>();
    }

    public User(String email, String password) {
        this.email = email;
        this.password = password;
        transactions = new ArrayList<Transactions>();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public Transactions createTransaction(Transactions Transaction){
    	
            transactions.add(Transaction);
            return Transaction;
    }

    public Transactions updateTransaction(String id, Transactions Transaction){

        Iterator<Transactions> it = transactions.iterator();

        while(it.hasNext()){

            Transactions transact = (Transactions)it.next();
            if(transact.getId().equals(id)){
                transact.setAmount(Transaction.getAmount());
                transact.setCategory(Transaction.getCategory());
                transact.setDate(Transaction.getDate());
                transact.setDescription(Transaction.getDescription());
                transact.setMerchant(Transaction.getMerchant());
                return transact;
            }
        }

        return null;
    }

    public void deleteTransaction(Transactions Transaction){
            transactions.remove(Transaction);
    }

}
