package com.example.webapp.Controller;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.example.webapp.Model.Attachments;
import com.example.webapp.Model.Transactions;
import com.example.webapp.Model.User;
import com.example.webapp.Repository.AttachmentRepository;
import com.example.webapp.Repository.LogHelper;
import com.example.webapp.Repository.TransactionsRepository;
import com.example.webapp.Repository.UserRepository;
import com.timgroup.statsd.StatsDClient;
import org.apache.commons.io.FilenameUtils;
import org.apache.tomcat.util.codec.binary.Base64;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.net.URL;
import java.util.*;


@RestController
@Profile("Dev")
public class DevController {

    @Autowired
    UserRepository userRepository;

    @Autowired
    TransactionsRepository transactionsRepository;
    
    @Autowired
    AttachmentRepository attachmentRepository;
    
    private String clientRegion = "us-east-1";

    private String bucketName = "";

    @Autowired
    private StatsDClient statsDClient;
    LogHelper logger = new LogHelper();

    public AWSCredentialsProviderChain getchain(){
        AWSCredentialsProviderChain chain  = new AWSCredentialsProviderChain(
                InstanceProfileCredentialsProvider.getInstance(),new ProfileCredentialsProvider()
        );
        return chain;
    }




    @GetMapping(value="/password_reset")
    public ResponseEntity<?> getResetPassword(@RequestHeader(value="Authorization")String auth) throws Exception {
        statsDClient.incrementCounter("endpoint.password_reset.api.get");
        logger.logInfoEntry("password_reset get is started");

        if (!auth.isEmpty()) {

            String[] creds = devGetcreds(auth);

            if (creds.length == 0) {
                logger.logInfoEntry("Please login and then try of get method of password_reset ");

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON).body("{\"Response\":\"You are not logged in! Please login and then try!\"}");

            }

            String regex = "[.0-9A-Za-z]+@[.0-9A-Za-z]+\\.[A-Za-z]{2,}";

            if (!creds[0].matches(regex)) {

                logger.logInfoEntry("wrong email id of get method of password_reset ");

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body("{\"Response\":\"Email format is invalid! Please try again!\"}");

            }


            try {
                if (devAuthUser(creds)) {

                    AmazonSNS sns = AmazonSNSClientBuilder.standard().withCredentials(getchain()).build();

                    String userName = creds[0];
                    String table = System.getProperty("dynamoDB");
                    System.out.println("DynamoDB table:" + table);
                    InetAddress myHost = InetAddress.getLocalHost();
                    String ec2 = myHost.getHostAddress();
                    System.out.println("ec2:" + ec2);
                    String emailHost = System.getProperty("emailHost");
                    System.out.println("emailHost:" + emailHost);
                    String msg = userName + "|" + emailHost + "|" + table + "|" + ec2;
                    System.out.println("msg:" + msg);
                    String topicARN = System.getProperty("topicARN");
                    System.out.println(topicARN);
                    sns.publish(topicARN, msg);

                    return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body("{\"Response\":\"Email for password reset has been sent!\"}");

                }
            } catch (NoSuchElementException e) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON).body("{\"Response\":\"No such account in the system! Please register!\"}");

            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON).body("{\"Response\":\"No such account in the system! Please register!\"}");
        } else {

            logger.logInfoEntry("wrong credentials of get method of password_reset ");

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON).body("{\"Response\":\"Credentials are not valid\"}");

        }


    }
    
    @GetMapping(value="/transaction")
    public ResponseEntity<?> getTransaction(@RequestHeader(value="Authorization")String auth) throws Exception {
        statsDClient.incrementCounter("endpoint.transaction.api.get");
        logger.logInfoEntry("transaction get is started");

        if(auth.isEmpty()){
            logger.logInfoEntry("transaction get where auth is wrong");

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON).body("{\"Response\":\"Credentials are not valid\"}");
        	
        }

        if(devGetTransactions(auth) == null){

            logger.logInfoEntry("transaction get where wrong user");

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON).body("{\"Response\":\"Oops! You are not authorized to perform this operation!\"}");
        	
        }

        return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(devGetTransactions(auth));

    }

    @PostMapping(value="/transaction")
    public ResponseEntity<?> createTransaction(@RequestHeader(value="Authorization")String auth, @RequestBody Transactions t) throws Exception {
        statsDClient.incrementCounter("endpoint.transaction.api.post");
        logger.logInfoEntry("Post Transaction Started ");

        if(auth.isEmpty()){
            logger.logInfoEntry("Authorization Issues of post method of Transaction ");
        	return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON).body("{\"Response\":\"Oops! You are not authorized to perform this operation!\"}");

        }

        if(t == null){
            logger.logInfoEntry("Transaction issues of post method of Transaction ");
        	return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body("{\"Response\":\"Kindly provide transaction body!\"}");

        }

        String[] creds = devGetcreds(auth);

        if(devAuthUser(creds)){
            User user = userRepository.getOne(creds[0]);
        	String hId = UUID.randomUUID().toString() + UUID.randomUUID().toString();
        	t.setId(hId);
            user.createTransaction(t);
            t.setUser(user);
            transactionsRepository.save(t);
            return ResponseEntity.status(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(t);
        }


        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON).body("{\"Response\":\"Oops! You are not authorized to perform this operation!\"}");

    }
    

    @PutMapping("/transaction/{id}")
    public ResponseEntity<?> updateTransaction(@RequestHeader(value="Authorization") String auth,
                                            @PathVariable(value="id")String id,
                                            @RequestBody Transactions transaction) throws Exception {


        statsDClient.incrementCounter("endpoint.transaction.id.api.put");
        logger.logInfoEntry(" put method of transaction is started");

        if(id == null){
            logger.logInfoEntry(" put method of transaction where id is null");

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body("{\"Response\":\"Kindly provide transaction ID for the request!\"}");
        }

        if(devUpdateTransaction(auth,id,transaction) != null){
        	
        	return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(devUpdateTransaction(auth,id,transaction));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON).body("{\"Response\":\"Oops! You are not authorized to perform this operation!\"}");
    }

    @DeleteMapping("/transaction/{id}")
    public ResponseEntity<?> deleteTransaction(@RequestHeader(value="Authorization") String auth,
                                            @PathVariable(value="id")String id) throws Exception {
        statsDClient.incrementCounter("endpoint.transaction.id.api.delete");
        logger.logInfoEntry("delete method of transaction has started");
        if(id == null){
            logger.logInfoEntry("id of delete transaction is null");
        	return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body("{\"Response\":\"Kindly provide transaction ID for the request!\"}");
        	
        }

        String[] creds = devGetcreds(auth);
        if(devAuthUser(creds)) {
            Optional<User> u = userRepository.findById(creds[0]);

            try{
                Transactions t = transactionsRepository.findUserTransactionById(id,u.get());
                List<Attachments> ats = devGetAllAttachments(auth, t.getId());
                if(ats!=null) {
                    logger.logInfoEntry("list is null");
                	for (Attachments a : ats) {
                		devDeleteAttachment(auth, t.getId(), a.getId());
                	}
                }
                
                User user = userRepository.getOne(creds[0]);
                user.deleteTransaction(t);
                transactionsRepository.delete(t);
                return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body("{\"Response\":\"Your Transaction deleted successfully!\"}");
            }catch (Exception e){
                System.out.print(e.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON).body("{\"Response\":\"Oops! Something went wrong! Please try again!\"}");
            }

        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON).body("{\"Response\":\"Oops! You are not authorized to perform this operation!\"}");
        
    }
    

    @GetMapping("/time")
    public ResponseEntity<?> authAndLogin(@RequestHeader(value="Authorization")String auth) throws Exception {

        statsDClient.incrementCounter("endpoint.time.api.get");
        logger.logInfoEntry("Get method of time has started");
        if(auth.isEmpty()){
            logger.logInfoEntry("Get method of time where authorization failed");
        	return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON).body("{\"Response\":\"You are not logged in! Please Login and Try again!\"}");
        	
        }

        return devAuthUser(auth);

    }

    @PostMapping("/user/register")
    public ResponseEntity<?> register(@RequestHeader(value="Authorization")String auth) throws Exception {
        statsDClient.incrementCounter("endpoint.user.register.api.post");
        logger.logInfoEntry("Post method of user registration has started");
        if(auth.isEmpty()){
            logger.logInfoEntry("Post method of user registration where authorization failed");
        	return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON).body("{\"Response\":\"Kindly provide credentials! Try again!\"}");
        	
        }

        return devCreateUser(auth);

    }

    @GetMapping("/transaction/{id}/attachments")
    public ResponseEntity<?> getAllAttachments(@RequestHeader(value="Authorization")String Auth,
                                            @PathVariable(value = "id")String id) throws Exception {
        statsDClient.incrementCounter("endpoint.transaction.id.attachments.api.get");
        logger.logInfoEntry("Get Method of Attachments strated");
        if(!Auth.isEmpty() && !id.isEmpty()){
            List<Attachments> attachmentsList = devGetAllAttachments(Auth,id);

            if(attachmentsList != null)
                logger.logInfoEntry("Attachment list is  not empty");
            	return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(attachmentsList);

        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON).body("{\"Response\":\"Oops! You are not authorized to perform this operation!\"}");
        
    }

    @DeleteMapping("/transaction/{id}/attachments/{idAttachment}")
    public ResponseEntity<?> deleteAttachment(@RequestHeader(value="Authorization")String auth,
                                           @PathVariable(value="id")String id,
                                           @PathVariable(value="idAttachment")String idAttachment) throws Exception {

        statsDClient.incrementCounter("endpoint.transaction.id.attachments.idAttachment.api.delete");
        logger.logInfoEntry("Inside delete of Attachments Bad request");

        if(!auth.isEmpty() && !id.isEmpty() && !idAttachment.isEmpty()){
            if(devDeleteAttachment(auth,id,idAttachment)){
            	return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body("{\"Response\":\"The requested attachment has been deleted successfully!\"}");
            	
            }
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON).body("{\"Response\":\"Oops! You are not authorized to perform this operation!\"}");
        
    }

    @PostMapping("/transaction/{id}/attachments")
    public ResponseEntity<?> addAttachment(@RequestHeader(value="Authorization")String auth,
                                        @PathVariable(value="id")String id,
                                        @RequestParam("file")MultipartFile attachments) throws Exception {
        statsDClient.incrementCounter("endpoint.transaction.id.attachments.api.post");
        logger.logInfoEntry("Post method of Attachmnets has started");

        if(!auth.isEmpty() && !id.isEmpty()){
        	
            return devAddAttachment(auth,id,attachments);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON).body("{\"Response\":\"Oops! You are not authorized to perform this operation!\"}");
        
    }

    
    @PutMapping("/transaction/{id}/attachments/{idAttachment}")
    public ResponseEntity<?> updateTransaction(@RequestHeader(value="Authorization",defaultValue = "NoAuth")String auth,
                                            @PathVariable(value="id")String id,
                                            @PathVariable(value="idAttachment")String idAttachment,
                                            @RequestParam("file") MultipartFile attachment) throws Exception {
        statsDClient.incrementCounter("endpoint.transaction.id.attachments.idAttachment.api.put");
        logger.logInfoEntry(" put of attachments is started");

        if(!auth.isEmpty() && !id.isEmpty() && attachment != null && !idAttachment.isEmpty()){
            return devUpdateAttachment(auth,id,attachment,idAttachment);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON).body("{\"Response\":\"Oops! You are not authorized to perform this operation!\"}");
        
    }
    
    public List<Attachments> devGetAllAttachments(String auth, String id) {

        String creds[] = devGetcreds(auth);


        Optional<User> U = userRepository.findById(creds[0]);
        try {

            User user = U.get();
            if (devAuthUser(creds)) {

                Transactions transactions = transactionsRepository.findUserTransactionById(id, user);

                return transactions.getAttachmentsList();
            }
        } catch (Exception e) {
        }
        return null;

    }

    public ResponseEntity<?> devAddAttachment(String auth, String id, MultipartFile multiPartFile) {
        String creds[] = devGetcreds(auth);

        Optional<User> U = userRepository.findById(creds[0]);
        try {

            User user = U.get();
            if (devAuthUser(creds)) {

                Transactions transaction = transactionsRepository.findUserTransactionById(id, user);

                File file = new File(multiPartFile.getOriginalFilename());
                String extension = FilenameUtils.getExtension(file.getName());
                extension.toLowerCase();
                if (!extension.equalsIgnoreCase("jpeg") && !extension.equalsIgnoreCase("jpg") && !extension.equalsIgnoreCase("png")) {
                    System.out.print(extension);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body("{\"Response\":\"Attachment only supported for jpeg, jpg or png file extensions! Please try again!\"}");
                    
                }

                file.setWritable(true);
                FileOutputStream fos = new FileOutputStream("/opt/tomcat/uploads/"+file);
                fos.write(multiPartFile.getBytes());
                fos.close();


                String newPath = devUploadToS3(multiPartFile, file.getName(),user.getEmail());

                if (newPath == null) {
                	return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body("{\"Response\":\"This request for upload to s3 has been failed! Please try again!\"}");
                	
                }

                Attachments newAttachment = new Attachments();
                newAttachment.setUrl(newPath);

                if (!newAttachment.getId().isEmpty() && !newAttachment.getUrl().isEmpty()) {
                    transaction.createAttachment(newAttachment);
                    newAttachment.setTransactions(transaction);
                    attachmentRepository.save(newAttachment);
                    transactionsRepository.save(transaction);
                    return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(newAttachment);
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON).body("{\"Response\":\"Oops! You are not authorized to perform this operation!\"}");
        
    }

    public ResponseEntity<?> devUpdateAttachment(String auth, String id,
                                           MultipartFile multiPartFile, String attachmentId) {
        String creds[] = devGetcreds(auth);

        Optional<User> U = userRepository.findById(creds[0]);
        try {

            User user = U.get();
            if (devAuthUser(creds)) {
                Transactions transactions = transactionsRepository.findUserTransactionById(id, user);

                Attachments previousAttachment = attachmentRepository.findTransactionAttachmentById(attachmentId, transactions);

                if (previousAttachment != null) {
                    //URL fileUrl = new URL(previousAttachment.getUrl());

                    File file = new File(multiPartFile.getOriginalFilename());

                    String extension = FilenameUtils.getExtension(file.getName());
                    extension.toLowerCase();
                    if (!extension.equalsIgnoreCase("jpeg") && !extension.equalsIgnoreCase("jpg") && !extension.equalsIgnoreCase("png")) {
                        System.out.print(extension);
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body("{\"Response\":\"Enter file with jpeg, jpg or png extension only! Try again!\"}");
                        
                    }

                    file.setWritable(true);
                    FileOutputStream fos = new FileOutputStream("/opt/tomcat/uploads/"+file);
                    fos.write(multiPartFile.getBytes());
                    fos.close();

                    String objectKeyName = FilenameUtils.getName(previousAttachment.getUrl());

                    String updatedUrl = updateInS3(multiPartFile, file.getName(),objectKeyName, user.getEmail());
                    if (updatedUrl != null) {
                        previousAttachment.setUrl(updatedUrl);
                        attachmentRepository.save(previousAttachment);
                        transactionsRepository.save(transactions);

                        return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(previousAttachment);
                        
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON).body("{\"Response\":\"Oops! You are not authorized to perform this operation!\"}");
        
    }

    public boolean devDeleteAttachment(String auth, String id, String attachmentId) {
        String creds[] = devGetcreds(auth);

        Optional<User> U = userRepository.findById(creds[0]);
        try {

            User user = U.get();
            if (devAuthUser(creds)) {

                Transactions transactions = transactionsRepository.findUserTransactionById(id, user);
                if (transactions != null) {
                    Attachments attachments = attachmentRepository.findTransactionAttachmentById(attachmentId, transactions);
                    URL existingURL = null;

                    if (attachments.getId().equals(attachmentId))
                        existingURL = new URL(attachments.getUrl());
                        String objectKeyName = FilenameUtils.getName(existingURL.getPath());
                    if (objectKeyName != null) {
                        if (devDeleteInS3(objectKeyName)) {
                            attachmentRepository.delete(attachments);
                        }
                    }
                    return transactions.removeAttachment(attachments);
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return false;
    }



    public String devUploadToS3(MultipartFile fileUrl,String fileName, String email) {

        Random random = new Random();
        float randomNumber = random.nextFloat();

        String fileObjectKeyName = email +"_"+String.valueOf(randomNumber)+"_"+FilenameUtils.getName(fileName);

        try {

            AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                    .withRegion(clientRegion)
                    .withCredentials(new DefaultAWSCredentialsProviderChain())
                    .build();

            if(!devGetBucketName(s3Client)){
                System.out.println("Please confirm the bucket name and Try again!");
                return null;
            }

            if(bucketName == null ||bucketName.isEmpty()){
                return null;
            }

            PutObjectRequest request = new PutObjectRequest(bucketName, fileObjectKeyName, fileUrl.getInputStream(),null);
            s3Client.putObject(request);
            String bucketUrl = " https://s3.amazonaws.com/" + bucketName + "/" + fileObjectKeyName;

            return bucketUrl;
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return null;
    }

    public String updateInS3(MultipartFile attachments, String fileName,String oldObjectKeyName, String email) {

        if (devDeleteInS3(oldObjectKeyName)) {
            return devUploadToS3(attachments,fileName,email);
        }

        return null;
    }

    public boolean devDeleteInS3(String url) {

        try {

            AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                    .withRegion(clientRegion)
                    .withCredentials(new DefaultAWSCredentialsProviderChain())
                    .build();

            if(bucketName == null || bucketName.isEmpty()){
                if(!devGetBucketName(s3Client)){
                    System.out.println("Please confirm the bucket name and Try again!");
                    return false;
                }
            }

            s3Client.deleteObject(new DeleteObjectRequest(bucketName, url));
            return true;
        } catch (Exception e) {
            System.out.println("failed to delete");
        }


        return false;
    }

    public boolean devGetBucketName(AmazonS3 seClient){

        try{

            List<Bucket> bucketNames = seClient.listBuckets();
            for(Bucket b : bucketNames){
                String bucketName = b.getName().toLowerCase();
                if(bucketName.matches("(csye6225-fall2018-)+[a-zA-Z0-9]+(.me.csye6225.com)")){
                    this.bucketName = b.getName();
                    return true;
                }
            }

        }catch(Exception e){
            System.out.println("Unable to find bucket! Please try again!");
            e.printStackTrace();
        }


        return false;

    }
    
    public ResponseEntity<?> devAuthUser(String auth){

        String []creds = devGetcreds(auth);

        if(creds.length == 0){
        	return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON).body("{\"Response\":\"You are not logged in! Please login and then try!\"}");
        	
        }

        String regex = "[.0-9A-Za-z]+@[.0-9A-Za-z]+\\.[A-Za-z]{2,}";
        
        if(!creds[0].matches(regex)) {
        	
        	return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body("{\"Response\":\"Email format is invalid! Please try again!\"}");
        	
        }
        

        try {
            if(devAuthUser(creds)){
            	
                Date date = new Date();
                
                return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body("{\"Response\": \"" + date.toString()+ "\"}");
                
            }
        }catch(NoSuchElementException e){
        	return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON).body("{\"Response\":\"No such account in the system! Please register!\"}");
        	
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON).body("{\"Response\":\"No such account in the system! Please register!\"}");
        
    }

    public ResponseEntity<?> devCreateUser(String auth){
        String []creds = devGetcreds(auth);

        if(creds.length == 0){
        	return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON).body("{\"Response\":\"Username or Password cannot be blank! Please Try again!\"}");
        	
        }
        
        String regex = "[.0-9A-Za-z]+@[.0-9A-Za-z]+\\.[A-Za-z]{2,}";
        if(!creds[0].matches(regex)) {
        	return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body("{\"Response\":\"Username has to be valid email-ID! Please try again!\"}");
        	
        }


        if(devAuthUserEmail(creds)){
        	return ResponseEntity.status(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON).body("{\"Response\":\"This account already exists in the system!\"}");
        	
        }else{
            try {

                String email = creds[0];
                String password = creds[1];

                String hash = devHash(password);

                User user = new User(email, hash);

                userRepository.save(user);

                return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body("{\"Response\":\"User Account has been successfully created!\"}");
                
            }
            catch(Exception e){
                System.out.print(e.getMessage());
                devCreateUser(auth);
                return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body("{\"Response\":\"User Account has been successfully created!\"}");
                
            }
        }

    }

    protected boolean devAuthUser(String []cred){

        String email = cred[0];
        String password = cred[1];

        Optional<User> u = userRepository.findById(email);

        try{
            User user = u.get();
            return devCheckHash(password,user.getPassword());

        }catch (Exception e){
            return false;
        }

    }


    protected boolean devAuthUserEmail(String []cred){

        String email = cred[0];

        Optional<User> U = userRepository.findById(email);

        try{
            User u = U.get();
            if(u!=null) {
            	return true;
            }
            return false;

        }catch (Exception e){
            return false;
        }

    }
    
    protected String devHash(String password){
        if(password.isEmpty() || password == null){
            return null;
        }
        Random rand = new Random();
        int log_rounds = rand.nextInt(12);

        return BCrypt.hashpw(password,BCrypt.gensalt(log_rounds));
    }

    protected boolean devCheckHash(String password,String hash){
        if(password.isEmpty() || password == null){
            return false;
        }

        return BCrypt.checkpw(password,hash);
    }

    protected String[] devGetcreds(String auth){
        String []data = auth.split(" ");

        byte[] cred = Base64.decodeBase64(data[1]);

        return new String(cred).split(":");
    }


    public List<Transactions> devGetTransactions(String auth){

        String [] creds = devGetcreds(auth);
        String email = creds[0];
        String password = creds[1];

        Optional<User> u = userRepository.findById(email);
        try{

            User user = u.get();
            if(devCheckHash(password,user.getPassword())){

                return user.getTransactions();

            }

        }catch(Exception e){
            return null;
        }
        return null;
    }


    public Transactions devUpdateTransaction(String auth, String id, Transactions t1){
        String[] creds = devGetcreds(auth);
        if(devAuthUser(creds)){
            Optional<User> u = userRepository.findById(creds[0]);
                try{

                    Transactions t = transactionsRepository.findUserTransactionById(id,u.get());

                    t.setDescription(t1.getDescription());
                    t.setMerchant(t1.getMerchant());
                    t.setAmount(t1.getAmount());
                    t.setDate(t1.getDate());
                    t.setCategory(t1.getCategory());

                    transactionsRepository.save(t);
                    User user = userRepository.getOne(creds[0]);
                    user.updateTransaction(id,t1);
                    userRepository.save(user);
                    return t;

                }catch (Exception e){
                    System.out.print(e.getMessage());
                    return null;
                }
        }

        return null;
    }


    
}