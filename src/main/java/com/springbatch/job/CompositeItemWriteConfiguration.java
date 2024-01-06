package com.springbatch.job;

import com.springbatch.dto.MemberVO;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
public class CompositeItemWriteConfiguration {

    private final EntityManagerFactory entityManagerFactory;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job compositeJob() {
        return new JobBuilder("compositeJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(compositeStep())
                .build();
//        return jobBuilderFactory.get("memberJob").incrementer(new RunIdIncrementer())
//                .start(memberStep()).build();
    }

    @Bean
    public Step compositeStep() {
        return new StepBuilder("compositeStep", jobRepository)
                .<MemberVO, MemberVO>chunk(100, transactionManager)
                .reader(memberPagingItemReader())
                .writer(compositeMemberItemWriter())
                .build();
//        return stepBuilderFactory.get("memberStep").<MemberVO, MemberVO>chunk(100)
//                .reader(memberPagingItemReader()).writer(compositeMemberItemWriter()).build();
    }

    @Bean
    public JpaPagingItemReader<MemberVO> memberPagingItemReader() {

        return new JpaPagingItemReaderBuilder<MemberVO>()
                .name("memberPagingItemReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(100)
                .queryString("select m from Member m")
                .build();
    }

    @Bean
    public CompositeItemWriter<MemberVO> compositeMemberItemWriter() {
        CompositeItemWriter<MemberVO> compositeItemWriter = new CompositeItemWriter<>();
        compositeItemWriter.setDelegates(Arrays.asList(memberFlatFileItemWriter(), memberVOFlatFileItemWriter()));
        return compositeItemWriter;
    }

    @Bean
    public FlatFileItemWriter<MemberVO> memberFlatFileItemWriter() {
        BeanWrapperFieldExtractor<MemberVO> extractor = new BeanWrapperFieldExtractor<>();
        extractor.setNames(new String[] {"userNo", "email", "name", "password", "age"});

        DelimitedLineAggregator<MemberVO> lineAggregator = new DelimitedLineAggregator<>();
        lineAggregator.setDelimiter(",");
        lineAggregator.setFieldExtractor(extractor);

        return new FlatFileItemWriterBuilder<MemberVO>()
                .name("memberFlatFileItemWriter")
                .encoding(StandardCharsets.UTF_8.name())
                .resource(new FileSystemResource("/Users/parkchanseok/data.sam"))
                .lineAggregator(lineAggregator)
                .build();
    }

    @Bean
    public FlatFileItemWriter<MemberVO> memberVOFlatFileItemWriter() {
        DelimitedLineAggregator<MemberVO> lineAggregator = new DelimitedLineAggregator<>();
        BeanWrapperFieldExtractor<MemberVO> extractor = new BeanWrapperFieldExtractor<>();
        extractor.setNames(new String[] {"userNo", "email", "name", "password", "age"});
        lineAggregator.setDelimiter("|");
        lineAggregator.setFieldExtractor(extractor);

        return new FlatFileItemWriterBuilder<MemberVO>()
                .name("memberFlatFileItemWriter").encoding("MS949")
                .resource(new FileSystemResource("/Users/parkchanseok/data.dat"))
                .lineAggregator(lineAggregator)
                .build();
    }
}
