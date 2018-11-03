package com.example.rest_api.Service;


import com.example.rest_api.Dao.AttachmentDao;
import com.example.rest_api.Dao.TransactionsDao;
import com.example.rest_api.Dao.UserDao;
import com.example.rest_api.Entities.Attachments;
import com.example.rest_api.Entities.Transactions;
import com.example.rest_api.Entities.User;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class AttachmentService {

    @Autowired
    UserService userService;

    @Autowired
    UserDao userDao;

    @Autowired
    TransactionService transactionService;

    @Autowired
    TransactionsDao transactionsDao;

    @Autowired
    AttachmentDao attachmentDao;

    @Autowired
    ResponseService responseService;

    //Location to store the file in local system (Destination directory)
    private String resourcePath = "src/main/resources/Cloud_Files/";


    public List<Attachments> getAllAttachments(String auth, String transcation_id) {

        String userCredentials[] = userService.getUserCredentials(auth);

        Optional<User> optionalUser = userDao.findById(userCredentials[0]);
        try {
            User user = optionalUser.get();
            //Authenticate user
            if (userService.authUser(userCredentials)) {

                //Authenticate transaction and user
                Transactions transactions = transactionsDao.findTransactionAttachedToUser(transcation_id, user);

                return transactions.getAttachmentsList();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return null;

    }

    public ResponseEntity addAttachment(String auth, String transcation_id, Attachments attachment) {

        String userCredentials[] = userService.getUserCredentials(auth);

        Optional<User> optionalUser = userDao.findById(userCredentials[0]);
        try {

            User user = optionalUser.get();
            //Authenticate user
            if (userService.authUser(userCredentials)) {

                //Authenticate transaction and user
                Transactions transaction = transactionsDao.findTransactionAttachedToUser(transcation_id, user);

                //File object for existing file
                File file = new File(attachment.getUrl());

                String extension = FilenameUtils.getExtension(file.getName());

                if (!extension.equals("jpeg") && !extension.equals("jpg") && !extension.equals("png")) {
                    return responseService.generateResponse(HttpStatus.UNAUTHORIZED,
                            "{\"Response\":\"Enter file with jpeg, jpg or png extension only\"}");
                }

                //Build new location for the file
                String newPath = resourcePath +user.getUsername()+"/"+transaction.getTransaction_id()+"/"+ file.getName();

                //Transfer file from source to destination
                FileUtils.copyFile(file,new File(newPath));

                Attachments attachments = new Attachments();
                attachments.setUrl(newPath);

                if (!attachments.getId().isEmpty()) {

                    transaction.addAttachment(attachments);
                    attachments.setTransactions(transaction);

                    //Save attachment and transaction in database
                    attachmentDao.save(attachments);
                    transactionsDao.save(transaction);
                    return responseService.generateResponse(HttpStatus.OK, attachments);

                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return new ResponseEntity(HttpStatus.UNAUTHORIZED);
    }

    public ResponseEntity updateAttachment(String auth, String transactionId,
                                           Attachments newAttachment, String attachmentId) {
        String userCredentials[] = userService.getUserCredentials(auth);
        Optional<User> optionalUser = userDao.findById(userCredentials[0]);
        try {
            User user = optionalUser.get();

            //Authenticate User
            if (userService.authUser(userCredentials)) {

                //Get transaction attached to user
                Transactions transactions = transactionsDao.findTransactionAttachedToUser(transactionId, user);

                if (transactions != null) {

                    Attachments previousAttachments = attachmentDao.findAttachmentAttachedToTransaction(attachmentId, transactions);

                    if (previousAttachments != null) {
                        File newFile = new File(newAttachment.getUrl());

                        //Build path for new file
                        String newPath = resourcePath +user.getUsername()+"/"+transactions.getTransaction_id()+"/"+ newFile.getName();

                        //Transfer file from source to destination
                        FileUtils.copyFile(newFile,new File(newPath));

                        //Delete the previous file
                        File previousFile = new File(previousAttachments.getUrl());
                        if (previousFile.exists()) {
                            previousFile.delete();
                        }

                        previousAttachments.setUrl(newPath);
                        transactionsDao.save(transactions);
                        attachmentDao.save(previousAttachments);
                        return ResponseEntity.status(HttpStatus.OK)
                                .body(newAttachment);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return new ResponseEntity(HttpStatus.UNAUTHORIZED);
    }

    public boolean deleteAttachment(String auth, String transactionId, String attachmentId) {
        String userCredentials[] = userService.getUserCredentials(auth);

        Optional<User> optionalUser = userDao.findById(userCredentials[0]);
        try {
            User user = optionalUser.get();
            if (userService.authUser(userCredentials)) {

                Transactions transactions = transactionsDao.findTransactionAttachedToUser(transactionId,user);
                if (transactions != null) {

                    List<Attachments> attachmentList = transactions.getAttachmentsList();

                    Iterator it = attachmentList.iterator();
                    while (it.hasNext()) {
                        Attachments attachments = (Attachments) it.next();
                        File fileToBeDeleted = null;
                        if (attachments.getId().equals(attachmentId)) {
                            fileToBeDeleted = new File(attachments.getUrl());
                            if (fileToBeDeleted.exists()) {
                                fileToBeDeleted.delete();
                            }
                            attachmentDao.delete(attachments);
                            return transactions.deleteAttachment(attachments);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return false;
    }


//    public boolean ifAttachmentExists(String id) {
//        Optional<Attachments> optionalAttachments = attachmentDao.findById(id);
//        try {
//            Attachments attachments = optionalAttachments.get();
//            if (attachments != null) {
//                return true;
//            }
//        } catch (Exception e) {
//            return false;
//        }
//        return false;
//    }

}
