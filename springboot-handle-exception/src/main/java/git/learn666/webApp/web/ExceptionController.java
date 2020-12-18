package git.learn666.webApp.web;

import com.google.common.collect.ImmutableMap;
import git.learn666.webApp.entity.Person;
import git.learn666.webApp.exception.ResourceNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ExceptionController {
//
//    @GetMapping("/illegalArgumentException")
//    public void throwException() {
//        throw new IllegalArgumentException();
//    }
//
//    @GetMapping("/resourceNotFoundException")
//    public void throwException2() {
//        throw new ResourceNotFoundException();
//    }

    @GetMapping("/resourceNotFound")
    public void throwException() {
        Person p=new Person(1L,"SnailClimb");
        throw new ResourceNotFoundException(ImmutableMap.of("person id:", p.getId()));
    }
}
