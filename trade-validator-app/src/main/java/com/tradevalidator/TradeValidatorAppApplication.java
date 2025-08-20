package com.tradevalidator;

import ch.qos.logback.classic.ViewStatusMessagesServlet;
import com.tradevalidator.validator.CurrencyHolidayService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;
import org.jminix.console.servlet.MiniConsoleServlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@SpringBootApplication
@ComponentScan("com.tradevalidator")
@Configuration
@EnableSwagger2
public class TradeValidatorAppApplication implements ServletContextInitializer {

	public static void main(String[] args) {
		SpringApplication.run(TradeValidatorAppApplication.class, args);
	}

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		servletContext.addServlet("ViewStatusMessages", ViewStatusMessagesServlet.class).addMapping("/logs");
        servletContext.addServlet("JmxMiniConsoleServlet", MiniConsoleServlet.class).addMapping("/jmx/*");
	}

	@Bean
	public CurrencyHolidayService currencyHolidayService() {

		// service which return set of holidays for currencies, good point to extend and add querying of remote holidays rules repository
		return currency -> {
			SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");
            Currency USD = Currency.getInstance("USD");

            if (USD.equals(currency)) {
				try {
					return Optional.of(new HashSet<>(
                            Arrays.asList(
                                    DATE_FORMATTER.parse("2017-01-01"),
                                    DATE_FORMATTER.parse("2017-01-02"),
                                    DATE_FORMATTER.parse("2017-02-20")
                            )));
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}


            return Optional.empty();
        };
	}

	@Bean
	public Jackson2ObjectMapperBuilder jacksonBuilder() {
		return new Jackson2ObjectMapperBuilder()
				.dateFormat(new SimpleDateFormat("yyyy-MM-dd"))
				.failOnEmptyBeans(false)
				;

	}

	@Bean
	public Docket apiDocs() {
		return new Docket(DocumentationType.SWAGGER_2)
				.select()
				.apis(RequestHandlerSelectors.any())
				.paths(PathSelectors.any())
				.build();
	}
}
