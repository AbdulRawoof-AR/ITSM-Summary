package com.example.itsm_summary.service;

import com.example.itsm_summary.model.Ticket;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class TicketMongoService {

    private final MongoTemplate mongoTemplate;

    public TicketMongoService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public long getUnassignedTicketCount() {

        Query query = new Query();

        query.addCriteria(
                Criteria.where("status").is("NEW")
        );

        return mongoTemplate.count(query, Ticket.class);
    }

    public List<Document> getAgentsWithTickets() {

        Aggregation aggregation = Aggregation.newAggregation(

                context -> new Document(
                        "$match",
                        new Document("agent",
                                new Document("$ne", null)
                                        .append("$exists", true))
                ),

                context -> new Document(
                        "$group",
                        new Document("_id", "$agent")
                                .append("assigned",
                                        new Document("$sum",
                                                new Document("$cond", List.of(
                                                        new Document("$eq",
                                                                List.of("$status", "ASSIGNED")),
                                                        1, 0
                                                ))
                                        )
                                )
                                .append("inProgress",
                                        new Document("$sum",
                                                new Document("$cond", List.of(
                                                        new Document("$eq",
                                                                List.of("$status", "IN_PROGRESS")),
                                                        1, 0
                                                ))
                                        )
                                )
                                .append("pending",
                                        new Document("$sum",
                                                new Document("$cond", List.of(
                                                        new Document("$eq",
                                                                List.of("$status", "PENDING")),
                                                        1, 0
                                                ))
                                        )
                                )
                                .append("transfer",
                                        new Document("$sum",
                                                new Document("$cond", List.of(
                                                        new Document("$eq",
                                                                List.of("$status", "TRANSFER")),
                                                        1, 0
                                                ))
                                        )
                                )
                                .append("closed",
                                        new Document("$sum",
                                                new Document("$cond", List.of(
                                                        new Document("$eq",
                                                                List.of("$status", "CLOSED")),
                                                        1, 0
                                                ))
                                        )
                                )
                                .append("overdue",
                                        new Document("$sum",
                                                new Document("$cond", Arrays.asList(
                                                        new Document("$and", List.of(
                                                                new Document("$ne",
                                                                        List.of("$status", "CLOSED")
                                                                ),
                                                                new Document("$ne",
                                                                        Arrays.asList(
                                                                                new Document("$ifNull",
                                                                                        Arrays.asList("$dueAt", null)
                                                                                ),
                                                                                null
                                                                        )
                                                                ),
                                                                new Document("$lt",
                                                                        List.of("$dueAt", "$$NOW")
                                                                )
                                                        )),
                                                        1,
                                                        0
                                                ))
                                        )
                                )
                ),
                context -> new Document(
                        "$set",
                        new Document(
                                "open",
                                new Document("$add", List.of(
                                        "$assigned",
                                        "$inProgress",
                                        "$pending",
                                        "$transfer"
                                ))
                        )
                ),

                context -> new Document(
                        "$project",
                        new Document("_id", 0)
                                .append("name", "$_id")
                                .append("assigned", 1)
                                .append("inProgress", 1)
                                .append("pending", 1)
                                .append("transfer", 1)
                                .append("open", 1)
                                .append("overdue", 1)
                                .append("closed", 1)
                ),

                context -> new Document(
                        "$sort",
                        new Document("open", -1)
                )
        );

        return mongoTemplate.aggregate(
                aggregation,
                Ticket.class,
                Document.class
        ).getMappedResults();
    }

    public long getOpenTicketCount() {

        Query query = new Query();

        query.addCriteria(
                Criteria.where("status").ne("CLOSED")
        );

        return mongoTemplate.count(query, Ticket.class);
    }

    public Document getWorkloadKpis() {

        List<Document> agents = getAgentsWithTickets();

        long openTickets = getOpenTicketCount();

        long unassigned = getUnassignedTicketCount();

        int totalOpen = agents.stream()
                .mapToInt(agent -> {
                    Integer open = agent.getInteger("open");
                    return open != null ? open : 0;
                })
                .sum();

        double avgOpenPerAgent = agents.isEmpty()
                ? 0.0
                : Math.round(
                (double) totalOpen / agents.size() * 10
        ) / 10.0;

        Document mostLoaded = null;

        if (!agents.isEmpty() && totalOpen > 0) {

            Document topAgent = agents.get(0);

            Integer openVal = topAgent.getInteger("open");
            String topAgentName = topAgent.getString("name");
            mostLoaded = new Document(
                    "agent", topAgentName != null ? topAgentName : ""
            ).append(
                    "openCount", openVal != null ? openVal : 0
            );
        }

        Document kpis = new Document("openTickets", openTickets)
                .append("unassigned", unassigned)
                .append("avgOpenPerAgent", avgOpenPerAgent)
                .append("mostLoaded", mostLoaded);

        return new Document("kpis", kpis)
                .append("agents", agents)
                .append("unassignedQueue", unassigned);
    }

    public List<Document> getTicketStatusSummary() {

        Aggregation aggregation = Aggregation.newAggregation(

                context -> new Document(
                        "$group",
                        new Document("_id", "$status")
                                .append("count", new Document("$sum", 1))
                )
        );

        List<Document> results = mongoTemplate.aggregate(
                aggregation,
                Ticket.class,
                Document.class
        ).getMappedResults();

        List<String> statuses = List.of(
                "NEW",
                "ASSIGNED",
                "IN_PROGRESS",
                "PENDING",
                "TRANSFER",
                "CLOSED"
        );

        List<Document> finalResults = new ArrayList<>();

        for (String status : statuses) {

            int count = results.stream()
                    .filter(item -> status.equalsIgnoreCase(item.getString("_id")))
                    .mapToInt(item -> {
                        Object c = item.get("count");
                        return c instanceof Number ? ((Number) c).intValue() : 0;
                    })
                    .findFirst()
                    .orElse(0);

            finalResults.add(
                    new Document("_id", status)
                            .append("count", count)
            );
        }

        return finalResults;
    }

    public Document getSummaryKpis() {

        Aggregation aggregation = Aggregation.newAggregation(

                context -> new Document(
                        "$group",
                        new Document("_id", null)
                                .append("total",
                                        new Document("$sum", 1))
                                .append("new",
                                        new Document("$sum",
                                                new Document("$cond", List.of(
                                                        new Document("$eq", List.of("$status", "NEW")),
                                                        1, 0
                                                ))
                                        )
                                )
                                .append("assigned",
                                        new Document("$sum",
                                                new Document("$cond", List.of(
                                                        new Document("$eq", List.of("$status", "ASSIGNED")),
                                                        1, 0
                                                ))
                                        )
                                )
                                .append("inProgress",
                                        new Document("$sum",
                                                new Document("$cond", List.of(
                                                        new Document("$eq", List.of("$status", "IN_PROGRESS")),
                                                        1, 0
                                                ))
                                        )
                                )
                                .append("pending",
                                        new Document("$sum",
                                                new Document("$cond", List.of(
                                                        new Document("$eq", List.of("$status", "PENDING")),
                                                        1, 0
                                                ))
                                        )
                                )
                                .append("transfer",
                                        new Document("$sum",
                                                new Document("$cond", List.of(
                                                        new Document("$eq", List.of("$status", "TRANSFER")),
                                                        1, 0
                                                ))
                                        )
                                )
                                .append("closed",
                                        new Document("$sum",
                                                new Document("$cond", List.of(
                                                        new Document("$eq", List.of("$status", "CLOSED")),
                                                        1, 0
                                                ))
                                        )
                                )
                                .append("overdue",
                                        new Document("$sum",
                                                new Document("$cond", List.of(
                                                        new Document("$and", List.of(
                                                                new Document("$ne", List.of("$status", "CLOSED")),
                                                                new Document("$ne", Arrays.asList(
                                                                        new Document("$ifNull", Arrays.asList("$dueAt", null)),
                                                                        null
                                                                )),
                                                                new Document("$lt", List.of("$dueAt", "$$NOW"))
                                                        )),
                                                        1, 0
                                                ))
                                        )
                                )
                )
        );

        List<Document> results = mongoTemplate.aggregate(
                aggregation,
                Ticket.class,
                Document.class
        ).getMappedResults();

        return results.isEmpty() ? new Document() : results.get(0);
    }

    public List<Document> getOpenTicketsByPriority() {

        Aggregation aggregation = Aggregation.newAggregation(

                context -> new Document(
                        "$match",
                        new Document("status",
                                new Document("$ne", "CLOSED"))
                ),

                context -> new Document(
                        "$group",
                        new Document("_id", "$priority")
                                .append("count", new Document("$sum", 1))
                )
        );

        List<Document> results = mongoTemplate.aggregate(
                aggregation,
                Ticket.class,
                Document.class
        ).getMappedResults();

        List<String> priorities = mongoTemplate.query(Ticket.class)
                .distinct("priority")
                .as(String.class)
                .all();

        Collections.sort(priorities);

        List<Document> finalResults = new ArrayList<>();

        for (String priority : priorities) {
            if (priority == null || priority.isBlank()) {
                continue;
            }

            int count = results.stream()
                    .filter(item -> priority.equalsIgnoreCase(item.getString("_id")))
                    .mapToInt(item -> {
                        Object c = item.get("count");
                        return c instanceof Number ? ((Number) c).intValue() : 0;
                    })
                    .findFirst()
                    .orElse(0);

            String displayName = priority.length() > 1
                    ? priority.substring(0, 1).toUpperCase() + priority.substring(1).toLowerCase()
                    : priority.toUpperCase();

            finalResults.add(
                    new Document("priority", displayName)
                            .append("count", count)
            );
        }

        return finalResults;
    }

    public List<Document> getOpenTicketsByCategory() {

        Aggregation aggregation = Aggregation.newAggregation(

                context -> new Document(
                        "$match",
                        new Document("status",
                                new Document("$ne", "CLOSED"))
                ),

                context -> new Document(
                        "$group",
                        new Document("_id", "$category")
                                .append("count", new Document("$sum", 1))
                )
        );

        List<Document> results = mongoTemplate.aggregate(
                aggregation,
                Ticket.class,
                Document.class
        ).getMappedResults();

        List<String> categories = mongoTemplate.query(Ticket.class)
                .distinct("category")
                .as(String.class)
                .all();

        Collections.sort(categories);

        List<Document> finalResults = new ArrayList<>();

        for (String category : categories) {
            if (category == null || category.isBlank()) {
                continue;
            }

            int count = results.stream()
                    .filter(item -> category.equalsIgnoreCase(item.getString("_id")))
                    .mapToInt(item -> {
                        Object c = item.get("count");
                        return c instanceof Number ? ((Number) c).intValue() : 0;
                    })
                    .findFirst()
                    .orElse(0);

            String displayName = category.length() > 1
                    ? category.substring(0, 1).toUpperCase() + category.substring(1).toLowerCase()
                    : category.toUpperCase();

            finalResults.add(
                    new Document("category", displayName)
                            .append("count", count)
            );
        }

        return finalResults;
    }

    public List<Document> getLast7DaysSummary() {

        LocalDate today = LocalDate.now(
                ZoneId.of("Asia/Kolkata")
        );

        LocalDate startDate = today.minusDays(6);

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("yyyy-MM-dd");

        List<Document> createdResults = getDailyCounts(
                "createdAt", startDate, today
        );

        List<Document> closedResults = getDailyCounts(
                "closedDate", startDate, today
        );

        Map<String, Integer> createdMap = new HashMap<>();
        Map<String, Integer> closedMap = new HashMap<>();

        for (Document item : createdResults) {
            String id = item.getString("_id");
            Object countObj = item.get("count");
            int count = countObj instanceof Number ? ((Number) countObj).intValue() : 0;
            if (id != null) {
                createdMap.put(id, count);
            }
        }

        for (Document item : closedResults) {
            String id = item.getString("_id");
            Object countObj = item.get("count");
            int count = countObj instanceof Number ? ((Number) countObj).intValue() : 0;
            if (id != null) {
                closedMap.put(id, count);
            }
        }

        List<Document> finalResults = new ArrayList<>();

        for (int i = 0; i < 7; i++) {

            LocalDate date = startDate.plusDays(i);
            String dateKey = date.format(formatter);

            int createdCount = createdMap.getOrDefault(dateKey, 0);
            int closedCount = closedMap.getOrDefault(dateKey, 0);

            finalResults.add(
                    new Document("date", dateKey)
                            .append("count", createdCount)
                            .append("created", createdCount)
                            .append("closed", closedCount)
            );
        }

        return finalResults;
    }

    public long getTotalTicketsLast7Days() {
        return getLast7DaysSummary().stream()
                .mapToLong(doc -> {
                    Object countObj = doc.get("count");
                    return countObj instanceof Number ? ((Number) countObj).longValue() : 0L;
                })
                .sum();
    }

    public Document getLast7DaysDetailedSummary() {
        long totalTickets = getTotalTicketsLast7Days();
        List<Document> daily = getLast7DaysSummary();
        List<Document> recentlyUpdated = getRecentlyUpdatedTickets(7);

        return new Document("totalTicketsLast7Days", totalTickets)
                .append("dailyCounts", daily)
                .append("recentlyUpdatedTickets", recentlyUpdated);
    }

    public List<Document> getRecentlyUpdatedTickets(int limit) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        LocalDate startDate = today.minusDays(6);

        Date startInstant = Date.from(startDate.atStartOfDay(ZoneOffset.UTC).toInstant());
        Date endInstant = Date.from(today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());

        Aggregation aggregation = Aggregation.newAggregation(
                context -> new Document(
                        "$addFields",
                        new Document("effectiveUpdatedAt",
                                new Document("$ifNull", List.of("$updatedAt", "$lastModifiedDate"))
                        )
                ),
                context -> new Document(
                        "$match",
                        new Document("effectiveUpdatedAt",
                                new Document("$gte", startInstant)
                                        .append("$lt", endInstant)
                        )
                ),
                context -> new Document(
                        "$sort",
                        new Document("effectiveUpdatedAt", -1)
                ),
                context -> new Document(
                        "$limit",
                        limit > 0 ? limit : 7
                )
        );

        List<Document> rawResults = mongoTemplate.aggregate(
                aggregation,
                Ticket.class,
                Document.class
        ).getMappedResults();

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        List<Document> formattedTickets = new ArrayList<>();

        for (Document doc : rawResults) {
            String id = doc.getString("_id") != null ? doc.getString("_id") : doc.getString("id");
            String title = doc.getString("title");
            String priority = doc.getString("priority");
            String status = doc.getString("status");
            String agent = doc.getString("agent");

            Date d = doc.get("effectiveUpdatedAt", Date.class);
            if (d == null) {
                d = doc.get("lastModifiedDate", Date.class);
            }
            if (d == null) {
                d = doc.get("updatedAt", Date.class);
            }

            String updatedAtStr = "";
            String updatedAgoStr = "";

            if (d != null) {
                LocalDateTime ticketTime = LocalDateTime.ofInstant(d.toInstant(), ZoneOffset.UTC);
                updatedAtStr = ticketTime.format(formatter);
                updatedAgoStr = calculateRelativeTime(ticketTime, now);
            }

            Document ticketDoc = new Document();
            ticketDoc.append("id", id != null ? id : "");
            ticketDoc.append("title", title != null ? title : "");
            ticketDoc.append("priority", priority != null ? priority : "");
            ticketDoc.append("status", status != null ? status : "");
            ticketDoc.append("agent", agent != null ? agent : "");
            ticketDoc.append("updatedAt", updatedAtStr);
            ticketDoc.append("updatedAgo", updatedAgoStr);

            formattedTickets.add(ticketDoc);
        }

        return formattedTickets;
    }

    public String calculateRelativeTime(LocalDateTime updateTime, LocalDateTime now) {
        if (updateTime == null) {
            return "";
        }
        Duration duration = Duration.between(updateTime, now);
        if (duration.isNegative() || duration.getSeconds() < 60) {
            return "Just now";
        }

        long minutes = duration.toMinutes();
        if (minutes < 60) {
            return minutes == 1 ? "1 minute ago" : minutes + " minutes ago";
        }

        long hours = duration.toHours();
        if (hours < 24) {
            return hours == 1 ? "1 hour ago" : hours + " hours ago";
        }

        long days = duration.toDays();
        if (days < 7) {
            return days == 1 ? "1 day ago" : days + " days ago";
        }

        long weeks = days / 7;
        return weeks == 1 ? "1 week ago" : weeks + " weeks ago";
    }

    public Document getRecentlyUpdatedTicketsSummary(int limit) {
        long totalTickets = getTotalTicketsLast7Days();
        List<Document> recentlyUpdated = getRecentlyUpdatedTickets(limit > 0 ? limit : 7);

        return new Document("totalTicketsLast7Days", totalTickets)
                .append("recentlyUpdatedTickets", recentlyUpdated);
    }

    private List<Document> getDailyCounts(
            String dateField,
            LocalDate startDate,
            LocalDate endDate) {

        List<String> fallbackFields;
        if ("createdAt".equalsIgnoreCase(dateField) || "createdDate".equalsIgnoreCase(dateField)) {
            fallbackFields = List.of("$createdAt", "$createdDate");
        } else if ("closedAt".equalsIgnoreCase(dateField) || "closedDate".equalsIgnoreCase(dateField)) {
            fallbackFields = List.of("$closedAt", "$closedDate");
        } else {
            fallbackFields = List.of("$" + dateField);
        }

        Date startInstant = Date.from(startDate.atStartOfDay(ZoneOffset.UTC).toInstant());
        Date endInstant = Date.from(endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());

        Aggregation aggregation = Aggregation.newAggregation(
                context -> new Document(
                        "$addFields",
                        new Document("effectiveDate",
                                new Document("$ifNull", fallbackFields)
                        )
                ),
                context -> new Document(
                        "$match",
                        new Document("effectiveDate",
                                new Document("$gte", startInstant)
                                        .append("$lt", endInstant)
                        )
                ),
                context -> new Document(
                        "$group",
                        new Document(
                                "_id",
                                new Document("$dateToString",
                                        new Document("format", "%Y-%m-%d")
                                                .append("date", "$effectiveDate")
                                                .append("timezone", "Asia/Kolkata")
                                )
                        ).append("count", new Document("$sum", 1))
                )
        );

        return mongoTemplate.aggregate(
                aggregation,
                Ticket.class,
                Document.class
        ).getMappedResults();
    }

    public Document getCompleteSummary() {

        Document kpis = getSummaryKpis();

        if (kpis != null) {
            kpis.remove("_id");
        } else {
            kpis = new Document();
        }

        List<Document> last7Days = getLast7DaysSummary();
        long totalTicketsLast7Days = last7Days.stream()
                .mapToLong(doc -> {
                    Object countObj = doc.get("count");
                    return countObj instanceof Number ? ((Number) countObj).longValue() : 0L;
                })
                .sum();

        List<Document> recentlyUpdated = getRecentlyUpdatedTickets(7);

        Document response = new Document();

        response.append("kpis", kpis);
        response.append("openByPriority", getOpenTicketsByPriority());
        response.append("openByCategory", getOpenTicketsByCategory());
        response.append("last7Days", last7Days);
        response.append("totalTicketsLast7Days", totalTicketsLast7Days);
        response.append("recentlyUpdatedTickets", recentlyUpdated);

        return response;
    }
}
