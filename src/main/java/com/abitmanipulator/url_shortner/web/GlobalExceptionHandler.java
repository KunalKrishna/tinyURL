package com.abitmanipulator.url_shortner.web;

import com.abitmanipulator.url_shortner.domain.Exception.ShortUrlNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalExceptionHandler {
    public static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // @ResponseStatus: the error page must carry a real error status, not 200,
    // so browsers, crawlers and (later) metrics/alerts can tell it apart from a success.
    @ExceptionHandler(ShortUrlNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String handleShortUrlNotFoundException(ShortUrlNotFoundException ex){
        LOGGER.warn("Short Url Not Found: {}", ex.getMessage());
        return "error/404";
    }

    // Rethrowing leaves the exception unhandled here, so Spring Security answers it:
    // 403 for a logged-in user, redirect to /login for an anonymous one.
    // Otherwise the catch-all below would turn a denied @PreAuthorize into a 500 page.
    @ExceptionHandler(AccessDeniedException.class)
    void handleAccessDenied(AccessDeniedException ex) {
        throw ex;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    String handleException(Exception ex){
        LOGGER.error("Exception Occurred: {}", ex.getMessage(), ex); // keep the stack trace
        return "error/500";
    }
}
