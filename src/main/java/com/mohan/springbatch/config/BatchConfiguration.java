package com.mohan.springbatch.config;

import com.mohan.springbatch.JobCompletionNotificationListener;
import com.mohan.springbatch.PersonItemProcessor;
import com.mohan.springbatch.model.Person;
import com.mohan.springbatch.repository.PersonRepository;
import lombok.AllArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@AllArgsConstructor
public class BatchConfiguration {

  private PersonRepository personRepository;
  private PersonWriter personWriter;
  private PlatformTransactionManager platformTransactionManager;

  @Bean
  public FlatFileItemReader<Person> reader() {
    return new FlatFileItemReaderBuilder<Person>()
        .name("personItemReader")
        .resource(new ClassPathResource("sample-data.csv"))
        .delimited()
        .names("id", "firstName", "lastName")
        .targetType(Person.class)
        .build();
  }

  @Bean
  public PersonItemProcessor processor() {
    return new PersonItemProcessor();
  }

  /*@Bean
    public JdbcBatchItemWriter<Person> writer(DataSource dataSource) {
      return new JdbcBatchItemWriterBuilder<Person>()
          .sql("INSERT INTO people (first_name, last_name) VALUES (:firstName, :lastName)")
          .dataSource(dataSource)
          .beanMapped()
          .build();
    }
  */

  @Bean
  public RepositoryItemWriter<Person> writer() {
    RepositoryItemWriter<Person> writer = new RepositoryItemWriter<>();
    writer.setRepository(personRepository);
    writer.setMethodName("save");

    return writer;
  }

  @Bean
  public Job importUserJob(
      JobRepository jobRepository, Step step1, JobCompletionNotificationListener listener) {
    return new JobBuilder("importUserJob", jobRepository).listener(listener).start(step1(jobRepository)).build();
  }

  @Bean
  public Step step1(JobRepository jobRepository) {
    return new StepBuilder("step1", jobRepository)
        .<Person, Person>chunk(3, platformTransactionManager)
        .reader(reader())
        .processor(processor())
        .writer(writer())
        .build();
  }
}
