package com.springbatch.job;

import com.springbatch.dto.MemberVO;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
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
import org.springframework.core.io.Resource;

import javax.persistence.EntityManagerFactory;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class CompositeItemWriterJob {

    private final EntityManagerFactory entityManagerFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final JobBuilderFactory jobBuilderFactory;

    @Bean
    public Job memberJob() {
        return jobBuilderFactory.get("memberJob").incrementer(new RunIdIncrementer())
                .start(memberStep()).build();
    }

    @Bean
    public Step memberStep() {
        return stepBuilderFactory.get("memberStep").<MemberVO, MemberVO>chunk(100)
                .reader(memberPagingItemReader()).writer(compositeMemberItemWriter()).build();
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
        DelimitedLineAggregator<MemberVO> lineAggregator = new DelimitedLineAggregator<>();
        BeanWrapperFieldExtractor<MemberVO> extractor = new BeanWrapperFieldExtractor<>();
        Resource resource = new FileSystemResource("/Users/parkchanseok/data.sam");
        extractor.setNames(new String[] {"userNo", "email", "name", "password", "age"});
        lineAggregator.setDelimiter(",");
        lineAggregator.setFieldExtractor(extractor);

        return new FlatFileItemWriterBuilder<MemberVO>()
                .name("memberFlatFileItemWriter").encoding(StandardCharsets.UTF_8.name())
                .resource(resource)
                .lineAggregator(lineAggregator)
                .build();
    }

    @Bean
    public FlatFileItemWriter<MemberVO> memberVOFlatFileItemWriter() {
        DelimitedLineAggregator<MemberVO> lineAggregator = new DelimitedLineAggregator<>();
        BeanWrapperFieldExtractor<MemberVO> extractor = new BeanWrapperFieldExtractor<>();
        Resource resource = new FileSystemResource("/Users/parkchanseok/data.dat");
        extractor.setNames(new String[] {"userNo", "email", "name", "password", "age"});
        lineAggregator.setDelimiter("|");
        lineAggregator.setFieldExtractor(extractor);

        return new FlatFileItemWriterBuilder<MemberVO>()
                .name("memberFlatFileItemWriter").encoding("MS949")
                .resource(resource)
                .lineAggregator(lineAggregator)
                .build();
    }
}
