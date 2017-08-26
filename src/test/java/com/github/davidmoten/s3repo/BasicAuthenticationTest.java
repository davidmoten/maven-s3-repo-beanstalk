package com.github.davidmoten.s3repo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.github.davidmoten.security.Hmac;

public class BasicAuthenticationTest {

	@Test
	public void test() {
		String header = "Basic YW1zYTpib28";
		BasicAuthentication b = BasicAuthentication.from(header);
		Assert.assertEquals("amsa", b.username());
		Assert.assertEquals("boo", b.password());
	}

	@Test
	public void generateAuth() {
		String user = System.getProperty("user");
		String password = System.getProperty("password");
		if (user != null && password != null) {
			File file = new File("src/main/resources/authentication.properties");
			System.out.println("regenerating " + file);
			try {
				try (PrintStream out = new PrintStream(file)) {
					out.println("user=" + user);
					String salt = UUID.randomUUID().toString();
					String key = UUID.randomUUID().toString();
					out.println("salt=" + salt);
					out.println("key=" + key);
					String hmac = Hmac.hmac(user + ":" + password + salt, key);
					out.println("hmac=" + hmac);
				}
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
	}

}
