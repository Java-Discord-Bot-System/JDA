package com.almightyalpaca.discord.bot.system.events.manager;

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
     * Weather the handler supports async or not. Default is <b>false</b>.
	 */
	boolean async() default false;

	/**
	 * Not used yet, might be in the future.
	 * The priority of this method. Default is <b>0</b>.
	 */
	int priority() default 0;

}
