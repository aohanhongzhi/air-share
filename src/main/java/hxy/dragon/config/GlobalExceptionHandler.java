package hxy.dragon.config;


import hxy.dragon.entity.reponse.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.util.validation.ValidationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

/**
 * Description:全局异常处理，采用@RestControllerAdvice + @ExceptionHandler解决 <br>
 * 自定义异常处理类
 *
 * @author eric
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler implements InitializingBean {

    /**
     * Hide exception field in the return object
     *
     * @return
     */
    @Bean
    @Profile("prod")
    public ErrorAttributes errorAttributes() {
        return new DefaultErrorAttributes() {
            @Override
            public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
                return super.getErrorAttributes(webRequest,
                        ErrorAttributeOptions.defaults().excluding(ErrorAttributeOptions.Include.EXCEPTION));
            }
        };
    }


    /**
     * 处理异常的访问链接
     *
     * @param e
     * @param webRequest
     * @return
     */
    @ExceptionHandler(value = {NoHandlerFoundException.class})
    public ModelAndView noMappingHandler(Exception e, WebRequest webRequest) {
        log.error("No mapping exception:{}", e.getMessage(), e);
        ModelAndView modelAndView = new ModelAndView(new MappingJackson2JsonView());
        modelAndView.addObject("code", 404);
        modelAndView.addObject("msg", "接口异常，详情查询服务端");
        modelAndView.addObject("data", e.getMessage());
        return modelAndView;
    }

    /**
     * 方法参数校验 https://blog.csdn.net/chengliqu4475/article/details/100834090
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public <T> BaseResponse<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("{}", e.getMessage(), e);
        return BaseResponse.error("参数检验出错啦！", e.getBindingResult().getFieldError().getDefaultMessage());
    }

    /**
     * 处理400参数错误
     *
     * @param e
     * @return
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public BaseResponse handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("参数解析失败{}", e.getMessage(), e);
        return BaseResponse.badrequest("参数解析失败", e.getMessage());
    }

    /**
     * 405错误方法不支持
     *
     * @param e
     * @return
     */
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public BaseResponse handleHttpRequestMethodNotSupportedException(HttpServletRequest request,
                                                                     HttpRequestMethodNotSupportedException e) {
        log.warn("\n====>[{}]不支持当前[{}]请求方法,应该是[{},{}]", request.getRequestURI(), e.getMethod(),
                e.getSupportedHttpMethods(), e.getSupportedMethods(), e);
        return BaseResponse.badrequest("请求方法不支持", e.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public BaseResponse MissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.warn("\n====>请求参数错误{}", e.getMessage(), e);
        return BaseResponse.badrequest("请求参数错误", e.getMessage());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public BaseResponse handleDuplicateKeyException(DuplicateKeyException e) {
        log.warn("\n====>", e);
        return BaseResponse.badrequest("请求参数错误", e.getMessage());

    }

    /**
     * 这个应该放在最下面比较好，最后加载 处理未定义的其他异常信息 参数为空等
     *
     * @param request
     * @param exception
     * @return
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = Exception.class)
    public BaseResponse exceptionHandler(HttpServletRequest request, Exception exception) {
        String message = "系统发生错误";
        log.error("当前URL={} ", request.getRequestURI(), exception);

        if (exception instanceof NullPointerException) {
            message = "biu，踩雷啦！";
        } else if (exception instanceof ValidationException) {
            message = "参数检验出错啦！";
        }

        return BaseResponse.error(message, exception.getMessage());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("\n====>全局异常注入正常");
    }

    /**
     * bean注入初始化
     */
    @PostConstruct
    public void init() {
        log.info("\n====>@PostConstruct 全局异常注入正常");
    }
}
