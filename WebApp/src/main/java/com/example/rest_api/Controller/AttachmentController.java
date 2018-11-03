package com.example.rest_api.Controller;

import com.example.rest_api.Entities.Attachments;
import com.example.rest_api.Service.AttachmentService;
import com.example.rest_api.Service.ResponseService;
import com.example.rest_api.Service.S3AttachmentService;
import com.example.rest_api.Service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Profile("Local")
public class AttachmentController {

    //For local attachments
    @Autowired
    AttachmentService attachmentService;
    /*--------------------------------------- Services for local attachments -----------------------------*/
    @GetMapping("/transact/{transaction_id}/attachments")
    public ResponseEntity getAllAttachments(@RequestHeader(value="Authorization", defaultValue = "NoAuth")String Auth,
                                            @PathVariable(value = "transaction_id")String transactionId){

        if(!Auth.equals("NoAuth") && !transactionId.isEmpty()){
            List<Attachments> attachmentsList = attachmentService.getAllAttachments(Auth,transactionId);

                if(attachmentsList != null)
                    return ResponseEntity.status(HttpStatus.OK)
                            .body(attachmentsList);

        }

        return new ResponseEntity(HttpStatus.UNAUTHORIZED);
    }

    @PostMapping("/transact/{transaction_id}/attachments")
    public ResponseEntity addAttachment(@RequestHeader(value="Authorization",defaultValue = "NoAuth")String auth,
                                        @PathVariable(value="transaction_id")String transactionId,
                                        @RequestBody Attachments attachments){

        if(!auth.equals("NoAuth") && !transactionId.isEmpty()){
           return attachmentService.addAttachment(auth,transactionId,attachments);
        }

        return new ResponseEntity(HttpStatus.UNAUTHORIZED);
    }

    @PutMapping("/transact/{transaction_id}/attachments/{attachment_id}")
    public ResponseEntity updateTransaction(@RequestHeader(value="Authorization",defaultValue = "NoAuth")String auth,
                                            @PathVariable(value="transaction_id")String transactionId,
                                            @PathVariable(value="attachment_id")String attachmentId,
                                            @RequestBody Attachments attachment){

        if(!auth.equals("NoAuth") && !transactionId.isEmpty() && attachment != null && !attachmentId.isEmpty()){
            return attachmentService.updateAttachment(auth,transactionId,attachment,attachmentId);
        }

        return new ResponseEntity(HttpStatus.UNAUTHORIZED);
    }

    @DeleteMapping("/transact/{transaction_id}/attachments/{attachment_id}")
    public ResponseEntity deleteAttachment(@RequestHeader(value="Authorization",defaultValue = "NoAuth")String auth,
                                           @PathVariable(value="transaction_id")String transactionId,
                                           @PathVariable(value="attachment_id")String attachmentId){

        if(!auth.equals("NoAuth") && !transactionId.isEmpty() && !attachmentId.isEmpty()){
            if(attachmentService.deleteAttachment(auth,transactionId,attachmentId)){
                return new ResponseEntity(HttpStatus.NO_CONTENT);
            }
        }

        return new ResponseEntity(HttpStatus.UNAUTHORIZED);
    }


}
