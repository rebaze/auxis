package org.auxis.jrunner;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(value = RetentionPolicy.RUNTIME)
@RunConfiguration(jar="tbd")
public @interface TigerConfiguration {
}
