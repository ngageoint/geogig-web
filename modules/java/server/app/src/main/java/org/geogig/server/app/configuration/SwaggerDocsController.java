package org.geogig.server.app.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Home redirection to swagger api documentation
 */
@Controller
@Configuration
@EnableSwagger2
public class SwaggerDocsController {

    @RequestMapping(value = "/docs")
    public String docs() {
        return "redirect:swagger-ui.html";
    }
}
