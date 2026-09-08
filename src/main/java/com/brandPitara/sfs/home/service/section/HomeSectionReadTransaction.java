package com.brandPitara.sfs.home.service.section;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Defines the short, read-only transaction owned by a database-backed home section. */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
public @interface HomeSectionReadTransaction {
}
