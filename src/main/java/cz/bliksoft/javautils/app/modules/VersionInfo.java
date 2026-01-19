package cz.bliksoft.javautils.app.modules;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Deprecated
public @interface VersionInfo {
	int majorVersion() default 1;

	int minorVersion() default 0;

	int revisionVersion() default 0;
}
