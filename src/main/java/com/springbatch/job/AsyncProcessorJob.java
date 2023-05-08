package com.springbatch.job;

import com.springbatch.domain.Member;
import com.springbatch.dto.MemberVO;
import com.springbatch.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.file.transform.Range;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.Future;

@Slf4j
@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class AsyncProcessorJob {
    private final StepBuilderFactory stepBuilderFactory;
    private final JobBuilderFactory jobBuilderFactory;
    private final MemberRepository memberRepository;

    @Bean
    public Job asyncProcessJob() {
        return jobBuilderFactory.get("asyncProcessJob")
                .start(memberAsyncStep())
                .incrementer(new RunIdIncrementer())
                .build();
    }

    @Bean
    public Step memberAsyncStep() {
        return stepBuilderFactory.get("memberAsyncStep").<MemberVO, Future<MemberVO>>chunk(10)
                .reader(memberFlatFileItemReader()).processor(asyncMemberItemProcessor()).writer(asyncMemberItemWriter()).build();
    }

    @Bean
    public FlatFileItemReader<MemberVO> memberFlatFileItemReader() {
        return new FlatFileItemReaderBuilder<MemberVO>().name("memberFlatFileItemReader")
                .encoding("UTF-8")
                .resource(new FileSystemResource("/Users/parkchanseok/Develop/name.same"))
                .fixedLength()
                .addColumns(new Range(1, 3))
                .names("name")
                .targetType(MemberVO.class)
                .build();
    }

    @Bean
    public AsyncItemProcessor<MemberVO, MemberVO> asyncMemberItemProcessor() {
        AsyncItemProcessor<MemberVO, MemberVO> asyncItemProcessor = new AsyncItemProcessor<>();
        asyncItemProcessor.setDelegate(memberItemProcessor());
        asyncItemProcessor.setTaskExecutor(new SimpleAsyncTaskExecutor());
        return asyncItemProcessor;
    }

    @Bean
    public AsyncItemWriter<MemberVO> asyncMemberItemWriter() {
        AsyncItemWriter<MemberVO> asyncItemWriter = new AsyncItemWriter<>();
        asyncItemWriter.setDelegate(memberItemWriter());
        return asyncItemWriter;
    }

    @Bean
    @Transactional
    public ItemProcessor<MemberVO, MemberVO> memberItemProcessor() {
        return item -> {
            Thread.sleep(1000);

            Member member = memberRepository.findByName(item.getName());

            if(member == null) {
                return null;
            }

            return item;
        };
    }

    public FlatFileItemWriter<MemberVO> memberItemWriter() {
        DelimitedLineAggregator<MemberVO> lineAggregator = new DelimitedLineAggregator<>();
        BeanWrapperFieldExtractor<MemberVO> extractor = new BeanWrapperFieldExtractor<>();
        Resource resource = new FileSystemResource("/Users/parkchanseok/Develop/after_name.sam");
        lineAggregator.setFieldExtractor(extractor);

        extractor.setNames(new String[] {"name"});
        return new FlatFileItemWriterBuilder<MemberVO>()
                .name("memberItemWriter")
                .encoding("UTF-8")
                .resource(resource)
                .lineAggregator(lineAggregator)
                .build();
    }
    
}
