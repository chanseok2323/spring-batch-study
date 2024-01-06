package com.springbatch.job;

import com.springbatch.domain.Member;
import com.springbatch.dto.MemberVO;
import com.springbatch.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
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
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.Future;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AsyncProcessorConfiguration {
    private final MemberRepository memberRepository;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job asyncProcessJob() {
        return new JobBuilder("asyncProcessJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(asyncProcessorStep())
                .build();
//        return jobBuilderFactory.get("asyncProcessJob")
//                .start(memberAsyncStep())
//                .incrementer(new RunIdIncrementer())
//                .build();
    }

    @Bean
    public Step asyncProcessorStep() {
        return new StepBuilder("asyncProcessorStep", jobRepository)
                    .<MemberVO, Future<MemberVO>>chunk(100, transactionManager)
                    .reader(memberFlatFileItemReader())
                    .processor(asyncMemberItemProcessor())
                    .writer(asyncMemberItemWriter())
                .build();
//        return stepBuilderFactory.get("memberAsyncStep").<MemberVO, Future<MemberVO>>chunk(10)
//                .reader(memberFlatFileItemReader()).processor(asyncMemberItemProcessor()).writer(asyncMemberItemWriter()).build();
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
            Member member = memberRepository.findByName(item.getName());

            if(member == null) {
                return null;
            }

            return item;
        };
    }

    @Bean
    public FlatFileItemWriter<MemberVO> memberItemWriter() {
        BeanWrapperFieldExtractor<MemberVO> extractor = new BeanWrapperFieldExtractor<>();
        extractor.setNames(new String[] {"name"});

        DelimitedLineAggregator<MemberVO> lineAggregator = new DelimitedLineAggregator<>();
        lineAggregator.setFieldExtractor(extractor);

        return new FlatFileItemWriterBuilder<MemberVO>()
                .name("memberItemWriter")
                .encoding("UTF-8")
                .resource(new FileSystemResource("/Users/parkchanseok/Develop/after_name.sam"))
                .lineAggregator(lineAggregator)
                .build();
    }
    
}
