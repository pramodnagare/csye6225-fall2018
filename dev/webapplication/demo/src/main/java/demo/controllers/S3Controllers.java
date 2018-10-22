package demo.controllers;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
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
import org.springframework.web.bind.annotation.RequestBody;
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
import net.bytebuddy.utility.RandomString;

@Profile("dev")
@RestController
public class S3Controllers {
	private static final Logger log = Logger.getLogger(S3Controllers.class);

	@Autowired
	UserRepository userRepository;

	@Autowired
	UserTransactionRepository userTransactionRepository;

	@Autowired
	AttachmentsRepository attachmentTransactionRepository;

	String clientRegion = "us-east-1";

	String bucketName = "amas3ha";

	@PostMapping(value = "/transaction/{id}/attachments")
	public ResponseEntity afghjj(@RequestParam("file") MultipartFile file,
			@RequestHeader(value = "Authorization", defaultValue = "No Auth") String auth, @PathVariable String id) {

		log.info("Entered post Mapping");
		byte[] bytes = Base64.decodeBase64(auth.split(" ")[1]);
		String uNamePwd[] = new String(bytes).split(":");

		try {

			System.out.println("1");
			String username1 = uNamePwd[0];
			String pass1 = uNamePwd[1];
			if (username1.isEmpty() && pass1.isEmpty() && username1.length() == 0 && pass1.length() == 0) {
				log.info("Authorization not provided");
				return new ResponseEntity("Unauthorized ", HttpStatus.UNAUTHORIZED);
			} else {
				log.info("Authorization provided");
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
						System.out.println("id :" + id + "user_id : " + user_id);
						Optional<String> utopt = userTransactionRepository.findUid(id, user_id);
						//System.out.println(" utop is : " + utopt.toString());
						if (utopt.isPresent() && !utopt.equals(0) && !utopt.equals(Optional.empty())) {
							log.info("User ID exist");
							//System.out.println("inside but shudnt be");
							if (!file.isEmpty()) {
								log.info("Image exist");
								try {

									//System.out.println("inside but shudnt be");
									AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withRegion(clientRegion)
											.withCredentials(new DefaultAWSCredentialsProviderChain()).build();

									String filename = file.getOriginalFilename();
									// String string = RandomStringUtils.random(64, false, true);
									String stri = RandomString.make();
									System.out.println(stri);
									// String fileLocation = file.getName();
									File f = new File(stri + "_" + filename);
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
									String urlStr = ur + bucketName + "/" + fileObjKeyName;
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
									log.info("Image posted");
									return new ResponseEntity("Authorized", HttpStatus.OK);
								} catch (AmazonServiceException e) {
									// The call was transmitted successfully, but Amazon S3 couldn't process
									// it, so it returned an error response.
									log.info(e);
									e.printStackTrace();
								}

							} else {
								log.info("Image does not exist");
								return new ResponseEntity<FileInfo>(HttpStatus.UNAUTHORIZED);
							}
						} else {
							log.info("User ID does not exist");
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
			log.error(ex);
			ex.printStackTrace();
		}
		return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);

	}

	@GetMapping("transaction/{id}/attachments")
	public ResponseEntity getAllS3Transaction(
			@RequestHeader(value = "Authorization", defaultValue = "No Auth") String auth, HttpServletResponse response,
			@PathVariable String id) {
		log.info("Entered get Image");

		byte[] bytes = Base64.decodeBase64(auth.split(" ")[1]);
		String uNamePwd[] = new String(bytes).split(":");
		try {

			String username1 = uNamePwd[0];
			String pass1 = uNamePwd[1];
			if (username1.isEmpty() && pass1.isEmpty() && username1.length() == 0 && pass1.length() == 0) {
				log.info("Authorization not provided");
				return new ResponseEntity("Unauthorized ", HttpStatus.UNAUTHORIZED);
			} else {

				log.info("Authorization provided");
				Optional<Integer> optionalUserAuth = userRepository.findIdByUserName(uNamePwd[0]);
				int user_id;
				if (optionalUserAuth.isPresent() && optionalUserAuth.get() != 0
						&& !optionalUserAuth.equals(Optional.empty())) {
					User u = userRepository.findById(optionalUserAuth.get()).get();
					String abc = uNamePwd[0].toString();
					String def = uNamePwd[1].toString();
					String encode = BCrypt.hashpw(def, BCrypt.gensalt(12));

					if (u.getEmail().equals(abc) && BCrypt.checkpw(def, u.getPassword()) == true) {
						user_id = optionalUserAuth.get(); // 9
						if (user_id != 0) {
							log.info("User ID exist");
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
							log.info("Fetching Image");
							return new ResponseEntity(myTranscation, HttpStatus.ACCEPTED);

						}
					}
				}
			}

		} catch (Exception ex) {
			log.error(ex);
			System.out.println(ex.getMessage());
		}

		return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);

	}

	@DeleteMapping("/transaction/{user_transaction_id}/attachments/{attachment_id}")
	public ResponseEntity deleteAttachments(@PathVariable String user_transaction_id,
			@PathVariable String attachment_id, HttpServletResponse response,
			@RequestHeader(value = "Authorization", defaultValue = "No Auth") String auth)
			throws JSONException, JsonProcessingException, ArrayIndexOutOfBoundsException, InvocationTargetException {
		log.info("Entered Delete Image");
		byte[] bytes = Base64.decodeBase64(auth.split(" ")[1]);
		String uNamePwd[] = new String(bytes).split(":");

		try {

			String username1 = uNamePwd[0];
			String pass1 = uNamePwd[1];
			if (username1.isEmpty() && pass1.isEmpty() && username1.length() == 0 && pass1.length() == 0) {
				log.info("Authorization not provided");
				return new ResponseEntity("Unauthorized ", HttpStatus.UNAUTHORIZED);
			} else {
				log.info("Authorization provided");
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

							log.info("User ID exist");
							Optional<String> attach_id = attachmentTransactionRepository.findIdByAttachIds(ost_id,
									attachment_id);

							if (attach_id.isPresent()) {
								log.info("Image exist");
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
								log.info("Image deleted");
								return new ResponseEntity("Deleted", HttpStatus.ACCEPTED);
							} else {
								log.info("Image does not exist");
								return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);
							}

							// userTransactionRepository.deleteTransaction(user_transaction_id, user_id);

						} else {
							log.info("User ID does not exist");
							return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);
						}

					} else
						return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);
				} else
					return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);

			}
		} catch (Exception ex) {
			log.error(ex);
			ex.printStackTrace();
		}
		return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);
	}

	// update

	@PostMapping(value = "/transaction/{id}/attachments/{attachment_id}")
	public ResponseEntity updateatt(@RequestParam("file") MultipartFile file,
			@RequestHeader(value = "Authorization", defaultValue = "No Auth") String auth, @PathVariable String id,
			@PathVariable String attachment_id) {
		log.info("Entered posting Image");

		byte[] bytes = Base64.decodeBase64(auth.split(" ")[1]);
		String uNamePwd[] = new String(bytes).split(":");

		try {

			System.out.println("1");
			String username1 = uNamePwd[0];
			String pass1 = uNamePwd[1];
			if (username1.isEmpty() && pass1.isEmpty() && username1.length() == 0 && pass1.length() == 0) {
				log.info("Authorization not provided");
				return new ResponseEntity("Unauthorized ", HttpStatus.UNAUTHORIZED);
			} else {
				log.info("Authorization provided");
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
						if (utopt.isPresent() && !utopt.equals(0) && !utopt.equals(Optional.empty())) {

							if (!file.isEmpty()) {
								log.info("Image file exist");
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
									log.info("Image posted");
									return new ResponseEntity("Authorized", HttpStatus.OK);
								} catch (AmazonServiceException e) {
									// The call was transmitted successfully, but Amazon S3 couldn't process
									// it, so it returned an error response.
									log.error(e);
									e.printStackTrace();
								}

							} else {		
								return new ResponseEntity<FileInfo>(HttpStatus.UNAUTHORIZED);
							}
						} else {
							log.info("Image file does not exist");
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
			log.error(ex);
			ex.printStackTrace();
		}
		return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);

	}

	// update ends here
	@PutMapping(value = "/transaction/{id}/attachments/{attachment_id}")
	public ResponseEntity updateFiles(@RequestHeader(value = "Authorization", defaultValue = "No Auth") String auth,
			@PathVariable String id, @PathVariable String attachment_id, @RequestBody Attachments attachments) {
		log.info("Entered update image");

		byte[] bytes = Base64.decodeBase64(auth.split(" ")[1]);
		String uNamePwd[] = new String(bytes).split(":");

		try {

			String username1 = uNamePwd[0];
			String pass1 = uNamePwd[1];
			if (username1.isEmpty() && pass1.isEmpty() && username1.length() == 0 && pass1.length() == 0) {
				log.info("Authentication not provided");
				return new ResponseEntity("Unauthorized ", HttpStatus.UNAUTHORIZED);
			} else {
				log.info("Authentication provided");
				Optional<Integer> optionalUserAuth = userRepository.findIdByUserName(uNamePwd[0]);
				int user_id;
				if (optionalUserAuth.isPresent() && optionalUserAuth.get() != 0
						&& !optionalUserAuth.equals(Optional.empty())) {
					User u = userRepository.findById(optionalUserAuth.get()).get();

					String abc = uNamePwd[0].toString();
					String def = uNamePwd[1].toString();

					String encode = BCrypt.hashpw(def, BCrypt.gensalt(12));

					if (u.getEmail().equals(abc) && BCrypt.checkpw(def, u.getPassword()) == true) {
						user_id = optionalUserAuth.get();
						Optional<String> utopt = userTransactionRepository.findUid(id, user_id);
						String ost_id = utopt.toString();

						if (utopt.isPresent() && !utopt.equals(0) && !utopt.equals(Optional.empty())) {
							Optional<String> attach_id = attachmentTransactionRepository.findIdByAttachIds(id,
									attachment_id);

							if (attach_id.isPresent()) {
								log.info("Image ID exist");
								Attachments att = attachmentTransactionRepository.findIdByAttachIdswa(attachment_id);
								List<String> stratt = attachmentTransactionRepository.findIdu(attachment_id);
								String urltobedeleteed = stratt.get(0).toString();
								// File file = new File(urltobedeleteed);

								if (!urltobedeleteed.isEmpty()) {
									AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withRegion(clientRegion)
											.withCredentials(new DefaultAWSCredentialsProviderChain()).build();
									log.info("Image url exist");

									List<String> attach_list = attachmentTransactionRepository.findIdu(attachment_id);
									String reciept = attach_list.get(0).toString();
									String str[] = reciept.split(bucketName);
									String img = str[1];
									String substr_img = img.substring(1);
									System.out.println("sub string is :" + substr_img);
									s3Client.deleteObject(new DeleteObjectRequest(bucketName, substr_img));

									UserTransaction ut = userTransactionRepository.findById(utopt.get()).get();
									// changes from here
									//

									String urls = attachments.getUrl();
									String stri = RandomString.make();
									String urltosplit = urls.substring(urls.lastIndexOf("/") + 1); // buatti.png
									// add random name afterwards
									File fbody = new File(urls);
									File f = new File(urltosplit);
									String fileObjKeyName = f.toString();
									String ur = "https://s3.amazonaws.com/";
									String urlStr = ur + bucketName + "/" + fileObjKeyName;
									File fi = new File(urlStr + File.separator);
									String pa = fi.toPath().toString();
									// PutObjectRequest request = new PutObjectRequest(bucketName, fileObjKeyName,
									// fi);

									// ObjectMetadata metadata = new ObjectMetadata();
									// metadata.setContentType("plain/text");
									// metadata.addUserMetadata("x-amz-meta-title", "someTitle");
									// request.setMetadata(metadata);
									// s3Client.putObject(bucketName, fileObjKeyName, fi);
									// attachmentTransactionRepository.deleteById(attachment_id);
									s3Client.putObject(bucketName, fileObjKeyName, fbody);

									int uu = u.getId();
									String use = u.getEmail();
									String pas = u.getPassword();

									// u.setId(uu);
									u.setEmail(use);
									u.setPassword(pas);

									// userRepository.save(u);

									String mer = ut.getMerchant();
									String amt = ut.getAmount();
									String desc = ut.getDescription();
									String cat = ut.getCategory();
									String date = ut.getDate();
									String u_t_id = ut.getId();

									ut.setAmount(amt);
									ut.setCategory(cat);
									ut.setDate(date);
									ut.setDescription(desc);
									ut.setMerchant(mer);
									ut.setUser(u);

									userTransactionRepository.save(ut);

									attachmentTransactionRepository.deleteTransaction(attachment_id);
									Attachments fileinfo = new Attachments();
									String uuid = UUID.randomUUID().toString();
									fileinfo.setAttachment_id(uuid);
									log.info("Saing image name");
									fileinfo.setUrl(attachments.getUrl());
									fileinfo.setUserTransaction(ut);
									attachmentTransactionRepository.save(fileinfo);
									log.info("Image updated");
									return new ResponseEntity("Authorized ", HttpStatus.OK);

								} else {
									log.info("Image url does not exist");
									return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);
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
		} catch (Exception ex) {
			log.error(ex);
			ex.printStackTrace();
		}
		return new ResponseEntity("Not authorized", HttpStatus.UNAUTHORIZED);
	}

	// ---------------

}
