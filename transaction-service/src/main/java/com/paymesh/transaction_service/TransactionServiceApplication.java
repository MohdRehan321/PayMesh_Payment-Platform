package com.paymesh.transaction_service;

import java.io.File;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TransactionServiceApplication {

	public static void main(String[] args) {
		useWritableTempDir();
		SpringApplication.run(TransactionServiceApplication.class, args);
	}

	// On Windows, java.io.tmpdir falls back to C:\WINDOWS\ when TMP/TEMP/USERPROFILE are
// missing from the process environment. That dir is not writable, so Tomcat fails with
// "Unable to create tempDir. java.io.tmpdir is set to C:\WINDOWS\". Force a writable dir.
	private static void useWritableTempDir() {
		if (new File(System.getProperty("java.io.tmpdir", "")).canWrite()) {
			return;
		}
		File base = new File(System.getProperty("user.home", System.getProperty("user.dir", ".")));
		File fallback = new File(base, "paymesh-tmp");
		fallback.mkdirs();
		System.setProperty("java.io.tmpdir", fallback.getAbsolutePath());
	}

}