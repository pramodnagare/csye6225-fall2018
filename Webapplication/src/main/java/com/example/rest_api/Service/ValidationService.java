package com.example.rest_api.Service;

import org.springframework.stereotype.Service;

@Service
public class ValidationService {

    public boolean validateUsername(String username){
        String regex = "[0-9A-Za-z]+@[0-9A-Za-z]+\\.[A-Za-z]{2,}";
        return username.matches(regex);
    }

}
