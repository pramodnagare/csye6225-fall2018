package com.example.rest_api.Controller;

import com.example.rest_api.Service.ResponseService;
import com.example.rest_api.Service.UserService;
import com.example.rest_api.Service.ValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
public class AuthorizationController {

    @Autowired
    UserService userService;

    @Autowired
    ValidationService validateService;

    @Autowired
    ResponseService responseService;

    @RequestMapping(value="/time", method = RequestMethod.GET)
    public ResponseEntity authAndLogin(@RequestHeader(value="Authorization")String auth){

        if(auth.isEmpty() || auth.equals("NoValueFound")){
            return responseService.generateResponse(HttpStatus.UNAUTHORIZED,
                    "{\"Response\":\"You are not logged in! Please login and try again\"}");
        }

        return userService.authUser(auth);

    }

    @RequestMapping(value="/user/register",method=RequestMethod.POST)
    public ResponseEntity register(@RequestHeader(value="Authorization")String auth){

        if(auth.isEmpty() || auth.equals("NoValueFound")){
            return responseService.generateResponse(HttpStatus.UNAUTHORIZED,
                        "{\"Response\":\"Kindly provide credentials! Try again!\"}");
        }

        return userService.createUser(auth);

    }

}
