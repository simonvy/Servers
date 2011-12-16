package javax.persistence;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface OneToOne {
	
	String mappedBy();
	
	CascadeType[] cascade();
	
}
