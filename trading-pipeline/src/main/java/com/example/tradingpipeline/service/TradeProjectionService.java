package com.example.tradingpipeline.service;

import com.example.tradingpipeline.config.PipelineProperties;
import com.example.tradingpipeline.model.TradeDocument;
import com.example.tradingpipeline.model.TradeEvent;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.model.BulkWriteOptions;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static org.springframework.data.domain.Sort.Direction.ASC;

@Service
public class TradeProjectionService implements InitializingBean {
    private final MongoTemplate mongoTemplate;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final PipelineProperties properties;

    public TradeProjectionService(
            MongoTemplate mongoTemplate,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            PipelineProperties properties) {
        this.mongoTemplate = mongoTemplate;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        mongoTemplate.indexOps(properties.mongoCollection())
                .createIndex(new Index().on("securityId", ASC).on("tradeDate", ASC).on("source", ASC).unique());
    }

    public void persist(List<TradeEvent> events) {
        var mongoWrites = events.stream()
                .map(event -> (WriteModel<Document>) new ReplaceOneModel<>(
                        and(
                                eq("securityId", event.securityId()),
                                eq("tradeDate", event.tradeDate().toString()),
                                eq("source", event.source())),
                        toMongoDocument(event),
                        new ReplaceOptions().upsert(true)))
                .toList();

        var redisWrites = events.stream()
                .map(event -> new RedisWrite(redisKey(event), toJson(event)))
                .toList();

        if (!mongoWrites.isEmpty()) {
                mongoTemplate.getCollection(properties.mongoCollection())
                    .bulkWrite(mongoWrites, new BulkWriteOptions().ordered(false));
        }

        if (!redisWrites.isEmpty()) {
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (var write : redisWrites) {
                    connection.stringCommands().set(
                            write.key().getBytes(StandardCharsets.UTF_8),
                            write.value().getBytes(StandardCharsets.UTF_8));
                }
                return null;
            });
        }
    }

    public List<TradeDocument> findTrades(String securityId, LocalDate from, LocalDate to) {
        var criteria = Criteria.where("securityId").is(securityId);
        if (from != null) {
            criteria.and("tradeDate").gte(from.toString());
        }
        if (to != null) {
            criteria.and("tradeDate").lte(to.toString());
        }
        var query = Query.query(criteria).with(Sort.by(ASC, "tradeDate", "source"));
        return mongoTemplate.find(query, TradeDocument.class, properties.mongoCollection());
    }

    private Document toMongoDocument(TradeEvent event) {
        return new Document("_id", event.key())
                .append("eventId", event.eventId())
                .append("securityId", event.securityId())
                .append("tradeDate", event.tradeDate().toString())
                .append("source", event.source())
                .append("price", event.price())
                .append("quantity", event.quantity())
                .append("eventTime", event.eventTime().toString());
    }

    private String toJson(TradeEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Unable to serialize trade event " + event.eventId(), exception);
        }
    }

    private String redisKey(TradeEvent event) {
        return "trade:" + event.key();
    }

    private record RedisWrite(String key, String value) {
    }
}