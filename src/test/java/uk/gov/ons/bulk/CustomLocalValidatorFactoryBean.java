package uk.gov.ons.bulk;

import org.hibernate.validator.HibernateValidator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import javax.validation.Configuration;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;
import java.util.List;

public class CustomLocalValidatorFactoryBean extends LocalValidatorFactoryBean {

    private final List<ConstraintValidator<?, ?>> customConstraintValidators;

    public CustomLocalValidatorFactoryBean(List<ConstraintValidator<?, ?>> customConstraintValidators) {
        this.customConstraintValidators = customConstraintValidators;
        setProviderClass(HibernateValidator.class);
        afterPropertiesSet();
    }

    @Override
    protected void postProcessConfiguration(Configuration<?> configuration) {
        super.postProcessConfiguration(configuration);
        ConstraintValidatorFactory defaultConstraintValidatorFactory =
                configuration.getDefaultConstraintValidatorFactory();
        configuration.constraintValidatorFactory(
                new ConstraintValidatorFactory() {
                    @Override
                    public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
                        for (ConstraintValidator<?, ?> constraintValidator : customConstraintValidators) {
                            if (key.equals(constraintValidator.getClass())) //noinspection unchecked
                                return (T) constraintValidator;
                        }
                        return defaultConstraintValidatorFactory.getInstance(key);
                    }

                    @Override
                    public void releaseInstance(ConstraintValidator<?, ?> instance) {
                        defaultConstraintValidatorFactory
                                .releaseInstance(instance);
                    }
                }
        );
    }

}
