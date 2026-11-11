package com.acme.notify.template;

import com.acme.notify.common.RenderRequest;
import com.acme.notify.common.RenderedContent;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateRenderingService rendering;

    @PostMapping("/render")
    public RenderedContent render(@RequestBody RenderRequest request) {
        return rendering.render(request);
    }

    @ExceptionHandler(TemplateNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(TemplateNotFoundException e) {
        return e.getMessage();
    }
}
