package com.example.rest_api.Controller;

import com.example.rest_api.Entities.Transactions;
import com.example.rest_api.Service.ResponseService;
import com.example.rest_api.Service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class TransactionController {

    @Autowired
    TransactionService transactionService;

    @Autowired
    ResponseService responseService;

    @GetMapping(value="/transaction")
    public ResponseEntity getTransaction(@RequestHeader(value="Authorization")String auth){

        if(auth.isEmpty() || auth.equals("NoValueFound")){
            return new ResponseEntity("Credentials are not valid",HttpStatus.UNAUTHORIZED);
        }

        if(transactionService.getTransactions(auth) == null){
            return new ResponseEntity("Oops! You are not authorized to perform this operation!",HttpStatus.UNAUTHORIZED);
        }

        return ResponseEntity.status(HttpStatus.OK)
                .body(transactionService.getTransactions(auth));

    }

    @PostMapping(value="/transaction")
    public ResponseEntity createTransaction(@RequestHeader(value="Authorization")String auth, @RequestBody Transactions transaction){

        if(auth.isEmpty() || auth.equals("NoValueFound")){
            return new ResponseEntity("Oops! You are not authorized to perform this operation!",HttpStatus.UNAUTHORIZED);
        }

        if(transaction == null){
            return new ResponseEntity("Kindly provide transaction body!",HttpStatus.BAD_REQUEST);
        }

        if(transactionService.createTransaction(auth,transaction)){
            return ResponseEntity.status(HttpStatus.CREATED)
                        .body(transaction);
        }

        return new ResponseEntity("Oops! You are not authorized to perform this operation!",HttpStatus.UNAUTHORIZED);

    }

    @PutMapping("/transaction/{transaction_id}")
    public ResponseEntity updateTransaction(@RequestHeader(value="Authorization") String auth,
                                            @PathVariable(value="transaction_id")String transaction_id,
                                            @RequestBody Transactions transaction){

        if(transaction_id == null){
            return new ResponseEntity("Kindly provide transaction ID for the request!",HttpStatus.BAD_REQUEST);
        }

        if(transactionService.updateTransaction(auth,transaction_id,transaction) != null){
            return ResponseEntity.status(HttpStatus.OK)
                    .body(transactionService.updateTransaction(auth,transaction_id,transaction));
        }

        return new ResponseEntity("Oops! You are not authorized to perform this operation!",HttpStatus.UNAUTHORIZED);
    }

    @DeleteMapping("/transaction/{transaction_id}")
    public ResponseEntity deleteTransaction(@RequestHeader(value="Authorization") String auth,
                                            @PathVariable(value="transaction_id")String transaction_id){

        if(transaction_id == null){
            return new ResponseEntity("Kindly provide transaction ID for the request!",HttpStatus.BAD_REQUEST);
        }

        if(transactionService.deleteTransaction(auth,transaction_id)){
            return new ResponseEntity(HttpStatus.NO_CONTENT);
        }


        return new ResponseEntity("Oops! You are not authorized to perform this operation!",HttpStatus.UNAUTHORIZED);
    }

}
