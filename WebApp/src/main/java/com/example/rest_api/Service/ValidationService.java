package com.example.rest_api.Service;

import org.springframework.stereotype.Service;

@Service
public class ValidationService {

    public boolean validateUsername(String username){
        String regex = "[A-Za-z1-9]+@[A-Za-z0-9]+\\.[A-Za-z]{2,}";
        return username.matches(regex);
    }

}
