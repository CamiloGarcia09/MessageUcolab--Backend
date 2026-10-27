package co.edu.uco.application.secondaryports.entity;

import java.time.Instant;
import java.util.UUID;

public record MessageAvailabilityData(UUID messageId, Instant updatedAt) {
}
