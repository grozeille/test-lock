package fr.grozeille.demo;

import fr.grozeille.demo.services.LockService;
import fr.grozeille.demo.services.impl.DBLockService;
import fr.grozeille.demo.services.impl.MyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
@Slf4j
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Bean
	public CommandLineRunner demo(final MyService myService, final DBLockService lockService) {
		return (args) -> {

			if(args.length > 0) {
				if(args[0].equalsIgnoreCase("init")) {
					myService.buildInitData();
					return;
				}
				else {
					System.err.println("Invalid argument " + args[0]+". Valid argument is 'init' or nothing.");
				}
			}

			List<Thread> threadList = new ArrayList<>();

			for(int cpt = 0; cpt < 4; cpt++) {
				final String lambdaId = String.valueOf(cpt+1);
				Thread t = new Thread(() -> {
					try {
						myService.callLambda(lambdaId);
					} catch (Exception e) {
						log.error("Unable to call lambda "+lambdaId, e);
					}
				});
				threadList.add(t);
			}

			log.info("Starting threads...");

			for(Thread t : threadList) {
				t.start();
			}

			log.info("Waiting threads...");

			for(Thread t : threadList) {
				t.join();
			}

			log.info("Done.");
		};
	}
}
