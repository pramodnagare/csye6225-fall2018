package com.example.rest_api.Controller;

import com.example.rest_api.Entities.Attachments;
import com.example.rest_api.Service.S3AttachmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@Profile("Dev")
public class S3AttachmentController {

    @Autowired
    S3AttachmentService s3AttachmentService;
    @GetMapping("/transaction/{transaction_id}/attachments")
    public ResponseEntity getAllAttachments(@RequestHeader(value="Authorization")String Auth,
                                            @PathVariable(value = "transaction_id")String transactionId){

        if(!Auth.equals("NoAuth") && !transactionId.isEmpty()){
            List<Attachments> attachmentsList = s3AttachmentService.getAllAttachments(Auth,transactionId);

            if(attachmentsList != null)
                return ResponseEntity.status(HttpStatus.OK)
                        .body(attachmentsList);

        }

        return new ResponseEntity("Oops! You are not authorized to perform this operation!",HttpStatus.UNAUTHORIZED);
    }

    @PostMapping("/transaction/{transaction_id}/attachments")
    public ResponseEntity addAttachment(@RequestHeader(value="Authorization")String auth,
                                        @PathVariable(value="transaction_id")String transactionId,
                                        @RequestParam("file")MultipartFile attachments){

        if(!auth.equals("NoAuth") && !transactionId.isEmpty()){
            return s3AttachmentService.addAttachment(auth,transactionId,attachments);
        }

        return new ResponseEntity("Oops! You are not authorized to perform this operation!",HttpStatus.UNAUTHORIZED);
    }

    @PutMapping("/transaction/{transaction_id}/attachments/{attachment_id}")
    public ResponseEntity updateTransaction(@RequestHeader(value="Authorization",defaultValue = "NoAuth")String auth,
                                            @PathVariable(value="transaction_id")String transactionId,
                                            @PathVariable(value="attachment_id")String attachmentId,
                                            @RequestParam("file") MultipartFile attachment){

        if(!auth.equals("NoAuth") && !transactionId.isEmpty() && attachment != null && !attachmentId.isEmpty()){
            return s3AttachmentService.updateAttachment(auth,transactionId,attachment,attachmentId);
        }

        return new ResponseEntity("Oops! You are not authorized to perform this operation!",HttpStatus.UNAUTHORIZED);
    }

    @DeleteMapping("/transaction/{transaction_id}/attachments/{attachment_id}")
    public ResponseEntity deleteAttachment(@RequestHeader(value="Authorization")String auth,
                                           @PathVariable(value="transaction_id")String transactionId,
                                           @PathVariable(value="attachment_id")String attachmentId){

        if(!auth.equals("NoAuth") && !transactionId.isEmpty() && !attachmentId.isEmpty()){
            if(s3AttachmentService.deleteAttachment(auth,transactionId,attachmentId)){
                return new ResponseEntity("The requested attachment has been deleted successfully!",HttpStatus.OK);
            }
        }

        return new ResponseEntity("Oops! You are not authorized to perform this operation!",HttpStatus.UNAUTHORIZED);
    }

}
