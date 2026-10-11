package co.edu.uco.infraestructure.primaryadapters;

import co.edu.uco.application.primaryports.dto.message.ResolvedMessageDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping("${crosswords.api.path.messages}")
public interface ResolveMessageController {

    @GetMapping(value = "/resolve", produces = "application/json")
    ResolvedMessageDTO resolve(@RequestParam("code") String code,
                               HttpServletRequest request, HttpServletResponse response);
}
