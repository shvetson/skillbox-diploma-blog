package ru.shvets.blog.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class TaskGlobalExceptionHandler {
    @ExceptionHandler
    public ResponseEntity<TaskIncorrectData> handleException(NoSuchTaskException exception){
        TaskIncorrectData data = new TaskIncorrectData();
        data.setInfo(exception.getMessage());
        return new ResponseEntity<>(data, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler
    public ResponseEntity<TaskIncorrectData> handleException(Exception exception){
        TaskIncorrectData data = new TaskIncorrectData();
        data.setInfo(exception.getMessage());
        return new ResponseEntity<>(data, HttpStatus.BAD_REQUEST);
    }
}
