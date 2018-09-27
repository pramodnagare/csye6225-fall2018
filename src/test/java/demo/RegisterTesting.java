package demo;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.http.ResponseEntity;

import demo.controllers.UserController;
import demo.models.User;

public class RegisterTesting {
	User user = new User("abc", "xyz");
	@Test
	public void test1() {
		UserController u = new UserController();
		User user1 = (User)u.createUser("", new User("abc1", "xyz1")).getBody();
		assertEquals(user1.getEmail(), "abc1");
	}
}
