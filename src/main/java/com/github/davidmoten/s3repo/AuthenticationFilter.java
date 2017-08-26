package com.github.davidmoten.s3repo;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.auth.AUTH;

import com.github.davidmoten.security.Hmac;

@WebFilter(urlPatterns = { "/repo/*" })
public final class AuthenticationFilter implements Filter {

	private Set<String> hmacs;
	private String salt;
	private String key;
	private String realm;

	@Override
	public void init(FilterConfig conf) throws ServletException {
		try {
			{
				Properties props = new Properties();
				props.load(AuthenticationFilter.class.getResourceAsStream("/authentication.properties"));
				salt = props.getProperty("salt");
				key = props.getProperty("key");
				String[] hmacs = props.getProperty("hmacs").split(",");
				this.hmacs = new HashSet<>(Arrays.asList(hmacs));
			}
			{
				Properties props = new Properties();
				props.load(AuthenticationFilter.class.getResourceAsStream("/configuration.properties"));
				this.realm = props.getProperty("realm");
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		// see main(String[]) method below to generate a new hmac to insert into
		// configuration.properties
		String challenge = "Basic realm=\"" + realm + "\"";
		try {
			HttpServletRequest req = (HttpServletRequest) request;
			System.out.println(req.getHeader(AUTH.WWW_AUTH_RESP));
			BasicAuthentication auth = BasicAuthentication.from(req);
			String hmac = Hmac.hmac(auth.username() + ":" + auth.password() + salt, key);
			System.out.println(hmac);
			if (hmacs.contains(hmac)) {
				chain.doFilter(request, response);
			} else {
				HttpServletResponse h = (HttpServletResponse) response;
				h.addHeader(AUTH.WWW_AUTH, challenge);
				h.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			}
		} catch (RuntimeException e) {
			System.out.println(
					"Authentication failed: '" + e.getMessage() + "', requesting authentication (AUTH.WWW_AUTH).");
			HttpServletResponse h = (HttpServletResponse) response;
			h.addHeader(AUTH.WWW_AUTH, challenge);
			h.sendError(HttpServletResponse.SC_UNAUTHORIZED);
		}
	}

	@Override
	public void destroy() {
		// do nothing
	}

	public static void main(String[] args) {
		System.out.println(UUID.randomUUID());
		String user = "fred";
		String salt = "some salt";
		String key = "some key";
		String hmac = Hmac.hmac(user + ":" + "password" + salt, key);
		System.out.println(hmac);
	}
}
