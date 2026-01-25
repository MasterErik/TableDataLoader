package su.erik.tabledataloader.importer.model;

import java.time.LocalDate;

public class UploadDTO {
    private Long id;
    private final String name;
    private final Long userId;
    private final LocalDate date;
    private final Long size;
    private final String entity;

    public UploadDTO(String name, Long userId, LocalDate date, Long size, String entity) {
        this.name = name;
        this.userId = userId;
        this.date = date;
        this.size = size;
        this.entity = entity;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public Long getUserId() { return userId; }
    public LocalDate getDate() { return date; }
    public Long getSize() { return size; }
    public String getEntity() { return entity; }
}
