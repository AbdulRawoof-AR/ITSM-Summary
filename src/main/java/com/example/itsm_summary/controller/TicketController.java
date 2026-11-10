package com.example.itsm_summary.controller;

import com.example.itsm_summary.service.TicketMongoService;
import org.bson.Document;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TicketController {

    private final TicketMongoService ticketMongoService;

    public TicketController(TicketMongoService ticketMongoService) {
        this.ticketMongoService = ticketMongoService;
    }

    @GetMapping("/tickets/unassigned/count")
    public long getUnassignedTicketCount() {
        return ticketMongoService.getUnassignedTicketCount();
    }

    @GetMapping("/agents/with-tickets")
    public List<Document> getAgentsWithTickets() {
        return ticketMongoService.getAgentsWithTickets();
    }

    @GetMapping("/workload/kpis")
    public Document getWorkloadKpis() {
        return ticketMongoService.getWorkloadKpis();
    }

    @GetMapping("/summary/status")
    public List<Document> getTicketStatusSummary() {
        return ticketMongoService.getTicketStatusSummary();
    }

    @GetMapping("/summary/kpis")
    public Document getSummaryKpis() {
        return ticketMongoService.getSummaryKpis();
    }

    @GetMapping("/summary/priority")
    public List<Document> getOpenTicketsByPriority() {
        return ticketMongoService.getOpenTicketsByPriority();
    }

    @GetMapping("/summary/category")
    public List<Document> getOpenTicketsByCategory() {
        return ticketMongoService.getOpenTicketsByCategory();
    }

    @GetMapping("/summary/last-7-days")
    public List<Document> getLast7DaysSummary() {
        return ticketMongoService.getLast7DaysSummary();
    }

    @GetMapping("/summary/last-7-days/total")
    public Document getLast7DaysTotal() {
        return ticketMongoService.getRecentlyUpdatedTicketsSummary(7);
    }

    @GetMapping("/summary/last-7-days/summary")
    public Document getLast7DaysDetailedSummary() {
        return ticketMongoService.getLast7DaysDetailedSummary();
    }

    @GetMapping("/summary/recently-updated")
    public Document getSummaryRecentlyUpdated(@RequestParam(defaultValue = "7") int limit) {
        return ticketMongoService.getRecentlyUpdatedTicketsSummary(limit);
    }

    @GetMapping("/tickets/recently-updated")
    public Document getRecentlyUpdatedTickets(@RequestParam(defaultValue = "7") int limit) {
        return ticketMongoService.getRecentlyUpdatedTicketsSummary(limit);
    }

    @GetMapping("/summary")
    public Document getCompleteSummary() {
        return ticketMongoService.getCompleteSummary();
    }
}
