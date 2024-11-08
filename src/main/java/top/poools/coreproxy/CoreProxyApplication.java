package top.poools.coreproxy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.event.EventListener;

import top.poools.coreproxy.test.TcpClient;

import java.util.Random;

@Slf4j
@SpringBootApplication
@ConfigurationPropertiesScan
public class CoreProxyApplication {

	private final Random random = new Random();

	public static void main(String[] args) {
		SpringApplication.run(CoreProxyApplication.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onStart() throws InterruptedException {
		//startThreads(1, 1);
		//startThreads(1, 100);
		//startThreads(101, 500);
		//startThreads(501, 1000);
		//startThreads(1001, 3000);
		//startThreads(3001, 5000);
		//startThreads(5001, 10000);
	}

	private void startThreads(int from, int to) throws InterruptedException {
		Thread.sleep(2000);
		for (int i = from; i <= to; i++) {
			String name = "tcp-client-" + i;
			TcpClient tcpClient = new TcpClient("localhost", 3333, name);
			tcpClient.start();
			Thread.sleep(random.nextLong(5) + 1);
		}
	}
}
