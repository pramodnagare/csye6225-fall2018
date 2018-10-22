package demo;

import org.apache.log4j.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import demo.controllers.UserController;

@SpringBootApplication
public class DemoApplication {
	
	private static final Logger log = Logger.getLogger(DemoApplication.class);

	public static void main(String[] args) {
		
		log.info("Application Started");
		System.out.println("main");
		SpringApplication.run(DemoApplication.class, args);
	}
}
