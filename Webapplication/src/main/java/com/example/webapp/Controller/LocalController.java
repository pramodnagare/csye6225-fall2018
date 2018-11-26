package com.example.webapp.Controller;
import com.example.webapp.Model.Attachments;
import com.example.webapp.Model.Transactions;
import com.example.webapp.Model.User;
import com.example.webapp.Repository.AttachmentRepository;
import com.example.webapp.Repository.TransactionsRepository;
import com.example.webapp.Repository.UserRepository;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.tomcat.util.codec.binary.Base64;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@RestController
@Profile("Local")
public class LocalController {


    @Autowired
    UserRepository userRepository;

    @Autowired
    TransactionsRepository transactionsRepository;
    
    @Autowired
    AttachmentRepository attachmentRepository;
    
    private String resourcePath = "src/main/resources/Cloud_Files/";


    @GetMapping(value="/transaction")
    public ResponseEntity getTransaction(@RequestHeader(value="Authorization")String auth){

        if(auth.isEmpty() || auth.equals("NoValueFound")){
            return new ResponseEntity("Credentials are not valid",HttpStatus.UNAUTHORIZED);
        }

        if(localGetTransactions(auth) == null){
            return new ResponseEntity("Oops! You are not authorized to perform this operation!",HttpStatus.UNAUTHORIZED);
        }

        return ResponseEntity.status(HttpStatus.OK)
                .body(localGetTransactions(auth));

    }

    @PostMapping(value="/transaction")
    public ResponseEntity createTransaction(@RequestHeader(value="Authorization")String auth, @RequestBody Transactions transaction){

        if(auth.isEmpty() || auth.equals("NoValueFound")){
            return new ResponseEntity("Oops! You are not authorized to perform this operation!",HttpStatus.UNAUTHORIZED);
        }

        if(transaction == null){
            return new ResponseEntity("Kindly provide transaction body!",HttpStatus.BAD_REQUEST);
        }

        if(localCreateTransaction(auth,transaction)){
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

        if(localUpdateTransaction(auth,transaction_id,transaction) != null){
            return ResponseEntity.status(HttpStatus.OK)
                    .body(localUpdateTransaction(auth,transaction_id,transaction));
        }

        return new ResponseEntity("Oops! You are not authorized to perform this operation!",HttpStatus.UNAUTHORIZED);
    }

    @DeleteMapping("/transaction/{transaction_id}")
    public ResponseEntity deleteTransaction(@RequestHeader(value="Authorization") String auth,
                                            @PathVariable(value="transaction_id")String transaction_id){

        if(transaction_id == null){
            return new ResponseEntity("Kindly provide transaction ID for the request!",HttpStatus.BAD_REQUEST);
        }

        if(localDeleteTransaction(auth,transaction_id)){
            return new ResponseEntity(HttpStatus.NO_CONTENT);
        }


        return new ResponseEntity("Oops! You are not authorized to perform this operation!",HttpStatus.UNAUTHORIZED);
    }

    @RequestMapping(value="/time", method = RequestMethod.GET)
    public ResponseEntity authAndLogin(@RequestHeader(value="Authorization")String auth){

        if(auth.isEmpty() || auth.equals("NoValueFound")){
            return localGenerateResponse(HttpStatus.UNAUTHORIZED,
                    "{\"Response\":\"You are not logged in! Please login and try again\"}");
        }

        return localAuthUser(auth);

    }

    @RequestMapping(value="/user/register",method=RequestMethod.POST)
    public ResponseEntity register(@RequestHeader(value="Authorization")String auth){

        if(auth.isEmpty() || auth.equals("NoValueFound")){
            return localGenerateResponse(HttpStatus.UNAUTHORIZED,
                        "{\"Response\":\"Kindly provide credentials! Try again!\"}");
        }

        return localCreateUser(auth);

    }
    
    @GetMapping("/transaction/{transaction_id}/attachments")
    public ResponseEntity getAllAttachments(@RequestHeader(value="Authorization")String Auth,
                                            @PathVariable(value = "transaction_id")String transactionId){

        if(!Auth.equals("NoAuth") && !transactionId.isEmpty()){
            List<Attachments> attachmentsList = localGetAllAttachments(Auth,transactionId);

                if(attachmentsList != null)
                    return ResponseEntity.status(HttpStatus.OK)
                            .body(attachmentsList);

        }

        return new ResponseEntity(HttpStatus.UNAUTHORIZED);
    }

    @DeleteMapping("/transaction/{transaction_id}/attachments/{attachment_id}")
    public ResponseEntity deleteAttachment(@RequestHeader(value="Authorization"
    )String auth,
                                           @PathVariable(value="transaction_id")String transactionId,
                                           @PathVariable(value="attachment_id")String attachmentId){

        if(!auth.equals("NoAuth") && !transactionId.isEmpty() && !attachmentId.isEmpty()){
            if(localDeleteAttachment(auth,transactionId,attachmentId)){
                return new ResponseEntity(HttpStatus.NO_CONTENT);
            }
        }

        return new ResponseEntity("Oops! You are not authorized to perform this operation!",HttpStatus.UNAUTHORIZED);
    }
    
    @PostMapping("/transaction/{transaction_id}/attachments")
    public ResponseEntity addAttachment(@RequestHeader(value="Authorization")String auth,
                                        @PathVariable(value="transaction_id")String transactionId,
                                        @RequestBody Attachments attachments){

        if(!auth.equals("NoAuth") && !transactionId.isEmpty()){
           return localAddAttachment(auth,transactionId,attachments);
        }

        return new ResponseEntity("Oops! You are not authorized to perform this operation!",HttpStatus.UNAUTHORIZED);
    }
    
    @PutMapping("/transaction/{transaction_id}/attachments/{attachment_id}") 
    public ResponseEntity updateTransaction(@RequestHeader(value="Authorization")String auth,
                                            @PathVariable(value="transaction_id")String transactionId,
                                            @PathVariable(value="attachment_id")String attachmentId,
                                            @RequestBody Attachments attachment){

        if(!auth.equals("NoAuth") && !transactionId.isEmpty() && attachment != null && !attachmentId.isEmpty()){
            return localUpdateAttachment(auth,transactionId,attachment,attachmentId);
        }

        return new ResponseEntity("Oops! You are not authorized to perform this operation!",HttpStatus.UNAUTHORIZED);
    }

    public List<Attachments> localGetAllAttachments(String auth, String transcation_id) {

        String userCredentials[] = localGetUserCredentials(auth);

        Optional<User> optionalUser = userRepository.findById(userCredentials[0]);
        try {
            User user = optionalUser.get();
            if (localAuthUser(userCredentials)) {

                Transactions transactions = transactionsRepository.findUserTransactionById(transcation_id, user);

                return transactions.getAttachmentsList();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return null;

    }

    public ResponseEntity<?> localAddAttachment(String auth, String transcation_id, Attachments attachment) {

        String userCredentials[] = localGetUserCredentials(auth);

        Optional<User> optionalUser = userRepository.findById(userCredentials[0]);
        try {

            User user = optionalUser.get();
            if (localAuthUser(userCredentials)) {

                Transactions transaction = transactionsRepository.findUserTransactionById(transcation_id, user);

                File file = new File(attachment.getUrl());

                String extension = FilenameUtils.getExtension(file.getName());

                if (!extension.equals("jpeg") && !extension.equals("jpg") && !extension.equals("png")) {
                    return localGenerateResponse(HttpStatus.UNAUTHORIZED,
                            "{\"Response\":\"Enter file with jpeg, jpg or png extension only\"}");
                }

                String newPath = resourcePath +user.getEmail()+"/"+transaction.getId()+"/"+ file.getName();

                FileUtils.copyFile(file,new File(newPath));

                Attachments attachments = new Attachments();
                attachments.setUrl(newPath);

                if (!attachments.getId().isEmpty()) {

                    transaction.createAttachment(attachments);
                    attachments.setTransactions(transaction);
                    attachmentRepository.save(attachments);
                    transactionsRepository.save(transaction);
                    return localGenerateResponse(HttpStatus.OK, attachments);

                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return new ResponseEntity(HttpStatus.UNAUTHORIZED);
    }

    public ResponseEntity<Attachments> localUpdateAttachment(String auth, String transactionId,
                                           Attachments newAttachment, String attachmentId) {
        String userCredentials[] = localGetUserCredentials(auth);
        Optional<User> optionalUser = userRepository.findById(userCredentials[0]);
        try {
            User user = optionalUser.get();

            if (localAuthUser(userCredentials)) {

                Transactions transactions = transactionsRepository.findUserTransactionById(transactionId, user);

                if (transactions != null) {

                    Attachments previousAttachments = attachmentRepository.findTransactionAttachmentById(attachmentId, transactions);

                    if (previousAttachments != null) {
                        File newFile = new File(newAttachment.getUrl());

                        String newPath = resourcePath +user.getEmail()+"/"+transactions.getId()+"/"+ newFile.getName();

                        FileUtils.copyFile(newFile,new File(newPath));

                        File previousFile = new File(previousAttachments.getUrl());
                        if (previousFile.exists()) {
                            previousFile.delete();
                        }

                        previousAttachments.setUrl(newPath);
                        transactionsRepository.save(transactions);
                        attachmentRepository.save(previousAttachments);
                        return ResponseEntity.status(HttpStatus.OK)
                                .body(newAttachment);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return new ResponseEntity<Attachments>(HttpStatus.UNAUTHORIZED);
    }

    public boolean localDeleteAttachment(String auth, String transactionId, String attachmentId) {
        String userCredentials[] = localGetUserCredentials(auth);

        Optional<User> optionalUser = userRepository.findById(userCredentials[0]);
        try {
            User user = optionalUser.get();
            if (localAuthUser(userCredentials)) {

                Transactions transactions = transactionsRepository.findUserTransactionById(transactionId,user);
                if (transactions != null) {

                    List<Attachments> attachmentList = transactions.getAttachmentsList();

                    Iterator<Attachments> it = attachmentList.iterator();
                    while (it.hasNext()) {
                        Attachments attachments = (Attachments) it.next();
                        File fileToBeDeleted = null;
                        if (attachments.getId().equals(attachmentId)) {
                            fileToBeDeleted = new File(attachments.getUrl());
                            if (fileToBeDeleted.exists()) {
                                fileToBeDeleted.delete();
                            }
                            attachmentRepository.delete(attachments);
                            return transactions.removeAttachment(attachments);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return false;
    }
    
    public ResponseEntity<?> localAuthUser(String auth){

        String []userCredentials = localGetUserCredentials(auth);

        if(userCredentials.length == 0){
            return localGenerateResponse(HttpStatus.UNAUTHORIZED
                    ,"{\"Response\":\"You are not logged in! Please login and then try!\"}");
        }

        String regex = "[0-9A-Za-z]+@[0-9A-Za-z]+\\.[A-Za-z]{2,}";
        
        if(!userCredentials[0].matches(regex)) {
        	return localGenerateResponse(HttpStatus.BAD_GATEWAY
                    ,"(\"Response\":\"Username format is invalid! Please try again!\"}");
        }
        
        /*
        if(!validateService.validateUsername(userCredentials[0])){
            return localGenerateResponse(HttpStatus.BAD_GATEWAY
                    ,"(\"Response\":\"Username format is invalid! Please try again!\"}");
        }
        */

        try {
            if(localAuthUser(userCredentials)){
                DateFormat df = new SimpleDateFormat("HH:mm");
                Date date = new Date();
                return localGenerateResponse(HttpStatus.OK
                                ,"{\"Date\":\""+df.format(date)+"\"}");
            }
        }catch(NoSuchElementException e){
            return localGenerateResponse(HttpStatus.UNAUTHORIZED
                            ,"{\"Response\":\"No such account in the system! Please register!\"}");

        }

        return localGenerateResponse(HttpStatus.UNAUTHORIZED
                ,"{\"Response\":\"No such account in the system! Please register!\"}");
    }

    public ResponseEntity<?> localCreateUser(String auth){
        String []userCredentials = localGetUserCredentials(auth);

        if(userCredentials.length == 0){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("{\"Response\":\"Username or Password cannot be blank! Please Try again!\"}");
        }
        
        String regex = "[0-9A-Za-z]+@[0-9A-Za-z]+\\.[A-Za-z]{2,}";
        if(!userCredentials[0].matches(regex)) {
        	return localGenerateResponse(HttpStatus.BAD_GATEWAY
                    ,"{\"Response\":\"Username has to be valid email-ID! Please try again!\"}");
        }


        if(localAuthUser(userCredentials)){
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("{\"Response\":\"This account already exists in the system!\"}");
        }else{
            try {

                String username = userCredentials[0];
                String password = userCredentials[1];

                String hashedPassword = localHash(password);

                User user = new User(username, hashedPassword);

                userRepository.save(user);

                return ResponseEntity.status(HttpStatus.OK)
                            .body("{\"Response\":\"User Account has been successfully created!\"}");
            }
            catch(Exception e){
                System.out.print(e.getMessage());
                return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                            .body("{\"Response\":\"Error while crearting user account!\"}");
            }
        }

    }

    protected boolean localAuthUser(String []userCredentials){

        String username = userCredentials[0];
        String password = userCredentials[1];

        Optional<User> optionalUser = userRepository.findById(username);

        try{
            User user = optionalUser.get();
            return localCheckHash(password,user.password);

        }catch (Exception e){
            return false;
        }

    }


    protected String localHash(String password){
        if(password.isEmpty() || password == null){
            return null;
        }
        Random rand = new Random();
        int log_rounds = rand.nextInt(12);

        return BCrypt.hashpw(password,BCrypt.gensalt(log_rounds));
    }

    protected boolean localCheckHash(String password,String hash){
        if(password.isEmpty() || password == null){
            return false;
        }

        return BCrypt.checkpw(password,hash);
    }

    protected String[] localGetUserCredentials(String auth){
        String []authParts = auth.split(" ");

        byte[] decode = Base64.decodeBase64(authParts[1]);

        return new String(decode).split(":");
    }


    public List<Transactions> localGetTransactions(String auth){

        String [] userCredentials = localGetUserCredentials(auth);
        String username = userCredentials[0];
        String password = userCredentials[1];

        Optional<User> optionalUser = userRepository.findById(username);
        try{

            User user = optionalUser.get();
            if(localCheckHash(password,user.password)){

                return user.getTransactions();

            }

        }catch(Exception e){
            return null;
        }
        return null;
    }

    public boolean localCreateTransaction(String auth, Transactions transaction){

        String[] userCredentials = localGetUserCredentials(auth);

        if(localAuthUser(userCredentials)){
            User user = userRepository.getOne(userCredentials[0]);
            try{

                while(true) {
                    String hashedId = UUID.randomUUID().toString();

                    if(!localIfTransactExists(hashedId)) {
                        transaction.setId(hashedId);
                        break;
                    }
                }

                if(transaction.getId() != null) {
                    user.createTransaction(transaction);
                    transaction.setUser(user);
                    transactionsRepository.save(transaction);
                    return true;
                }

            }catch(Exception e){
                System.out.println(e.getMessage());
                return false;
            }
        }

        return false;

    }

    public Transactions localUpdateTransaction(String auth, String id, Transactions updatedTransaction){
        String[] userCredentials = localGetUserCredentials(auth);
        if(localAuthUser(userCredentials)){
            Optional<User> optionalUser = userRepository.findById(userCredentials[0]);
                try{

                    Transactions existingTransaction = transactionsRepository.findUserTransactionById(id,optionalUser.get());

                    existingTransaction.setAmount(updatedTransaction.getAmount());
                    existingTransaction.setCategory(updatedTransaction.getCategory());
                    existingTransaction.setDate(updatedTransaction.getDate());
                    existingTransaction.setDescription(updatedTransaction.getDescription());
                    existingTransaction.setMerchant(updatedTransaction.getMerchant());

                    transactionsRepository.save(existingTransaction);
                    User user = userRepository.getOne(userCredentials[0]);
                    user.updateTransaction(id,updatedTransaction);
                    userRepository.save(user);
                    return existingTransaction;

                }catch (Exception e){
                    System.out.print(e.getMessage());
                    return null;
                }
        }

        return null;
    }

    public boolean localDeleteTransaction(String auth,String id){

        String[] userCredentials = localGetUserCredentials(auth);
        if(localAuthUser(userCredentials)) {
            Optional<User> optionalUser = userRepository.findById(userCredentials[0]);

            try{
                Transactions existingTransaction = transactionsRepository.findUserTransactionById(id,optionalUser.get());

                transactionsRepository.delete(existingTransaction);
                User user = userRepository.getOne(userCredentials[0]);
                user.deleteTransaction(existingTransaction);
                return true;
            }catch (Exception e){
                System.out.print(e.getMessage());
                return false;
            }

        }


        return false;
    }


    public boolean localIfTransactExists(String id){

        Optional<Transactions> optionalTransactions = transactionsRepository.findById(id);
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
    

        public ResponseEntity localGenerateResponse(HttpStatus status, Object reason){

            if(reason == null){
                return new ResponseEntity(status);
            }
            return ResponseEntity.status(status)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(reason);
        }

}
