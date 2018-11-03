package com.example.rest_api.Service;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ResponseService {

    public ResponseEntity generateResponse(HttpStatus status, Object reason){

        if(reason == null){
            return new ResponseEntity(status);
        }
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(reason);
    }

}
