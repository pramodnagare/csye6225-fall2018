package demo.controllers;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.jni.FileInfo;
import org.apache.tomcat.util.codec.binary.Base64;
import org.json.JSONException;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.HttpMethod;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import demo.models.Attachments;
import demo.models.User;
import demo.models.UserTransaction;
import demo.repositories.AttachmentsRepository;
import demo.repositories.UserRepository;
import demo.repositories.UserTransactionRepository;

@Profile("dev")
@RestController
public class S3Controllers {

	@Autowired
	UserRepository userRepository;

	@Autowired
	UserTransactionRepository userTransactionRepository;

	@Autowired
	AttachmentsRepository attachmentTransactionRepository;

	// b4v transaction
	
	@GetMapping("/time")
	public ResponseEntity<String> getTime(@RequestHeader(value = "Authorization", defaultValue = "No Auth") String auth)
			throws JSONException {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
		LocalDateTime now = LocalDateTime.now();
		JSONObject bodyObject = new JSONObject("{}");
		HttpHeaders headers = new HttpHeaders();
		byte[] bytes = Base64.decodeBase64(auth.split(" ")[1]);
		String uNamePwd[] = new String(bytes).split(":");
		if (uNamePwd.length == 0) {
			return new ResponseEntity<String>(bodyObject.toString(), headers, HttpStatus.NOT_ACCEPTABLE);
		}
		Optional<Integer> optionalUserAuth = userRepository.findIdByUserName(uNamePwd[0]);

		if (optionalUserAuth.isPresent()) {

			try {
				bodyObject.put("currentTime", dtf.format(now));
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
			return new ResponseEntity<String>(bodyObject.toString(), headers, HttpStatus.ACCEPTED);
		}
		return new ResponseEntity<String>(bodyObject.toString(), headers, HttpStatus.NOT_ACCEPTABLE);

	}

	// ------------------------------ Fetching data for time ends here //
	// ---------------------------------------------------//

	// -------------------------------------create user and
	// register--------------------------------------------------//

	@PostMapping("/user/register")
	public ResponseEntity createUser(@RequestHeader(value = "Authorization", defaultValue = "No Auth") String auth,
			@RequestBody User userJson) throws ArrayIndexOutOfBoundsException, InvocationTargetException {

		byte[] bytes = Base64.decodeBase64(auth.split(" ")[1]);
		String uNamePwd[] = new String(bytes).split(":");

		try {

			String username1 = uNamePwd[0];
			System.out.println("username1" + username1);
			String pass1 = uNamePwd[1];
			System.out.println("pass1" + pass1);
			if (username1.isEmpty() && pass1.isEmpty() && username1.length() == 0 && pass1.length() == 0) {
				return new ResponseEntity("Bhai name and pass daal ", HttpStatus.UNAUTHORIZED);
			} else {
				Optional<Integer> optionalUserAuth = userRepository.findIdByUserName(uNamePwd[0]);
				System.out.println("opt auth " + optionalUserAuth.get().toString());
				if (optionalUserAuth.isPresent() || optionalUserAuth.get() != 0
						|| !optionalUserAuth.equals(Optional.empty())) {

					User u = userRepository.findById(optionalUserAuth.get()).get();

					String abc = uNamePwd[0].toString();
					String def = uNamePwd[1].toString();

					String encode = BCrypt.hashpw(def, BCrypt.gensalt(12));
					System.out.println("encode is" + encode);

					if (u.getEmail().equals(abc) && BCrypt.checkpw(def, u.getPassword()) == true) {
						Optional<Integer> optionalUser = userRepository.findIdByUserName(userJson.getEmail());
						if (!optionalUser.isPresent()) {
							userJson.setPassword(BCrypt.hashpw(userJson.getPassword(), BCrypt.gensalt(12))); // salting
																												// password
							userRepository.save(userJson);
							return new ResponseEntity(userJson, HttpStatus.OK);
						} else {
							return new ResponseEntity("User with the given email already exists!", HttpStatus.CONFLICT);
						}
					} else {
						return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);
					}
				} else {
					return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);
				}
				// }

			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);

	}

	// ------------------------------------------- create and user ends here //
	// --------------------------------------//

	// --------------------------------------create transaction //
	// --------------------------------------------------//

	@PostMapping("/transaction")
	public ResponseEntity createUserTransaction(@RequestBody UserTransaction ut,
			@RequestHeader(value = "Authorization", defaultValue = "No Auth") String auth)
			throws ArrayIndexOutOfBoundsException, InvocationTargetException {

		byte[] bytes = Base64.decodeBase64(auth.split(" ")[1]);
		String uNamePwd[] = new String(bytes).split(":");

		try {
			String username1 = uNamePwd[0];
			String pass1 = uNamePwd[1];
			if (username1.isEmpty() && pass1.isEmpty() && username1.length() == 0 && pass1.length() == 0) {
				return new ResponseEntity("Not Authorized ", HttpStatus.UNAUTHORIZED);
			} else {
				Optional<Integer> optionalUserAuth = userRepository.findIdByUserName(uNamePwd[0]); // user_id is there
				// System.out.println(" oauth is : " + optionalUserAuth);

				if (optionalUserAuth.isPresent() || optionalUserAuth.get() != 0
						|| !optionalUserAuth.equals(Optional.empty())) {

					// user obj wirt to db
					User u = userRepository.findById(optionalUserAuth.get()).get();
					// ------
					// chk for authentication
					String abc = uNamePwd[0].toString();
					String def = uNamePwd[1].toString();

					String encode = BCrypt.hashpw(def, BCrypt.gensalt(12));
					System.out.println("encode is" + encode);

					if (u.getEmail().equals(abc) && BCrypt.checkpw(def, u.getPassword()) == true) {
						System.out.println(BCrypt.checkpw(def, u.getPassword()) + "    lllllllllllllllllll");

						System.out.println(" near uuid ");
						String uuid = UUID.randomUUID().toString();
						String username = u.getEmail();
						String password = u.getPassword();

						u.setEmail(username);
						u.setPassword(password);
						ut.setId(uuid);
						ut.setUser(u);
						userTransactionRepository.save(ut);
						return new ResponseEntity("Authorized", HttpStatus.OK);
					} else {
						return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);
					}

				}

				else {
					return new ResponseEntity("CHK CREDENTIALS", HttpStatus.UNAUTHORIZED);
				}
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);
	}

	// ------------------------------------------- create transaction ends here //
	// ------------------------------------------//

	// -------------------------------------------- update transaction //
	// ---------------------------------------------------//
	@PutMapping("/transaction/{id}")
	public ResponseEntity put(@PathVariable String id,
			@RequestHeader(value = "Authorization", defaultValue = "No Auth") String auth,
			@RequestBody UserTransaction ut) throws ArrayIndexOutOfBoundsException, InvocationTargetException {

		byte[] bytes = Base64.decodeBase64(auth.split(" ")[1]);
		String uNamePwd[] = new String(bytes).split(":");

		try {

			String username1 = uNamePwd[0];
			String pass1 = uNamePwd[1];

			if (username1.isEmpty() && pass1.isEmpty() && username1.length() == 0 && pass1.length() == 0) {
				return new ResponseEntity("Bhai name and pass daal ", HttpStatus.UNAUTHORIZED);
			} else {

				Optional<Integer> optionalUserAuth = userRepository.findIdByUserName(uNamePwd[0]);
				if (optionalUserAuth.isPresent() || optionalUserAuth.get() != 0
						|| !optionalUserAuth.equals(Optional.empty())) {

					User u = userRepository.findById(optionalUserAuth.get()).get();
					// ------
					// chk for authentication
					String abc = uNamePwd[0].toString();
					String def = uNamePwd[1].toString();

					String encode = BCrypt.hashpw(def, BCrypt.gensalt(12));
					System.out.println("encode is" + encode);
					int user_id;
					if (u.getEmail().equals(abc) && BCrypt.checkpw(def, u.getPassword()) == true) {

						user_id = optionalUserAuth.get(); // 178
						Optional<String> opt = userTransactionRepository.findUid(id, user_id);

						if (opt.isPresent()) {
							String d = ut.getDescription();
							String a = ut.getAmount();
							String c = ut.getCategory();
							String dt = ut.getDate();
							String m = ut.getMerchant();

							userTransactionRepository.updateTransaction(id, user_id, d, a, c, dt, m);
							return new ResponseEntity("Updated", HttpStatus.ACCEPTED);
						} else {
							return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);
						}

					} else {
						return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);
					}

				} else {
					return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);
				}

			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);
	}

	// -------------------------------------------------- update transaction ends
	// here ---------------------------------------//

	// ---------------------------------------------------get transaction
	// -------------------------------------------------//

	@GetMapping("/transaction")
	public ResponseEntity getAll(@RequestHeader(value = "Authorization", defaultValue = "No Auth") String auth,
			HttpServletResponse response)
			throws JSONException, JsonProcessingException, ArrayIndexOutOfBoundsException, InvocationTargetException {
		byte[] bytes = Base64.decodeBase64(auth.split(" ")[1]);
		String uNamePwd[] = new String(bytes).split(":");

		try {

			String username1 = uNamePwd[0];
			String pass1 = uNamePwd[1];
			if (username1.isEmpty() && pass1.isEmpty() && username1.length() == 0 && pass1.length() == 0) {
				return new ResponseEntity("Unauthorized ", HttpStatus.UNAUTHORIZED);
			} else {

				Optional<Integer> optionalUserAuth = userRepository.findIdByUserName(uNamePwd[0]);
				int user_id;
				if (optionalUserAuth.isPresent() || optionalUserAuth.get() != 0
						|| !optionalUserAuth.equals(Optional.empty())) {
					User u = userRepository.findById(optionalUserAuth.get()).get();
					// ------
					// chk for authentication
					String abc = uNamePwd[0].toString();
					String def = uNamePwd[1].toString();

					String encode = BCrypt.hashpw(def, BCrypt.gensalt(12));
					System.out.println("encode is" + encode);

					if (u.getEmail().equals(abc) && BCrypt.checkpw(def, u.getPassword()) == true) {
						user_id = optionalUserAuth.get(); // 178

						ObjectMapper mapper = new ObjectMapper();
						List<UserTransaction> li = userTransactionRepository.findIdByUserId(user_id);
						List<String> myTranscation = new ArrayList<>();
						JSONObject bodyObject = new JSONObject("{}");
						for (UserTransaction t : li) {
							bodyObject.put("description", t.getDescription());
							bodyObject.put("merchant", t.getMerchant());
							bodyObject.put("amount", t.getAmount());
							bodyObject.put("date", t.getDate());
							bodyObject.put("category", t.getCategory());
							myTranscation.add(bodyObject.toString());

						}

						return new ResponseEntity(myTranscation, HttpStatus.ACCEPTED);
					} else
						return new ResponseEntity("Not authorized", HttpStatus.ACCEPTED);
				} else
					return new ResponseEntity("Not authorized", HttpStatus.ACCEPTED);
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);
	}

	// ---------------------------------------- get transaction ends here
	// ---------------------------------------------//

	// --------------------------------------- delete transaction
	// ---------------------------------------------------//
	@DeleteMapping("/transaction/{id}")
	public ResponseEntity delete(@PathVariable String id,
			@RequestHeader(value = "Authorization", defaultValue = "No Auth") String auth)
			throws JSONException, JsonProcessingException, ArrayIndexOutOfBoundsException, InvocationTargetException {

		byte[] bytes = Base64.decodeBase64(auth.split(" ")[1]);
		String uNamePwd[] = new String(bytes).split(":");

		try {

			String username1 = uNamePwd[0];
			String pass1 = uNamePwd[1];
			if (username1.isEmpty() && pass1.isEmpty() && username1.length() == 0 && pass1.length() == 0) {
				return new ResponseEntity("Unauthorized ", HttpStatus.UNAUTHORIZED);
			} else {
				Optional<Integer> optionalUserAuth = userRepository.findIdByUserName(uNamePwd[0]);
				int user_id;
				if (optionalUserAuth.isPresent() || optionalUserAuth.get() != 0
						|| !optionalUserAuth.equals(Optional.empty())) {
					User u = userRepository.findById(optionalUserAuth.get()).get();
					// ------
					// chk for authentication
					String abc = uNamePwd[0].toString();
					String def = uNamePwd[1].toString();

					String encode = BCrypt.hashpw(def, BCrypt.gensalt(12));
					System.out.println("encode is" + encode);

					if (u.getEmail().equals(abc) && BCrypt.checkpw(def, u.getPassword()) == true) {

						user_id = optionalUserAuth.get(); // 178

						Optional<String> ost = userTransactionRepository.findUid(id, user_id);
						if (ost.isPresent()) {
							userTransactionRepository.deleteTransaction(id, user_id);
							return new ResponseEntity("Deleted", HttpStatus.ACCEPTED);
						} else {
							return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);
						}

					} else
						return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);
				} else
					return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);

			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);
	}
	// -------------------------------------------- delete transaction ends here
	// -------------------------------------------//

	
	// b4 transaction
	
	
	String clientRegion = "us-east-1";
	String bucketName = "bucketassignmet4";
	// String fileObjKeyName = "firstattempt";
	// String fileName = "C:\\upload";

	@PostMapping(value = "/transaction/{id}/attachments")
	public ResponseEntity afghjj(@RequestParam("file") MultipartFile file,
			@RequestHeader(value = "Authorization", defaultValue = "No Auth") String auth, @PathVariable String id) {

		byte[] bytes = Base64.decodeBase64(auth.split(" ")[1]);
		String uNamePwd[] = new String(bytes).split(":");

		try {

			System.out.println("1");
			String username1 = uNamePwd[0];
			String pass1 = uNamePwd[1];
			if (username1.isEmpty() && pass1.isEmpty() && username1.length() == 0 && pass1.length() == 0) {
				return new ResponseEntity("Unauthorized ", HttpStatus.UNAUTHORIZED);
			} else {
				Optional<Integer> optionalUserAuth = userRepository.findIdByUserName(uNamePwd[0]);
				int user_id;
				if (optionalUserAuth.isPresent() || optionalUserAuth.get() != 0
						|| !optionalUserAuth.equals(Optional.empty())) {

					User u = userRepository.findById(optionalUserAuth.get()).get();
					// ------
					// chk for authentication
					String abc = uNamePwd[0].toString();
					String def = uNamePwd[1].toString();

					String encode = BCrypt.hashpw(def, BCrypt.gensalt(12));

					if (u.getEmail().equals(abc) && BCrypt.checkpw(def, u.getPassword()) == true) {
						user_id = optionalUserAuth.get();
						Optional<String> utopt = userTransactionRepository.findUid(id, user_id);
						if (utopt.isPresent() || !utopt.equals(0) || !utopt.equals(Optional.empty())) {

							if (!file.isEmpty()) {
								try {
									AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withRegion(clientRegion)
											.withCredentials(new DefaultAWSCredentialsProviderChain()).build();

									String filename = file.getOriginalFilename();
									String fileLocation = file.getName();
									File f = new File(filename);
									String fileObjKeyName = f.toString();
									file.transferTo(f);
									PutObjectRequest request = new PutObjectRequest(bucketName, fileObjKeyName, f);
									ObjectMetadata metadata = new ObjectMetadata();
									metadata.setContentType("plain/text");
									metadata.addUserMetadata("x-amz-meta-title", "someTitle");
									request.setMetadata(metadata);
									//
									s3Client.putObject(request);
									// GeneratePresignedUrlRequest generatePresignedUrlRequest = new
									// GeneratePresignedUrlRequest(
									// bucketName, fileObjKeyName).withMethod(HttpMethod.GET);
									// URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
									// String urlStr = url.toString();

									String ur = "https://s3.amazonaws.com/";
									String urlStr = ur + bucketName + "/" + filename;
									System.out.println("urlstr" + urlStr);

									UserTransaction ut = userTransactionRepository.findById(utopt.get()).get();

									Attachments fileinfo = new Attachments();
									String uuid = UUID.randomUUID().toString();

									int uu = u.getId();
									String email = u.getEmail();
									String pass = u.getPassword();
									String mer = ut.getMerchant();
									String amt = ut.getAmount();
									String desc = ut.getDescription();
									String cat = ut.getCategory();
									String date = ut.getDate();

									// u.setEmail(email);
									// u.setPassword(pass);
									ut.setUser(u);
									userTransactionRepository.save(ut);
									fileinfo.setAttachment_id(uuid);
									fileinfo.setUrl(urlStr);
									fileinfo.setUserTransaction(ut);

									attachmentTransactionRepository.save(fileinfo);

									return new ResponseEntity("Authorized", HttpStatus.OK);
								} catch (AmazonServiceException e) {
									// The call was transmitted successfully, but Amazon S3 couldn't process
									// it, so it returned an error response.
									e.printStackTrace();
								}

							} else {
								return new ResponseEntity<FileInfo>(HttpStatus.UNAUTHORIZED);
							}
						} else {
							return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);
						}

					} else {
						return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);
					}
				} else {
					return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);
				}

			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);

	}

	@GetMapping("transaction/{id}/attachments")
	public ResponseEntity getAllS3Transaction(
			@RequestHeader(value = "Authorization", defaultValue = "No Auth") String auth, HttpServletResponse response,
			@PathVariable String id) {

		byte[] bytes = Base64.decodeBase64(auth.split(" ")[1]);
		String uNamePwd[] = new String(bytes).split(":");
		try {

			String username1 = uNamePwd[0];
			String pass1 = uNamePwd[1];
			if (username1.isEmpty() && pass1.isEmpty() && username1.length() == 0 && pass1.length() == 0) {
				return new ResponseEntity("Unauthorized ", HttpStatus.UNAUTHORIZED);
			} else {

				Optional<Integer> optionalUserAuth = userRepository.findIdByUserName(uNamePwd[0]);
				int user_id;
				if (optionalUserAuth.isPresent() || optionalUserAuth.get() != 0
						|| !optionalUserAuth.equals(Optional.empty())) {
					User u = userRepository.findById(optionalUserAuth.get()).get();
					String abc = uNamePwd[0].toString();
					String def = uNamePwd[1].toString();
					String encode = BCrypt.hashpw(def, BCrypt.gensalt(12));

					if (u.getEmail().equals(abc) && BCrypt.checkpw(def, u.getPassword()) == true) {
						user_id = optionalUserAuth.get(); // 9
						if (user_id != 0) {

							Optional<String> opt = userTransactionRepository.findIdByUserIdwa(id, user_id); // uuid
							String opt_id = opt.get().toString();
							System.out.println("opt_id" + opt_id); // 234567890-= uuid

							// new changes//
							ObjectMapper mapper = new ObjectMapper();
							List<Attachments> li = attachmentTransactionRepository.findIdByu(opt_id);
							List<String> myTranscation = new ArrayList<>();
							JSONObject bodyObject = new JSONObject("{}");
							for (Attachments t : li) {
								bodyObject.put("url", t.getUrl());
								myTranscation.add(bodyObject.toString());

							}

							return new ResponseEntity(myTranscation, HttpStatus.ACCEPTED);

						}
					}
				}
			}

		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}

		return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);

	}

	@DeleteMapping("/transaction/{user_transaction_id}/attachments/{attachment_id}")
	public ResponseEntity deleteAttachments(@PathVariable String user_transaction_id,
			@PathVariable String attachment_id, HttpServletResponse response,
			@RequestHeader(value = "Authorization", defaultValue = "No Auth") String auth)
			throws JSONException, JsonProcessingException, ArrayIndexOutOfBoundsException, InvocationTargetException {

		byte[] bytes = Base64.decodeBase64(auth.split(" ")[1]);
		String uNamePwd[] = new String(bytes).split(":");

		try {

			String username1 = uNamePwd[0];
			String pass1 = uNamePwd[1];
			if (username1.isEmpty() && pass1.isEmpty() && username1.length() == 0 && pass1.length() == 0) {
				return new ResponseEntity("Unauthorized ", HttpStatus.UNAUTHORIZED);
			} else {
				Optional<Integer> optionalUserAuth = userRepository.findIdByUserName(uNamePwd[0]);
				int user_id;
				if (optionalUserAuth.isPresent() || optionalUserAuth.get() != 0
						|| !optionalUserAuth.equals(Optional.empty())) {
					User u = userRepository.findById(optionalUserAuth.get()).get();
					// ------
					// chk for authentication
					String abc = uNamePwd[0].toString();
					String def = uNamePwd[1].toString();

					String encode = BCrypt.hashpw(def, BCrypt.gensalt(12));
					System.out.println("encode is" + encode);

					if (u.getEmail().equals(abc) && BCrypt.checkpw(def, u.getPassword()) == true) {

						user_id = optionalUserAuth.get(); // 9
						Optional<String> ost = userTransactionRepository.findUid(user_transaction_id, user_id); // dfghjkl
						String ost_id = ost.get().toString(); // 38

						if (ost.isPresent()) {

							Optional<String> attach_id = attachmentTransactionRepository.findIdByAttachIds(ost_id,
									attachment_id);

							if (attach_id.isPresent()) {
								AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withRegion(clientRegion)
										.withCredentials(new DefaultAWSCredentialsProviderChain()).build();

								List<String> attach_list = attachmentTransactionRepository.findIdu(attachment_id);

								String reciept = attach_list.get(0).toString();
								System.out.println("reciept" + reciept);
								String str[] = reciept.split(bucketName);
								System.out.println(" 0 " + str[0] + " 1 " + str[1]);
								String img = str[1];
								String substr_img = img.substring(1);
								System.out.println(substr_img + " : substr_img");
								s3Client.deleteObject(new DeleteObjectRequest(bucketName, substr_img));

								attachmentTransactionRepository.deleteTransaction(attachment_id);
								return new ResponseEntity("Deleted", HttpStatus.ACCEPTED);
							} else {
								return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);
							}

							// userTransactionRepository.deleteTransaction(user_transaction_id, user_id);

						} else {
							return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);
						}

					} else
						return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);
				} else
					return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);

			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);
	}

	// update

	@PostMapping(value = "/transaction/{id}/attachments/{attachment_id}")
	public ResponseEntity updateatt(@RequestParam("file") MultipartFile file,
			@RequestHeader(value = "Authorization", defaultValue = "No Auth") String auth, @PathVariable String id,
			@PathVariable String attachment_id) {

		byte[] bytes = Base64.decodeBase64(auth.split(" ")[1]);
		String uNamePwd[] = new String(bytes).split(":");

		try {

			System.out.println("1");
			String username1 = uNamePwd[0];
			String pass1 = uNamePwd[1];
			if (username1.isEmpty() && pass1.isEmpty() && username1.length() == 0 && pass1.length() == 0) {
				return new ResponseEntity("Unauthorized ", HttpStatus.UNAUTHORIZED);
			} else {
				Optional<Integer> optionalUserAuth = userRepository.findIdByUserName(uNamePwd[0]);
				int user_id;
				if (optionalUserAuth.isPresent() || optionalUserAuth.get() != 0
						|| !optionalUserAuth.equals(Optional.empty())) {

					User u = userRepository.findById(optionalUserAuth.get()).get();
					// ------
					// chk for authentication
					String abc = uNamePwd[0].toString();
					String def = uNamePwd[1].toString();

					String encode = BCrypt.hashpw(def, BCrypt.gensalt(12));

					if (u.getEmail().equals(abc) && BCrypt.checkpw(def, u.getPassword()) == true) {
						user_id = optionalUserAuth.get();
						Optional<String> utopt = userTransactionRepository.findUid(id, user_id);
						if (utopt.isPresent() || !utopt.equals(0) || !utopt.equals(Optional.empty())) {

							if (!file.isEmpty()) {
								try {
									AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withRegion(clientRegion)
											.withCredentials(new DefaultAWSCredentialsProviderChain()).build();

									String filename = file.getOriginalFilename();
									String fileLocation = file.getName();
									File f = new File(filename);
									String fileObjKeyName = f.toString();
									file.transferTo(f);
									PutObjectRequest request = new PutObjectRequest(bucketName, fileObjKeyName, f);
									ObjectMetadata metadata = new ObjectMetadata();
									metadata.setContentType("plain/text");
									metadata.addUserMetadata("x-amz-meta-title", "someTitle");
									request.setMetadata(metadata);
									//
									s3Client.putObject(request);
									// GeneratePresignedUrlRequest generatePresignedUrlRequest = new
									// GeneratePresignedUrlRequest(
									// bucketName, fileObjKeyName).withMethod(HttpMethod.GET);
									// URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
									// String urlStr = url.toString();

									String ur = "https://s3.amazonaws.com/";
									String urlStr = ur + bucketName + "/" + filename;
									System.out.println("urlstr" + urlStr);

									UserTransaction ut = userTransactionRepository.findById(utopt.get()).get();

									Attachments fileinfo = new Attachments();
									String uuid = UUID.randomUUID().toString();

									int uu = u.getId();
									String email = u.getEmail();
									String pass = u.getPassword();
									String mer = ut.getMerchant();
									String amt = ut.getAmount();
									String desc = ut.getDescription();
									String cat = ut.getCategory();
									String date = ut.getDate();

									// u.setEmail(email);
									// u.setPassword(pass);
									ut.setUser(u);
									userTransactionRepository.save(ut);
									fileinfo.setAttachment_id(uuid);
									fileinfo.setUrl(urlStr);
									fileinfo.setUserTransaction(ut);

									List<String> attach_list = attachmentTransactionRepository.findIdu(attachment_id);

									String reciept = attach_list.get(0).toString();

									String str[] = reciept.split(bucketName);

									String img = str[1];
									String substr_img = img.substring(1);
									System.out.println(substr_img + " : substr_img");
									s3Client.deleteObject(new DeleteObjectRequest(bucketName, substr_img));

									attachmentTransactionRepository.deleteTransaction(attachment_id);

									attachmentTransactionRepository.save(fileinfo);

									return new ResponseEntity("Authorized", HttpStatus.OK);
								} catch (AmazonServiceException e) {
									// The call was transmitted successfully, but Amazon S3 couldn't process
									// it, so it returned an error response.
									e.printStackTrace();
								}

							} else {
								return new ResponseEntity<FileInfo>(HttpStatus.UNAUTHORIZED);
							}
						} else {
							return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);
						}

					} else {
						return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);
					}
				} else {
					return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);
				}

			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);

	}

	// update ends here
}
