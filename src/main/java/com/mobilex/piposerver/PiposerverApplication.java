package com.mobilex.piposerver;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Objects;

@EnableScheduling
@EnableBatchProcessing
@SpringBootApplication
public class PiposerverApplication {
	static {
		System.loadLibrary("native-lib");
	}
	public static native String ImportationTest();

	public static void main(String[] args) {
		assert Objects.equals(ImportationTest(), "SUCCESS");
		SpringApplication.run(PiposerverApplication.class, args);
	}
}
