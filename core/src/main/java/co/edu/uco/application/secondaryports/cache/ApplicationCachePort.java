package co.edu.uco.application.secondaryports.cache;

import java.util.List;
import java.util.UUID;

public interface ApplicationCachePort {
    void evict(UUID applicationId, List<UUID> messageIds);
}
