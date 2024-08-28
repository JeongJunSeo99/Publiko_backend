package com.kobot.backend.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class ExecutionTimeAspect {

  @Around("execution(* com.kobot.backend.controller..*(..))")
  public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
    long startTime = System.currentTimeMillis();

    Object proceed = joinPoint.proceed();  // 실제 메서드 실행

    long executionTime = System.currentTimeMillis() - startTime;

    log.info(joinPoint.getSignature() + " executed in " + executionTime + "ms");

    return proceed;
  }
}
