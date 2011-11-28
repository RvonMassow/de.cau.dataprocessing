package de.cau.dataprocessing.filters.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;



@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface InputPort {

	Configuration config() default @Configuration(false);

	boolean required() default false;
}
