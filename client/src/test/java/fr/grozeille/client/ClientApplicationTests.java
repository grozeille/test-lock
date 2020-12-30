package fr.grozeille.client;

import lombok.extern.slf4j.Slf4j;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.reporters.Summariser;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

@SpringBootTest
@Slf4j
class ClientApplicationTests {

	private static StandardJMeterEngine jmeter;

	@BeforeAll
	public static void init() throws IOException {
		log.info("startup");
		// JMeter Engine
		jmeter = new StandardJMeterEngine();
		ClassLoader classLoader = ClientApplicationTests.class.getClassLoader();
		JMeterUtils.setJMeterHome(System.getenv("JMETER_HOME"));
		JMeterUtils.loadJMeterProperties(new File(classLoader.getResource("jmeter.properties").getFile()).getAbsolutePath());
		JMeterUtils.loadProperties(new File(classLoader.getResource("user.properties").getFile()).getAbsolutePath());
		JMeterUtils.initLocale();
		SaveService.loadProperties();
	}

	@Test
	void jmeterSmallLoad() throws Exception {

		ClassLoader classLoader = getClass().getClassLoader();

		// Load existing .jmx Test Plan
		HashTree testPlanTree = SaveService.loadTree(new File(classLoader.getResource("test.jmx").getFile()));

		Summariser summer = null;
		String summariserName = JMeterUtils.getPropDefault("summariser.name", "summary");
		if (summariserName.length() > 0) {
			summer = new Summariser(summariserName);
		}


		// Store execution results into a .jtl file
		String logFile = "target/jmeter-report";

		ResultCollector logger = new ResultCollector(summer);
		logger.setFilename(logFile);
		testPlanTree.add(testPlanTree.getArray()[0], logger);


		// Run JMeter Test
		jmeter.configure(testPlanTree);
		jmeter.run();
	}

}
