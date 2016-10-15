package com.sep.controllers;

import com.sep.domain.Client;
import com.sep.domain.EventPlanningRequest;
import com.sep.domain.User;
import com.sep.repositories.ClientRepository;
import com.sep.repositories.UserRepository;
import com.sep.services.EventPlanningRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.text.SimpleDateFormat;
import java.util.Date;

@Controller
@RequestMapping("/epr")
public class EventPlanningRequestController {
    Logger log = LoggerFactory.getLogger(EventPlanningRequestController.class);

    @Autowired
    private EventPlanningRequestService eprService;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private UserRepository userRepository;

    @RequestMapping(value = {"/list"}, method = RequestMethod.GET)
    public String list(Model model){
        model.addAttribute("eprs", eprService.listAllEventPlanningRequests());
        return "epr/list";
    }

    @RequestMapping("/{id}")
    public String showEventPlanningRequest(@PathVariable Long id, Model model){
        EventPlanningRequest epr = eprService.getEventPlanningRequestById(id);
        if (epr != null) {
            log.info("Yo yo, it exists: " + epr.getId());
        }
        model.addAttribute("epr", eprService.getEventPlanningRequestById(id));
        return "epr/view";
    }


    // XXX Populate clients!
    @RequestMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model){
        model.addAttribute("epr", eprService.getEventPlanningRequestById(id));
        return "epr/form";
    }

    @RequestMapping(value={"/new", "/create"})
    public String newEventPlanningRequest(Model model, @ModelAttribute("epr") EventPlanningRequest epr){
        model.addAttribute("clients", clientRepository.findAll());
        return "epr/form";
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        CustomDateEditor editor = new CustomDateEditor(new SimpleDateFormat("yyyy-MM-dd"), true);
        binder.registerCustomEditor(Date.class, editor);
    }
    @RequestMapping(value = "", method = RequestMethod.POST)
    public String saveEventPlanningRequest(@Valid @ModelAttribute("epr") EventPlanningRequest eventPlanningRequest,
                                           Errors errors,
                                           Model model,
                                           RedirectAttributes redirectAttributes){
        if (errors.hasErrors()) {
            log.info(errors.getAllErrors().toString());
            redirectAttributes.addFlashAttribute("error", "Invalid input, please try again.");
            redirectAttributes.addFlashAttribute("epr", eventPlanningRequest);
            return "redirect:/epr/create";
        }
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByUsername(username);
            eventPlanningRequest.setCreator(user);
            eprService.saveEventPlanningRequest(eventPlanningRequest);
            // TODO Save in clients
            Client client = eventPlanningRequest.getClient();
            client.addEpr(eventPlanningRequest);
            clientRepository.save(client);

            redirectAttributes.addFlashAttribute("info", "Event planning request successfully created.");
            return "redirect:/epr/" + eventPlanningRequest.getId();
        } catch(Exception ex) {
            log.error("Ex: " + ex.getMessage());
            model.addAttribute("epr", eventPlanningRequest);
            model.addAttribute("error", "Invalid input, please try again.");
            model.addAttribute("message", "Invalid input, please try again.");

            return "redirect:/epr/create";
        }
    }

}
