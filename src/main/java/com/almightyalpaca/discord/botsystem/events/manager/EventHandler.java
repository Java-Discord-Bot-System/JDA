package com.almightyalpaca.discord.botsystem.events.manager;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EventHandler {

	/**
	 * Not used yet, might be in the future.
	 */
	boolean async() default false;

	/**
	 * The priority of this method. Default is <b>0</b>.
	 */
	int priority() default 0;

}
