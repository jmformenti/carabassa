package org.atypical.carabassa.restapi.controller.impl;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Forwards all non-API frontend routes to the SPA's index.html so that
 * client-side routing (e.g. /dataset/test) works when served from the backend.
 */
@Controller
public class ForwardingController {

    private static final Resource INDEX_HTML = new ClassPathResource("resources/index.html");

    @RequestMapping(value = {"/", "/dataset/**", "/item/**"})
    @ResponseBody
    public ResponseEntity<Resource> spaRoutes() {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(INDEX_HTML);
    }
}
