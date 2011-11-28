package de.cau.dataprocessing.filters.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Configuration {


	public boolean value() default false;
}
