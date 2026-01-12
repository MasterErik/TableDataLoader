package su.erik.tabledataloader.dto;

public enum LoaderHttpStatus {

    OK(200, Series.SUCCESSFUL),
    CREATED(201, Series.SUCCESSFUL),
    ACCEPTED(202, Series.SUCCESSFUL),
    NO_CONTENT(204, Series.SUCCESSFUL),

    BAD_REQUEST(400, Series.CLIENT_ERROR),
    UNAUTHORIZED(401, Series.CLIENT_ERROR),
    FORBIDDEN(403, Series.CLIENT_ERROR),
    NOT_FOUND(404, Series.CLIENT_ERROR),

    INTERNAL_SERVER_ERROR(500, Series.SERVER_ERROR);

    private final int value;
    private final Series series;

    LoaderHttpStatus(int value, Series series) {
        this.value = value;
        this.series = series;
    }

    public int value() {
        return this.value;
    }

    public Series series() {
        return this.series;
    }

    public enum Series {
        INFORMATIONAL(1),
        SUCCESSFUL(2),
        REDIRECTION(3),
        CLIENT_ERROR(4),
        SERVER_ERROR(5);

        private final int value;

        Series(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }
    }
}
