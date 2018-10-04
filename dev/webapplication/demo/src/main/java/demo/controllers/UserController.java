package demo.controllers;

import java.sql.Date;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Optional;

import org.apache.tomcat.util.codec.binary.Base64;
import org.json.JSONException;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import ch.qos.logback.core.pattern.color.BoldYellowCompositeConverter;
import demo.models.User;
import demo.models.UserTranscation;
import demo.repositories.UserRepository;
import demo.repositories.UserTransactionRepository;


@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {
	@Autowired
	UserRepository userRepository;

	@Autowired
	UserTransactionRepository userTransactionRepository;
	
	public boolean authUser(String auth){
		
		byte[] bytes = Base64.decodeBase64(auth.split(" ")[1]);
		
		String uNamePwd[] = new String(bytes).split(":");
		if(!uNamePwd[0].isEmpty() && !uNamePwd[1].isEmpty()) {
			Optional<Integer> optionalUserAuth = userRepository.findIdByUserName(uNamePwd[0]);
			if (optionalUserAuth.isPresent()) {
				User u = userRepository.findById(optionalUserAuth.get()).get();
				String hashed = u.getPassword();
				String password = uNamePwd[1].trim();
				
				if (password.equals(hashed))
					return true;
				else 
					return false;
			}
			else 
				return false;
		}
		else 
			return false;
	}

	public Optional<Integer> authUser_id(String auth){
	
	byte[] bytes = Base64.decodeBase64(auth.split(" ")[1]);
	System.out.println(auth);
	String uNamePwd[] = new String(bytes).split(":");
	if(!uNamePwd[0].isEmpty() && !uNamePwd[1].isEmpty()) {
		Optional<Integer> optionalUserAuth = userRepository.findIdByUserName(uNamePwd[0]);
		if (optionalUserAuth.isPresent()) {
			User u = userRepository.findById(optionalUserAuth.get()).get();
			if (u.getPassword().equals(uNamePwd[1])) {
				return optionalUserAuth;
			}	
			else
				return null;
		}
		else 
			return null;
	}
	else 
		return null;
	
}

	@GetMapping("/time")
	public ResponseEntity<String> getTime(@RequestHeader(value = "Authorization", defaultValue = "No Auth") String auth)
			throws JSONException {
	
		boolean authuser = authUser(auth);
		
		JSONObject bodyObject = new JSONObject("{}");
		
		if(authuser) {
			
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
			LocalDateTime now = LocalDateTime.now();
			HttpHeaders headers = new HttpHeaders();
			
			Optional<Integer> temp = authUser_id(auth);
			int uid = Integer.valueOf(temp.get());
			
			bodyObject.put("currentTime", dtf.format(now));
			
			return new ResponseEntity<String>(bodyObject.toString(), headers, HttpStatus.ACCEPTED);
			
		}
		
		else {
			
			bodyObject.put("Authorized:", "Not Authorized!");
			
			return new ResponseEntity<String>(bodyObject.toString(), HttpStatus.NOT_ACCEPTABLE);
			
		}
	}
		
	@PostMapping("/transaction")
	public ResponseEntity createTransactoin(@RequestHeader(value = "Authorization", defaultValue = "No Auth") String auth, @RequestBody UserTranscation usertransaction) {
		
		boolean authuser = authUser(auth);
		
		if(authuser) {
			Optional<Integer> temp = authUser_id(auth);
			int uid = Integer.valueOf(temp.get());
			usertransaction.setUid(uid);
			
			userTransactionRepository.save(usertransaction);
			return new ResponseEntity(usertransaction, HttpStatus.ACCEPTED);
		}
		else {
			return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);
		}
		
		
	}
	
	@GetMapping("/transaction")
	public ResponseEntity getAllTransactoin(@RequestHeader(value = "Authorization", defaultValue = "No Auth") String auth) {
		
		boolean authuser = authUser(auth);
		
		if(authuser) {
			Optional<Integer> temp = authUser_id(auth);
			int uid = Integer.valueOf(temp.get());
			
			List<UserTranscation> allTransaction = userTransactionRepository.findAll();
			
			List<UserTranscation> myTranscation = new ArrayList<>();
			
			for (UserTranscation t : allTransaction) {
				if (t.getUid()==uid) {
					myTranscation.add(t);
				}
			}
			
			return new ResponseEntity(myTranscation, HttpStatus.ACCEPTED);
			//return new ResponseEntity(null, HttpStatus.ACCEPTED);
		}
		else {
			return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);
		}
		
		
	}
	
	
	@PutMapping("/transaction/{id}")
	public ResponseEntity updatetransaction(@RequestHeader(value = "Authorization", defaultValue = "No Auth") String auth, @PathVariable("id") String id,@RequestBody UserTranscation ut ) {
		
		boolean authuser = authUser(auth);
		
		if(authuser) {
			Optional<Integer> temp = authUser_id(auth);
			int uid = Integer.valueOf(temp.get());
			
			if(id.equalsIgnoreCase(ut.getid())) {
				
				List<String> tid = userTransactionRepository.findAllIDByUserId(uid);
				
				for(String i : tid) {
					if(i.equalsIgnoreCase(id)) {
						ut.setUid(uid);
						String d = ut.getDescription();
						String a = ut.getAmount();
						String c = ut.getCategory();
						String dt = ut.getDate();
						String m = ut.getMerchant();
						
						userTransactionRepository.updateTransaction(id, uid, d, a, c, dt, m);
						
						return new ResponseEntity(ut, HttpStatus.ACCEPTED);
					}
					return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);
				}
				return new ResponseEntity("Transaction ID not found", HttpStatus.ACCEPTED);
				
			}
			else
				return new ResponseEntity("Update Failed!", HttpStatus.ACCEPTED);
		}
		else 
			return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);
	}
	
	
	@DeleteMapping("/transaction/{id}")
	public ResponseEntity deletetransaction(@RequestHeader(value = "Authorization", defaultValue = "No Auth") String auth, @PathVariable("id") String id) {
		
		boolean authuser = authUser(auth);
		
		if(authuser) {
			Optional<Integer> temp = authUser_id(auth);
			int uid = Integer.valueOf(temp.get());

			List<String> transId = userTransactionRepository.findAllIDByUserId(uid);
			
			if (!transId.isEmpty()) {
				for (String i : transId) {
					if(i.equalsIgnoreCase(id)) {
						userTransactionRepository.deleteTransaction(id, uid);
						return new ResponseEntity("Deleted", HttpStatus.ACCEPTED);
					}
				}
				return new ResponseEntity("Permission Denied", HttpStatus.UNAUTHORIZED);
			}
			else
				return new ResponseEntity("Permission Denied", HttpStatus.UNAUTHORIZED);
				
				
		}
		else 
			return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);
		
		
	}
	
	@PostMapping("/user/register")
	public ResponseEntity createUser(@RequestHeader(value = "Authorization", defaultValue = "No Auth") String auth,
			@RequestBody User userJson) {
		
		boolean authuser = authUser(auth);
		
		if (authuser) {
			userRepository.save(userJson);
			return new ResponseEntity(userJson, HttpStatus.OK);
		}
			
		else 
			return new ResponseEntity("Not Authorized!", HttpStatus.CONFLICT);
	}
		
}
