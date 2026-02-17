# Customizing ErrorPage 

*Disable Whitelabel Error Page*
1.
#server.error.whitelabel.enabled=false
2. Or,
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
3. Or 
@EnableAutoConfiguration(exclude = {ErrorMvcAutoConfiguration.class})
4. Or, Controller-View Technique
-----------------------------
STATIC Pages
-----------------------------
you can place the views like error.html in "res/static" w/o importing thymeleaf dependency
but you will need to return return "error.html"
```java
@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request) {
        //do something like logging
        return "error.html";
    }
}
```
Limitations : Cannot map dynamic data(Model) on views.
-----------------------------
DYNAMIC Pages(using Thymeleaf)
-----------------------------
place views like error.html in "res/resources/templates" & add Thymleaf in pom.xml
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```
and write a controller returning view name
```java
@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        model.setAttribute("key", "value");
        model.setAttribute("key", "value");
        model.setAttribute("key", "value");
        return "error";
    }
}
```
NOTE: @RequestMapping is a generic request mapper which accepts all type of HTTP verbs. 
To be more specific use its special subtypes e.g. GetMapping, PostMapping etc.

Now, we have made the view a dynamic page. 
Now, instead of returning views as html what if method returns JSON data so that we can 
facilitate machine to machine communication using APIs(Rest API to be specific) ?

Because
`return "{ \"message\" : \"Welcome to URL Shortner\"}";` 
gives following error
`org.thymeleaf.exceptions.TemplateInputException: Error resolving template [{ "message" : "Welcome to URL Shortner"}],`

Ans : use @ResponseBody annotation on method

```java
@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    @ResponseBody
    public String handleError(HttpServletRequest request) {
        return "{ \"message\" : \"Welcome to URL Shortner\"}";
    }
}
```

Still further if all methods of the controller is going to return JSON better to delegate
@ResponseBody at class level.
```java
@ResponseBody
@Controller
public class CustomErrorController implements ErrorController {

    @GetMapping("/error")
    public String handleError(HttpServletRequest request) {
        return "{ \"message\" : \"Welcome to URL Shortner\"}";
    }
    
    @GetMapping("/about")
    public String about(HttpServletRequest request) {
        return "{ \"about\" : \"Description of website\"}";
    }
}
```
Still combine two @ResponseBody + @Controller into single annotation @RestController 
