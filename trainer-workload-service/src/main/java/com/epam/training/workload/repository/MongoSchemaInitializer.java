package com.epam.training.workload.repository;

import com.epam.training.workload.model.TrainerSummary;
import jakarta.annotation.PostConstruct;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.schema.JsonSchemaObject;
import org.springframework.data.mongodb.core.schema.JsonSchemaProperty;
import org.springframework.data.mongodb.core.schema.MongoJsonSchema;
import org.springframework.stereotype.Component;

@Component
public class MongoSchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(MongoSchemaInitializer.class);

    private final MongoTemplate mongoTemplate;

    public MongoSchemaInitializer(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    public void applySchema() {
        MongoJsonSchema schema = schema();
        if (mongoTemplate.collectionExists(TrainerSummary.class)) {
            mongoTemplate.executeCommand(new Document("collMod", TrainerSummary.COLLECTION)
                    .append("validator", schema.toDocument()));
            log.info("Updated JSON schema validator of collection '{}'", TrainerSummary.COLLECTION);
        } else {
            mongoTemplate.createCollection(TrainerSummary.class, CollectionOptions.empty().schema(schema));
            log.info("Created collection '{}' with JSON schema validator", TrainerSummary.COLLECTION);
        }
    }

    static MongoJsonSchema schema() {
        JsonSchemaObject month = JsonSchemaObject.object()
                .required("month", "trainingSummaryDuration")
                .properties(
                        JsonSchemaProperty.int32("month").gte(1).lte(12),
                        JsonSchemaProperty.number("trainingSummaryDuration").gte(0));
        JsonSchemaObject year = JsonSchemaObject.object()
                .required("year", "months")
                .properties(
                        JsonSchemaProperty.int32("year").gte(1),
                        JsonSchemaProperty.array("months").items(month));
        return MongoJsonSchema.builder()
                .required("trainerUsername", "trainerFirstName", "trainerLastName", "trainerStatus", "years")
                .properties(
                        JsonSchemaProperty.string("trainerUsername").minLength(1),
                        JsonSchemaProperty.string("trainerFirstName").minLength(1),
                        JsonSchemaProperty.string("trainerLastName").minLength(1),
                        JsonSchemaProperty.bool("trainerStatus"),
                        JsonSchemaProperty.array("years").items(year))
                .build();
    }
}
