package fastcampus.spring.batch.part1;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class HelloConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    /**
     * JobBuilderFactory은 스프링 빈에 이미 설정되어있고 이걸 생성자 주입으로 객체를 사용할 수 있
     * @param jobBuilderFactory
     * @param stepBuilderFactory
     */
    public HelloConfiguration(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
    }

    /**
     * Job은 배치의 실행 단위
     * @return
     */
    @Bean
    public Job helloJob() {
        return jobBuilderFactory.get("helloJob")
                .incrementer(new RunIdIncrementer()) // incrementer : Job의 실행 단위를 설정할 수 있음.
                // RunIdIncrementer 항상 잡이 실행할 때 마다 파라미터 아이디를 자동으로 생성해주는 클래
                .start(this.helloStep()) // 잡 실행 시 최초로 실행될 스텝의 메서
                .build();
    }

    @Bean
    public Step helloStep() {
        return stepBuilderFactory.get("helloStep")
                .tasklet((contribution, chunkContext) -> { // 테스크 기반과 정크 기반의 스텝이 있다. 여기선 테스크
                    log.info("hello spring batch");
                    return RepeatStatus.FINISHED;
                }).build()
                ;
    }
}
