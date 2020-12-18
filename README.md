### 1. 使用 **@ControllerAdvice和**@ExceptionHandler处理全局异常

这是目前很常用的一种方式，非常推荐。测试代码中用到了 Junit 5，如果你新建项目验证下面的代码的话，记得添加上相关依赖。

**1. 新建异常信息实体类**

非必要的类，主要用于包装异常信息。

`src/main/java/com/twuc/webApp/exception/ErrorResponse.java`

```java
/**
 * @author shuang.kou
 */
public class ErrorResponse {

    private String message;
    private String errorTypeName;
  
    public ErrorResponse(Exception e) {
        this(e.getClass().getName(), e.getMessage());
    }

    public ErrorResponse(String errorTypeName, String message) {
        this.errorTypeName = errorTypeName;
        this.message = message;
    }
    ......省略getter/setter方法
}
```

**2. 自定义异常类型**

`src/main/java/com/twuc/webApp/exception/ResourceNotFoundException.java`

一般我们处理的都是 `RuntimeException` ，所以如果你需要自定义异常类型的话直接集成这个类就可以了。

```java
/**
 * @author shuang.kou
 * 自定义异常类型
 */
public class ResourceNotFoundException extends RuntimeException {
    private String message;

    public ResourceNotFoundException() {
        super();
    }

    public ResourceNotFoundException(String message) {
        super(message);
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
```

**3. 新建异常处理类**

我们只需要在类上加上`@ControllerAdvice`注解这个类就成为了全局异常处理类，当然你也可以通过 `assignableTypes `指定特定的 `Controller `类，让异常处理类只处理特定类抛出的异常。

`src/main/java/com/twuc/webApp/exception/GlobalExceptionHandler.java`

```java
/**
 * @author shuang.kou
 */
@ControllerAdvice(assignableTypes = {ExceptionController.class})
@ResponseBody
public class GlobalExceptionHandler {

    ErrorResponse illegalArgumentResponse = new ErrorResponse(new IllegalArgumentException("参数错误!"));
    ErrorResponse resourseNotFoundResponse = new ErrorResponse(new ResourceNotFoundException("Sorry, the resourse not found!"));

    @ExceptionHandler(value = Exception.class)// 拦截所有异常, 这里只是为了演示，一般情况下一个方法特定处理一种异常
    public ResponseEntity<ErrorResponse> exceptionHandler(Exception e) {

        if (e instanceof IllegalArgumentException) {
            return ResponseEntity.status(400).body(illegalArgumentResponse);
        } else if (e instanceof ResourceNotFoundException) {
            return ResponseEntity.status(404).body(resourseNotFoundResponse);
        }
        return null;
    }
}
```

**4. controller模拟抛出异常**

`src/main/java/com/twuc/webApp/web/ExceptionController.java`

```java
/**
 * @author shuang.kou
 */
@RestController
@RequestMapping("/api")
public class ExceptionController {

    @GetMapping("/illegalArgumentException")
    public void throwException() {
        throw new IllegalArgumentException();
    }

    @GetMapping("/resourceNotFoundException")
    public void throwException2() {
        throw new ResourceNotFoundException();
    }
}
```

使用  Get 请求 [localhost:8080/api/resourceNotFoundException](localhost:8333/api/resourceNotFoundException) （curl -i -s -X GET url），服务端返回的 JSON 数据如下：

```json
{
    "message": "Sorry, the resourse not found!",
    "errorTypeName": "com.twuc.webApp.exception.ResourceNotFoundException"
}
```

**5. 编写测试类** 

MockMvc 由`org.springframework.boot.test`包提供，实现了对Http请求的模拟，一般用于我们测试  controller 层。

```java
/**
 * @author shuang.kou
 */
@AutoConfigureMockMvc
@SpringBootTest
public class ExceptionTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void should_return_400_if_param_not_valid() throws Exception {
        mockMvc.perform(get("/api/illegalArgumentException"))
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.message").value("参数错误!"));
    }

    @Test
    void should_return_404_if_resourse_not_found() throws Exception {
        mockMvc.perform(get("/api/resourceNotFoundException"))
                .andExpect(status().is(404))
                .andExpect(jsonPath("$.message").value("Sorry, the resourse not found!"));
    }
}
```

### 2. @ExceptionHandler 处理 Controller 级别的异常

我们刚刚也说了使用`@ControllerAdvice`注解 可以通过 `assignableTypes `指定特定的类，让异常处理类只处理特定类抛出的异常。所以这种处理异常的方式，实际上现在使用的比较少了。

 我们把下面这段代码移到 `src/main/java/com/twuc/webApp/exception/GlobalExceptionHandler.java` 中就可以了。

```java
    @ExceptionHandler(value = Exception.class)// 拦截所有异常
    public ResponseEntity<ErrorResponse> exceptionHandler(Exception e) {

        if (e instanceof IllegalArgumentException) {
            return ResponseEntity.status(400).body(illegalArgumentResponse);
        } else if (e instanceof ResourceNotFoundException) {
            return ResponseEntity.status(404).body(resourseNotFoundResponse);
        }
        return null;
    }
```

### 3. ResponseStatusException

研究 ResponseStatusException 我们先来看看，通过  `ResponseStatus`注解简单处理异常的方法（将异常映射为状态码）。

`src/main/java/com/twuc/webApp/exception/ResourceNotFoundException.java`

 ```java
@ResponseStatus(code = HttpStatus.NOT_FOUND)
public class ResourseNotFoundException2 extends RuntimeException {

    public ResourseNotFoundException2() {
    }

    public ResourseNotFoundException2(String message) {
        super(message);
    }
}
 ```

 `src/main/java/com/twuc/webApp/web/ResponseStatusExceptionController.java`

```java
@RestController
@RequestMapping("/api")
public class ResponseStatusExceptionController {
    @GetMapping("/resourceNotFoundException2")
    public void throwException3() {
        throw new ResourseNotFoundException2("Sorry, the resourse not found!");
    }
}
```

  使用  Get 请求 [localhost:8080/api/resourceNotFoundException2](localhost:8333/api/resourceNotFoundException2) ，服务端返回的 JSON 数据如下：

```json
{
    "timestamp": "2019-08-21T07:11:43.744+0000",
    "status": 404,
    "error": "Not Found",
    "message": "Sorry, the resourse not found!",
    "path": "/api/resourceNotFoundException2"
}
```

这种通过 `ResponseStatus`注解简单处理异常的方法是的好处是比较简单，但是一般我们不会这样做，通过`ResponseStatusException`会更加方便,可以避免我们额外的异常类。

```java
    @GetMapping("/resourceNotFoundException2")
    public void throwException3() {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sorry, the resourse not found!", new ResourceNotFoundException());
    }
```

  使用  Get 请求 [localhost:8080/api/resourceNotFoundException2](localhost:8333/api/resourceNotFoundException2) ，服务端返回的 JSON 数据如下,和使用 `ResponseStatus`  实现的效果一样：

 ```json
{
    "timestamp": "2019-08-21T07:28:12.017+0000",
    "status": 404,
    "error": "Not Found",
    "message": "Sorry, the resourse not found!",
    "path": "/api/resourceNotFoundException3"
}
 ```

`ResponseStatusException` 提供了三个构造方法：

```java
	public ResponseStatusException(HttpStatus status) {
		this(status, null, null);
	}

	public ResponseStatusException(HttpStatus status, @Nullable String reason) {
		this(status, reason, null);
	}

	public ResponseStatusException(HttpStatus status, @Nullable String reason, @Nullable Throwable cause) {
		super(null, cause);
		Assert.notNull(status, "HttpStatus is required");
		this.status = status;
		this.reason = reason;
	}

```

构造函数中的参数解释如下：

- status ： http status
- reason ：response 的消息内容
- cause ： 抛出的异常




## 效果

返回的信息包含了异常下面 5 部分内容：

1. 唯一标示异常的 code
2. HTTP状态码
3. 错误路径
4. 发生错误的时间戳
5. 错误的具体信息

这样返回异常信息，更利于我们前端根据异常信息做出相应的表现。

## 异常处理核心代码

`ErrorCode.java` (此枚举类中包含了异常的唯一标识、HTTP状态码以及错误信息)

这个类的主要作用就是统一管理系统中可能出现的异常，比较清晰明了。但是，可能出现的问题是当系统过于复杂，出现的异常过多之后，这个类会比较庞大。有一种解决办法：将多种相似的异常统一为一个，比如将用户找不到异常和订单信息未找到的异常都统一为“未找到该资源”这一种异常，然后前端再对相应的情况做详细处理（我个人的一种处理方法，不敢保证是比较好的一种做法）。

```java
import org.springframework.http.HttpStatus;


public enum ErrorCode {
  
    RESOURCE_NOT_FOUND(1001, HttpStatus.NOT_FOUND, "未找到该资源"),
    REQUEST_VALIDATION_FAILED(1002, HttpStatus.BAD_REQUEST, "请求数据格式验证失败");
    private final int code;

    private final HttpStatus status;

    private final String message;

    ErrorCode(int code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "ErrorCode{" +
                "code=" + code +
                ", status=" + status +
                ", message='" + message + '\'' +
                '}';
    }
}
```

**`ErrorReponse.java`（返回给客户端具体的异常对象）**

这个类作为异常信息返回给客户端，里面包括了当出现异常时我们想要返回给客户端的所有信息。

```java
import org.springframework.util.ObjectUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class ErrorReponse {
    private int code;
    private int status;
    private String message;
    private String path;
    private Instant timestamp;
    private HashMap<String, Object> data = new HashMap<String, Object>();

    public ErrorReponse() {
    }

    public ErrorReponse(BaseException ex, String path) {
        this(ex.getError().getCode(), ex.getError().getStatus().value(), ex.getError().getMessage(), path, ex.getData());
    }

    public ErrorReponse(int code, int status, String message, String path, Map<String, Object> data) {
        this.code = code;
        this.status = status;
        this.message = message;
        this.path = path;
        this.timestamp = Instant.now();
        if (!ObjectUtils.isEmpty(data)) {
            this.data.putAll(data);
        }
    }

// 省略 getter/setter 方法

    @Override
    public String toString() {
        return "ErrorReponse{" +
                "code=" + code +
                ", status=" + status +
                ", message='" + message + '\'' +
                ", path='" + path + '\'' +
                ", timestamp=" + timestamp +
                ", data=" + data +
                '}';
    }
}

```

**`BaseException.java`（继承自 `RuntimeException` 的抽象类，可以看做系统中其他异常类的父类）**

系统中的异常类都要继承自这个类。

```java

public abstract class BaseException extends RuntimeException {
    private final ErrorCode error;
    private final HashMap<String, Object> data = new HashMap<>();

    public BaseException(ErrorCode error, Map<String, Object> data) {
        super(error.getMessage());
        this.error = error;
        if (!ObjectUtils.isEmpty(data)) {
            this.data.putAll(data);
        }
    }

    protected BaseException(ErrorCode error, Map<String, Object> data, Throwable cause) {
        super(error.getMessage(), cause);
        this.error = error;
        if (!ObjectUtils.isEmpty(data)) {
            this.data.putAll(data);
        }
    }

    public ErrorCode getError() {
        return error;
    }

    public Map<String, Object> getData() {
        return data;
    }

}
```

**`ResourceNotFoundException.java` （自定义异常）**

可以看出通过继承 `BaseException` 类我们自定义异常会变的非常简单！

```java
import java.util.Map;

public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(Map<String, Object> data) {
        super(ErrorCode.RESOURCE_NOT_FOUND, data);
    }
}
```

**`GlobalExceptionHandler.java`（全局异常捕获）**

我们定义了两个异常捕获方法。

这里再说明一下，实际上这个类只需要 `handleAppException()` 这一个方法就够了，因为它是本系统所有异常的父类。只要是抛出了继承 `BaseException` 类的异常后都会在这里被处理。

```java
import com.twuc.webApp.web.ExceptionController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import javax.servlet.http.HttpServletRequest;

@ControllerAdvice(assignableTypes = {ExceptionController.class})
@ResponseBody
public class GlobalExceptionHandler {
    
    // 也可以将 BaseException 换为 RuntimeException 
    // 因为 RuntimeException 是 BaseException 的父类
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<?> handleAppException(BaseException ex, HttpServletRequest request) {
        ErrorReponse representation = new ErrorReponse(ex, request.getRequestURI());
        return new ResponseEntity<>(representation, new HttpHeaders(), ex.getError().getStatus());
    }

    @ExceptionHandler(value = ResourceNotFoundException.class)
    public ResponseEntity<ErrorReponse> handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        ErrorReponse errorReponse = new ErrorReponse(ex, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorReponse);
    }
}

```

**（重要）一点扩展：**

哈哈！实际上我多加了一个算是多余的异常捕获方法`handleResourceNotFoundException()` 主要是为了考考大家当我们抛出了 `ResourceNotFoundException`异常会被下面哪一个方法捕获呢？

答案：

会被`handleResourceNotFoundException()`方法捕获。因为 `@ExceptionHandler` 捕获异常的过程中，会优先找到最匹配的。

下面通过源码简单分析一下：

`ExceptionHandlerMethodResolver.java`中`getMappedMethod`决定了具体被哪个方法处理。

```java

@Nullable
	private Method getMappedMethod(Class<? extends Throwable> exceptionType) {
		List<Class<? extends Throwable>> matches = new ArrayList<>();
    //找到可以处理的所有异常信息。mappedMethods 中存放了异常和处理异常的方法的对应关系
		for (Class<? extends Throwable> mappedException : this.mappedMethods.keySet()) {
			if (mappedException.isAssignableFrom(exceptionType)) {
				matches.add(mappedException);
			}
		}
    // 不为空说明有方法处理异常
		if (!matches.isEmpty()) {
      // 按照匹配程度从小到大排序
			matches.sort(new ExceptionDepthComparator(exceptionType));
      // 返回处理异常的方法
			return this.mappedMethods.get(matches.get(0));
		}
		else {
			return null;
		}
	}
```

从源代码看出：**`getMappedMethod()`会首先找到可以匹配处理异常的所有方法信息，然后对其进行从小到大的排序，最后取最小的那一个匹配的方法(即匹配度最高的那个)。**

## 写一个抛出异常的类测试

**`Person.java`**

```java
public class Person {
    private Long id;
    private String name;

    // 省略 getter/setter 方法
}
```

**`ExceptionController.java`（抛出异常的类）**

```java
@RestController
@RequestMapping("/api")
public class ExceptionController {

    @GetMapping("/resourceNotFound")
    public void throwException() {
        Person p=new Person(1L,"SnailClimb");
        throw new ResourceNotFoundException(ImmutableMap.of("person id:", p.getId()));
    }

}
```

源码地址：https://github.com/Snailclimb/springboot-guide/tree/master/source-code/basis/springboot-handle-exception-improved

