package fastcampus.spring.batch.part3;

import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@Slf4j
public class ChunkProcessingConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    public ChunkProcessingConfiguration(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
    }

    @Bean
    public Job chunkProcessingJob() {
        return jobBuilderFactory.get("chunkProcessingJob")
                .incrementer(new RunIdIncrementer())
                .start(this.taskBaseStep())
                .next(this.chunkBaseStep(null)) // 파라미터에 null로 실행해도 스프링 벨류 어노테잇녀으로 가져온다.
                .build();
    }

    /**
     * 정크 스텝
     * 스프링이 제공하는 @value 에노테이션 사용하여 chunk 사이즈 변경
     * ItemReader에서 null을 반환할 때까지 반복 함
     * @return
     */
    @Bean
    @JobScope // @Value 에노테이션으로 program arguments의 chunkSize 변수 값을 가져올 수 있게 한다
    public Step chunkBaseStep(@Value("#{jobParameters[chunkSize]}") String chunkSize) { // chunkSize는 스프링부트 실행 설정의 Program arguments에 '-chunkSize=20' 옵션을 가져온다.
        return stepBuilderFactory.get("chunkBaseStep")
                //// 데이터를 10번 나누라는 뜻이 아니고 10개씩 나눈다는 뜻. (예 : 데이터가 1000개 일 경우 chunk 10 은 데이터를 10개씩 묶어서 100번 실행. 만약 5이면 200번 실행)
                .<String, String>chunk(StringUtils.isNotEmpty(chunkSize) ? Integer.parseInt(chunkSize) : 10) // 첫번째 제네릭은 ItemReader에서 반환하는 input Type, 두 번째 제네릭은 ItemWriter에서 반환하는 output Type이다.
                .reader(itemReader())
                .processor(itemProcessor())
                .writer(itemWriter())
                .build();
    }

    /**
     * chunk Writer
     * @return
     */
    private ItemWriter<? super String> itemWriter() {
        // 여기서 items는 List<output> 타입이다. List의 Size는 <String, String>chunk(10) 여기의 파라미터에서 결정된다.
        return items -> log.info("chunk item size: {}", items.size());
//        return items -> items.forEach(log::info);
    }

    /**
     * chunk Processor
     * @return
     */
    private ItemProcessor<? super String, String> itemProcessor() {
        return item -> item + ", String Batsh"; // return 값이 null이면 writer로 값이 넘어가지 않는다.
    }

    /**
     * chunk Reader
     * @return
     */
    private ItemReader<String> itemReader() {
        return new ListItemReader<>(getItems());
    }

    @Bean
    public Step taskBaseStep() {
        return stepBuilderFactory.get("taskBaseStep")
                .tasklet(this.tasklet())
                .build();
    }

    /**
     * tasklet -> chunk 처럼 실행되게 만들기(비추라고 함)
     * @return
     */
    private Tasklet tasklet() {
        List<String> items = getItems();
        return (contribution, chunkContext) -> {
            StepExecution stepExecution = contribution.getStepExecution();
            JobParameters jobParameters = stepExecution.getJobParameters();

            String value = jobParameters.getString("chunkSize", "10"); // chunkSize는 스프링부트 실행 설정의 Program arguments에 '-chunkSize=20' 옵션을 가져온다.
            int chunkSize = StringUtils.isNotEmpty(value) ? Integer.parseInt(value) : 10;

            int fromIndex = stepExecution.getReadCount(); // for문으로 치차면 초기화 변수
            int toIndex = fromIndex + chunkSize;

            if(fromIndex >= items.size()) { // for문으로 치자면 중간에 종료 조건
                return RepeatStatus.FINISHED;
            }

            List<String> subList = items.subList(fromIndex, toIndex);

            log.info("task item size : {}", subList.size());

            stepExecution.setReadCount(toIndex); // for 문으로 치차면 맨 끝에 ++ 넣는 곳

            return RepeatStatus.CONTINUABLE; // 반복되도록 함
        };    }

    private List<String> getItems() {
        List<String> items = new ArrayList<>();

        for(int i=0; i<100; i++) {
            items.add(i + " Hello");
        }

        return items;
    }
}
