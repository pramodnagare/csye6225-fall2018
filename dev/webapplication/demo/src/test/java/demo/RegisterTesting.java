package demo;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.InvocationTargetException;

import org.junit.Test;
import org.springframework.http.ResponseEntity;

import demo.controllers.UserController;
import demo.models.User;

public class RegisterTesting {
	User user = new User();
	@Test
	public void test1() throws ArrayIndexOutOfBoundsException, InvocationTargetException {
		UserController u = new UserController();
		User user1 = (User)u.createUser("", user).getBody();
		assertEquals(user1.getEmail(), "abc1");
	}
}
