package demo.controllers;

import java.sql.Date;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Optional;

import org.apache.tomcat.util.codec.binary.Base64;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import ch.qos.logback.core.pattern.color.BoldYellowCompositeConverter;
import demo.models.User;
import demo.repositories.UserRepository;


@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {
	@Autowired
	UserRepository userRepository;


	@GetMapping("/time")
	public ResponseEntity<String> getTime() throws JSONException {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
		LocalDateTime now = LocalDateTime.now();
		JSONObject bodyObject = new JSONObject("{}");
		HttpHeaders headers = new HttpHeaders();
		try {
			bodyObject.put("currentTime", dtf.format(now));
		} catch (JSONException e1) {
			e1.printStackTrace();
		}
		return new ResponseEntity<String>(bodyObject.toString(), headers, HttpStatus.ACCEPTED);
	}

	@PostMapping("/user/register")
	public ResponseEntity createUser(@RequestHeader(value = "Authorization", defaultValue = "No Auth") String auth,
			@RequestBody User userJson) {

		byte[] bytes = Base64.decodeBase64(auth.split(" ")[1]);
		String uNamePwd[] = new String(bytes).split(":");
		System.out.println(uNamePwd);
		Optional<Integer> optionalUserAuth = userRepository.findIdByUserName(uNamePwd[0]);
		if (optionalUserAuth.isPresent()) {
			User u = userRepository.findById(optionalUserAuth.get()).get();
			if (u.getPassword().equals(uNamePwd[1])) {
				Optional<Integer> optionalUser = userRepository.findIdByUserName(userJson.getEmail());
				if (!optionalUser.isPresent()) {
					userRepository.save(userJson);
					return new ResponseEntity(userJson, HttpStatus.OK);
				}
				else {
					return new ResponseEntity("User with the given email already exists!", HttpStatus.CONFLICT);
				}
			}
			else {
				return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);
			}
		}
		else {
			return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);
		}
	}
}
