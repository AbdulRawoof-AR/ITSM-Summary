package com.example.itsm_summary;

import com.example.itsm_summary.model.Ticket;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@SpringBootApplication
public class ItsmSummaryApplication {

	public static void main(String[] args) {
		SpringApplication.run(ItsmSummaryApplication.class, args);
	}

	@Bean
	CommandLineRunner testMongoConnection(MongoTemplate mongoTemplate) {

		return args -> {

			mongoTemplate.executeCommand("{ ping: 1 }");

			System.out.println("MongoDB Connected Successfully!");

			System.out.println("Database Name: "
					+ mongoTemplate.getDb().getName());

			System.out.println("Total Tickets: "
					+ mongoTemplate.count(new Query(), Ticket.class));

			System.out.println("NEW Tickets: "
					+ mongoTemplate.count(
							Query.query(Criteria.where("status").is("NEW")),
							Ticket.class
					));
		};
	}
}
