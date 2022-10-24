package uk.gov.ons.bulk.config;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
public class BulkWebConfig implements WebMvcConfigurer {

    @Value("${web.async.executor.thread.core_pool_size}")
    private int corePoolSize;
    @Value("${web.async.executor.thread.max_pool_size}")
    private int maxPoolSize;
    @Value("${web.async.executor.thread.queue_capacity}")
    private int queueCapacity;
    @Value("${web.async.executor.thread.keep_alive_seconds}")
    private int keepAliveSeconds;
    @Value("${web.async.executor.thread.name.prefix}")
    private String namePrefix;
    
	@Override
	public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
		
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(corePoolSize);
		executor.setMaxPoolSize(maxPoolSize);
		executor.setQueueCapacity(queueCapacity);
		executor.setAllowCoreThreadTimeOut(true);
		executor.setKeepAliveSeconds(keepAliveSeconds);
		executor.setThreadNamePrefix(namePrefix);
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		executor.initialize();
        configurer.setTaskExecutor(executor);
	}

	@Override
	public Validator getValidator() {
		LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
	    bean.setValidationMessageSource(messageSource());
	    return bean;
	}

	@Bean
	public MessageSource messageSource() {
	    ReloadableResourceBundleMessageSource messageSource
	      = new ReloadableResourceBundleMessageSource();
	    
	    messageSource.setBasename("classpath:messages");
	    messageSource.setDefaultEncoding("UTF-8");
	    return messageSource;
	}	
}
