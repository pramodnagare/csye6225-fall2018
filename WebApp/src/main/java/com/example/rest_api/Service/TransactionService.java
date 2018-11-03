package com.example.rest_api.Service;

import com.example.rest_api.Dao.TransactionsDao;
import com.example.rest_api.Dao.UserDao;
import com.example.rest_api.Entities.Transactions;
import com.example.rest_api.Entities.User;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransactionService {

    @Autowired
    TransactionsDao transactionDao;

    @Autowired
    UserDao userDao;

    @Autowired
    ResponseService responseService;

    @Autowired
    UserService userService;

    public List<Transactions> getTransactions(String auth){

        String [] userCredentials = userService.getUserCredentials(auth);
        String username = userCredentials[0];
        String password = userCredentials[1];

        Optional<User> optionalUser = userDao.findById(username);
        try{

            User user = optionalUser.get();
            if(userService.checkHash(password,user.password)){

                return user.getTransactions();

            }

        }catch(Exception e){
            return null;
        }
        return null;
    }

    public boolean createTransaction(String auth, Transactions transaction){

        String[] userCredentials = userService.getUserCredentials(auth);

        //Authenticate user
        if(userService.authUser(userCredentials)){
            User user = userDao.getOne(userCredentials[0]);
            try{

                //Generate a new transaction ID
                while(true) {
                    //Generating UUID for transaction
                    String hashedId = UUID.randomUUID().toString();

                    //If transactioin id is unique then set the id else, continue the process
                    if(!ifTransactExists(hashedId)) {
                        transaction.setTransaction_id(hashedId);
                        break;
                    }
                }

                if(transaction.getTransaction_id() != null) {
                    user.addTransaction(transaction);
                    transaction.setUser(user);
                    transactionDao.save(transaction);
                    return true;
                }

            }catch(Exception e){
                System.out.println(e.getMessage());
                return false;
            }
        }

        return false;

    }

    public Transactions updateTransaction(String auth, String id, Transactions updatedTransaction){
        String[] userCredentials = userService.getUserCredentials(auth);
        if(userService.authUser(userCredentials)){
            Optional<User> optionalUser = userDao.findById(userCredentials[0]);
                try{

                    //Finding the previous transaction attached to the user
                    Transactions existingTransaction = transactionDao.findTransactionAttachedToUser(id,optionalUser.get());

                    //updating the existing transaction with new values
                    existingTransaction.setAmount(updatedTransaction.getAmount());
                    existingTransaction.setCategory(updatedTransaction.getCategory());
                    existingTransaction.setDate(updatedTransaction.getDate());
                    existingTransaction.setDescription(updatedTransaction.getDescription());
                    existingTransaction.setMerchant(updatedTransaction.getMerchant());

                    //saving the transaction
                    transactionDao.save(existingTransaction);
                    User user = userDao.getOne(userCredentials[0]);
                    user.updateTransaction(id,updatedTransaction);
                    userDao.save(user);
                    return existingTransaction;

                }catch (Exception e){
                    System.out.print(e.getMessage());
                    return null;
                }
        }

        return null;
    }

    public boolean deleteTransaction(String auth,String id){

        String[] userCredentials = userService.getUserCredentials(auth);
        if(userService.authUser(userCredentials)) {
            Optional<User> optionalUser = userDao.findById(userCredentials[0]);

            try{
                Transactions existingTransaction = transactionDao.findTransactionAttachedToUser(id,optionalUser.get());

                transactionDao.delete(existingTransaction);
                User user = userDao.getOne(userCredentials[0]);
                user.deleteTransaction(existingTransaction);
                return true;
            }catch (Exception e){
                System.out.print(e.getMessage());
                return false;
            }

        }


        return false;
    }


    public boolean ifTransactExists(String id){

        Optional<Transactions> optionalTransactions = transactionDao.findById(id);
        try{
            Transactions transact = optionalTransactions.get();
            if(transact != null){
                return true;
            }
        }catch (Exception e){
            return false;
        }
        return false;
    }

//    public Transactions ifTransactionAttachedToUser(User user,String id){
//
//       List<Transactions> transactionsList = user.getTransactions();
//
//       Iterator it = transactionsList.iterator();
//
//       while(it.hasNext()){
//           Transactions transact = (Transactions)it.next();
//           if(id.equals(transact.getTransaction_id())){
//               return transact;
//           }
//       }
//
//        return null;
//    }


}
