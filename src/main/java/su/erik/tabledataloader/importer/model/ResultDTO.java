package su.erik.tabledataloader.importer.model;

import java.io.Serializable;

public record ResultDTO(Long uploadId, long count) implements Serializable {
}
