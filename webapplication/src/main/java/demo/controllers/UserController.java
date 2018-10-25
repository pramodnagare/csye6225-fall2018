package demo.controllers;

//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.tomcat.jni.FileInfo;
import org.apache.tomcat.util.codec.binary.Base64;
import org.hibernate.annotations.Filter;
import org.json.JSONException;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoProperties.Storage;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
//import com.mysql.fabric.Response;

import demo.models.Attachments;
import demo.models.User;
import demo.models.UserTransaction;
import demo.repositories.AttachmentsRepository;
import demo.repositories.UserRepository;
import demo.repositories.UserTransactionRepository;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

@Profile("application")
@RestController

public class UserController {
	@Autowired
	UserRepository userRepository;

	@Autowired
	UserTransactionRepository userTransactionRepository;

	@Autowired
	AttachmentsRepository attachmentTransactionRepository;

	// -----------------------------------Fetching data for time
	// ----------------------------------------------------//

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

	// ----------------- upload attchment --------------------------//

	// private final String file_upload_location = "C:\\upload";

	private final String file_upload_location = "/home/harsh1291993/Documents/upload";

	@PostMapping(value = "/transaction/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<FileInfo> uploadFile(@RequestParam("file") MultipartFile file,
			@RequestHeader(value = "Authorization", defaultValue = "No Auth") String auth, @PathVariable String id) {

		byte[] bytes = Base64.decodeBase64(auth.split(" ")[1]);
		String uNamePwd[] = new String(bytes).split(":");
		System.out.println("1");
		try {

			String username1 = uNamePwd[0];
			String pass1 = uNamePwd[1];
			if (username1.isEmpty() && pass1.isEmpty() && username1.length() == 0 && pass1.length() == 0) {
				return new ResponseEntity("Unauthorized ", HttpStatus.UNAUTHORIZED);
			} else {

				System.out.println("2");
				Optional<Integer> optionalUserAuth = userRepository.findIdByUserName(uNamePwd[0]);
				int user_id;
				if (optionalUserAuth.isPresent() || optionalUserAuth.get() != 0
						|| !optionalUserAuth.equals(Optional.empty())) {
					System.out.println("3");
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
							System.out.println("4");
							if (!file.isEmpty()) {
								try {
									System.out.println("5");
									UserTransaction ut = userTransactionRepository.findById(utopt.get()).get();
									System.out.println("6");
									String filename = file.getOriginalFilename();
									String fileLocation = file.getName();
									File f = new File(file_upload_location + File.separator + filename);
									String pa = f.toPath().toString();

									file.transferTo(f);
									Attachments fileinfo = new Attachments();
									String uuid = UUID.randomUUID().toString();
									System.out.println("4");
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
									fileinfo.setUrl(pa);
									fileinfo.setUserTransaction(ut);

									attachmentTransactionRepository.save(fileinfo);

									// save in attachment table as well

									HttpHeaders headers = new HttpHeaders();
									headers.add("File has been uploaded successfully", filename);

									// return new ResponseEntity<FileInfo>(headers, HttpStatus.OK);
									return new ResponseEntity("Authorized", HttpStatus.OK);
								} catch (Exception ex) {
									return new ResponseEntity<FileInfo>(HttpStatus.BAD_REQUEST);

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

	// --------------- upload attachement ends here ---------------//

	// fetching the data

	@GetMapping("transaction/{id}/attachments")
	public ResponseEntity getAllTransaction(
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

	// ends here

	@DeleteMapping("/transaction/{user_transaction_id}/attachments/{attachment_id}")
	public ResponseEntity deleteAttachments(@PathVariable String user_transaction_id,
			@PathVariable String attachment_id,
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

	@PutMapping(value = "/transaction/{id}/attachments/{attachment_id}")
	public ResponseEntity updateFiles(@RequestHeader(value = "Authorization", defaultValue = "No Auth") String auth,
			@PathVariable String id, @PathVariable String attachment_id, @RequestBody Attachments attachments) {

		byte[] bytes = Base64.decodeBase64(auth.split(" ")[1]);
		String uNamePwd[] = new String(bytes).split(":");

		try {

			String username1 = uNamePwd[0];
			String pass1 = uNamePwd[1];
			if (username1.isEmpty() && pass1.isEmpty() && username1.length() == 0 && pass1.length() == 0) {
				return new ResponseEntity("Unauthorized ", HttpStatus.UNAUTHORIZED);
			} else {
				System.out.println("1");
				Optional<Integer> optionalUserAuth = userRepository.findIdByUserName(uNamePwd[0]);
				int user_id;
				if (optionalUserAuth.isPresent() || optionalUserAuth.get() != 0
						|| !optionalUserAuth.equals(Optional.empty())) {
					System.out.println("2");
					User u = userRepository.findById(optionalUserAuth.get()).get();

					String abc = uNamePwd[0].toString();
					String def = uNamePwd[1].toString();

					String encode = BCrypt.hashpw(def, BCrypt.gensalt(12));

					if (u.getEmail().equals(abc) && BCrypt.checkpw(def, u.getPassword()) == true) {
						user_id = optionalUserAuth.get();
						System.out.println("user_id" + user_id);
						Optional<String> utopt = userTransactionRepository.findUid(id, user_id);
						String ost_id = utopt.toString();
						System.out.println("ost_id" + ost_id);
						if (utopt.isPresent() || !utopt.equals(0) || !utopt.equals(Optional.empty())) {
							System.out.println("4");
							Optional<String> attach_id = attachmentTransactionRepository.findIdByAttachIds(id,
									attachment_id);

							System.out.println("attach id is :" + attach_id);

							if (attach_id.isPresent()) {

								Attachments att = attachmentTransactionRepository.findIdByAttachIdswa(attachment_id);

								List<String> stratt = attachmentTransactionRepository.findIdu(attachment_id);
								String urltobedeleteed = stratt.get(0).toString();

								File file = new File(urltobedeleteed);
								if (file.exists()) {
									file.delete();

									UserTransaction ut = userTransactionRepository.findById(utopt.get()).get();

									String email = u.getEmail();
									String pass = u.getPassword();
									String mer = ut.getMerchant();
									String amt = ut.getAmount();
									String desc = ut.getDescription();
									String cat = ut.getCategory();
									String date = ut.getDate();
									userTransactionRepository.save(ut);

									String urls = attachments.getUrl(); // body ?????
									System.out.println("urls" + urls);
									String urltosplit = urls.substring(urls.lastIndexOf("\\") + 1); // studentid.png
									System.out.println("urltosplit" + urltosplit);
									File fi = new File(file_upload_location + File.separator);

									String pa = fi.toPath().toString();
									System.out.println("pa" + pa);
									FileUtils.copyFileToDirectory(new File(urls), fi);

									String pat = pa + urltosplit;
									System.out.println("pat" + pat);
									att.setAttachment_id(attachment_id);
									att.setUrl(pat);
									att.setUserTransaction(ut);

									attachmentTransactionRepository.deleteById(attachment_id);

									attachmentTransactionRepository.save(att);

									return new ResponseEntity("Authorized ", HttpStatus.OK);

								} else {
									System.out.println("adasds");
								}

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

			}
		} catch (

		Exception ex) {
			ex.printStackTrace();
		}
		return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);
	}
}
