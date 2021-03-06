package demo;

import lombok.Data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.ExportMetricReader;
import org.springframework.boot.actuate.autoconfigure.ExportMetricWriter;
import org.springframework.boot.actuate.metrics.export.MetricExportProperties;
import org.springframework.boot.actuate.metrics.integration.SpringIntegrationMetricReader;
import org.springframework.boot.actuate.metrics.jmx.JmxMetricWriter;
import org.springframework.boot.actuate.metrics.repository.redis.RedisMetricRepository;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.sleuth.sampler.AlwaysSampler;
import org.springframework.cloud.stream.annotation.EnableModule;
import org.springframework.cloud.stream.annotation.Source;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.monitor.IntegrationMBeanExporter;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableModule(Source.class)
@IntegrationComponentScan
@RestController
@EnableDiscoveryClient
public class VoterApplication {

	private static Logger logger = LoggerFactory.getLogger(VoterApplication.class);

	@Autowired
	Voter voter;

	@Autowired
	MetricExportProperties export;

	@RequestMapping(value="/votes", method=RequestMethod.POST)
	public void accept(@RequestBody Vote vote) {
		logger.info("Sending: " + vote);
		this.voter.vote(vote);
	}

	@Bean
	@ExportMetricWriter
	public RedisMetricRepository redisMetricWriter(
			RedisConnectionFactory connectionFactory) {
		return new RedisMetricRepository(connectionFactory, this.export.getRedis().getPrefix(),
				this.export.getRedis().getKey());
	}

	@Bean
	@ExportMetricWriter
	public JmxMetricWriter jmxMetricWriter(
			@Qualifier("mbeanExporter") MBeanExporter exporter) {
		return new JmxMetricWriter(exporter);
	}

	@Bean
	@ExportMetricReader
	public SpringIntegrationMetricReader springIntegrationMetricReader(
			IntegrationMBeanExporter exporter) {
		return new SpringIntegrationMetricReader(exporter);
	}

	@Bean
	public AlwaysSampler alwaysSampler() {
		return new AlwaysSampler();
	}

	public static void main(String[] args) throws InterruptedException {
		SpringApplication.run(VoterApplication.class, args);
	}

}

@Data
class Vote {
	private long election;
	private long candidate;
	private int score;
}

@MessagingGateway(name = "voter")
interface Voter {
	@Gateway(requestChannel=Source.OUTPUT)
	void vote(Vote vote);
}