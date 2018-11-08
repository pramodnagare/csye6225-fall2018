package com.example.rest_api.Service;

import com.example.rest_api.Repository.UserRepository;
import com.example.rest_api.Entities.User;
import org.apache.tomcat.util.codec.binary.Base64;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Service

public class UserService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    ResponseService responseService;

    @Autowired
    ValidationService validateService;

    public ResponseEntity authUser(String auth){

        String []userCredentials = getUserCredentials(auth);

        if(userCredentials.length == 0){
            return responseService.generateResponse(HttpStatus.UNAUTHORIZED
                    ,"{\"Response\":\"You are not logged in! Please login and then try!\"}");
        }

        if(!validateService.validateUsername(userCredentials[0])){
            return responseService.generateResponse(HttpStatus.BAD_GATEWAY
                    ,"(\"Response\":\"Username format is invalid! Please try again!\"}");
        }

        try {
            if(authUser(userCredentials)){
                DateFormat df = new SimpleDateFormat("HH:mm");
                Date date = new Date();
                return responseService
                        .generateResponse(HttpStatus.OK
                                ,"{\"Date\":\""+df.format(date)+"\"}");
            }
        }catch(NoSuchElementException e){
            return responseService.generateResponse(HttpStatus.UNAUTHORIZED
                            ,"{\"Response\":\"No such account in the system! Please register!\"}");

        }

        return responseService.generateResponse(HttpStatus.UNAUTHORIZED
                ,"{\"Response\":\"No such account in the system! Please register!\"}");
    }

    public ResponseEntity createUser(String auth){
        String []userCredentials = getUserCredentials(auth);

        if(userCredentials.length == 0){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("{\"Response\":\"Username or Password cannot be blank! Please Try again!\"}");
        }

        if(!validateService.validateUsername(userCredentials[0])){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("{\"Response\":\"Username has to be valid email-ID! Please try again!\"}");
        }



        if(authUser(userCredentials)){
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("{\"Response\":\"This account already exists in the system!\"}");
        }else{
            try {

                String username = userCredentials[0];
                String password = userCredentials[1];

                String hashedPassword = hash(password);

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

    protected boolean authUser(String []userCredentials){

        String username = userCredentials[0];
        String password = userCredentials[1];

        Optional<User> optionalUser = userRepository.findById(username);

        try{
            User user = optionalUser.get();
            return checkHash(password,user.password);

        }catch (Exception e){
            return false;
        }

    }


    protected String hash(String password){
        if(password.isEmpty() || password == null){
            return null;
        }
        Random rand = new Random();
        int log_rounds = rand.nextInt(12);

        return BCrypt.hashpw(password,BCrypt.gensalt(log_rounds));
    }

    protected boolean checkHash(String password,String hash){
        if(password.isEmpty() || password == null){
            return false;
        }

        return BCrypt.checkpw(password,hash);
    }

    protected String[] getUserCredentials(String auth){
        String []authParts = auth.split(" ");

        byte[] decode = Base64.decodeBase64(authParts[1]);

        return new String(decode).split(":");
    }


}
