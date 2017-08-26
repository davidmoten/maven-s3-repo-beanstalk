package com.github.davidmoten.s3repo;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.util.IOUtils;
import com.google.common.base.Preconditions;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/repo/*")
public class RepoServlet extends HttpServlet {

	private static final String PROPERTY_MAVEN_REPO_BUCKET = "mavenRepoBucket";
	private String bucketName;

	@Override
	public void init(ServletConfig config) throws ServletException {
		Properties props = new Properties();
		try {
			props.load(AuthenticationFilter.class.getResourceAsStream("/configuration.properties"));
			this.bucketName = System.getProperty(PROPERTY_MAVEN_REPO_BUCKET);
			Preconditions.checkNotNull(bucketName, PROPERTY_MAVEN_REPO_BUCKET + " system property cannot be null");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		AmazonS3Client s3 = (AmazonS3Client) AmazonS3ClientBuilder.defaultClient();
		String path = getPath(req);
		System.out.println("getting maven-repo:" + path);
		if (path.length() > 0 && s3.doesObjectExist(bucketName, path)) {
			System.out.println("object exists");
			S3Object object = s3.getObject(bucketName, path);
			System.out.println("objectKey=" + object.getKey() + ", metadata=" + object.getObjectMetadata());
			object.getObjectContent();
			if (path.endsWith(".jar") || path.endsWith("zip") || path.endsWith("war")) {
				resp.setContentType("application/zip");
			} else if (path.endsWith("/pom.xml")) {
				resp.setContentType("application/xml");
			} else {
				resp.setContentType("application/octet-stream");
			}
			final String filename = lastPathElement(path);
			resp.addHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
			IOUtils.copy(object.getObjectContent(), resp.getOutputStream());
		} else {
			final String prefix;
			if (path.length() > 0) {
				prefix = path + "/";
			} else {
				prefix = path;
			}
			System.out.println("outputing directory");
			ListObjectsV2Request r = new ListObjectsV2Request().withBucketName(bucketName).withPrefix(prefix)
					.withDelimiter("/");
			ListObjectsV2Result objects = s3.listObjectsV2(r);
			if (objects.getCommonPrefixes().isEmpty() && objects.getObjectSummaries().isEmpty()) {
				resp.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
			resp.setContentType("text/html");
			PrintStream out = new PrintStream(resp.getOutputStream());
			out.println("<html>");
			out.println(
					"<head><style>p {font-family: Courier, Monospace; -webkit-margin-after:0px; -webkit-margin-before:0px;}</style><title>maven-repo</title></head>");
			out.println("<h2>Index of /" + path + "</h2>");
			out.println("<hr/>");
			String parent;
			int i = req.getRequestURI().lastIndexOf('/');
			if (i != -1) {
				parent = req.getRequestURI().substring(0, i);
			} else {
				parent = req.getRequestURI();
			}

			out.println("<p><a href=\"" + parent + "\">../</a></p>");
			for (String entry : objects.getCommonPrefixes()) {
				String p;
				if (path.length() > 0) {
					p = lastPathElement(path) + "/" + lastPathElement(stripFinalSlash(entry));
				} else {
					p = req.getRequestURI() + "/" + lastPathElement(stripFinalSlash(entry));
				}

				out.println("<p><a href=\"" + p + "\">" + lastPathElement(stripFinalSlash(entry)) + "</a></p>");
			}
			for (S3ObjectSummary s : objects.getObjectSummaries()) {
				if (!s.getKey().endsWith("/")) {
					out.println("<p><a style=\"color:black\" href=\"" + lastPathElement(path) + "/"
							+ lastPathElement(s.getKey()) + "\">" + lastPathElement(s.getKey()) + "</a></p>");
				}
			}
			out.println("</html>");
		}
	}

	private static String getPath(HttpServletRequest req) {
		String path = req.getPathInfo();
		if (path == null)
			path = "";
		else
			// skip leading forward-slash
			path = path.substring(1);
		if (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		return path;
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		System.out.println("doPut");
		saveFileToS3(req);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		System.out.println("doPost");
		saveFileToS3(req);
	}

	private void saveFileToS3(HttpServletRequest req) throws IOException {
		String path = getPath(req);
		AmazonS3Client s3 = (AmazonS3Client) AmazonS3ClientBuilder.defaultClient();
		ObjectMetadata m = new ObjectMetadata();
		m.setContentLength(req.getContentLength());
		s3.putObject(bucketName, path, req.getInputStream(), m);
	}

	private static String stripFinalSlash(String s) {
		if (s.endsWith("/")) {
			return s.substring(0, s.length() - 1);
		} else {
			return s;
		}
	}

	private String lastPathElement(String path) {
		final String filename;
		int i = path.lastIndexOf('/');
		if (i == -1)
			filename = path;
		else
			filename = path.substring(i + 1);
		return filename;
	}

}
