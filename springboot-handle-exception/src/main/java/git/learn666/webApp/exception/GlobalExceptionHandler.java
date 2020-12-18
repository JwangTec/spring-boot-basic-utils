package git.learn666.webApp.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@ControllerAdvice(assignableTypes = {ExceptionHandler.class})
@ResponseBody
public class GlobalExceptionHandler {
//    ErrorResponse illegalArgumentResponse =  new ErrorResponse(new IllegalArgumentException("参数错误"));
//    ErrorResponse resourseNotFoundResponse = new ErrorResponse(new ResourceNotFoundException("Sorry, the resourse not found!"));
//
//    @ExceptionHandler(value = Exception.class)// 拦截所有异常
//    public ResponseEntity<ErrorResponse> exceptionHandler(Exception e) {
//
//        if (e instanceof IllegalArgumentException) {
//            return ResponseEntity.status(400).body(illegalArgumentResponse);
//        } else if (e instanceof ResourceNotFoundException) {
//            return ResponseEntity.status(404).body(resourseNotFoundResponse);
//        }
//        return null;
//    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<?> handleAppException(BaseException ex, HttpServletRequest request) {
        ErrorResponse representation = new ErrorResponse(ex, request.getRequestURI());
        return new ResponseEntity<>(representation, new HttpHeaders(), ex.getError().getStatus());
    }

    @ExceptionHandler(value = ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        ErrorResponse errorReponse = new ErrorResponse(ex, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorReponse);
    }
}
