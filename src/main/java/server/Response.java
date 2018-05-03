package server;

import java.util.Collection;
import java.util.stream.Collectors;

public class Response {
    public static final Response FAILURE = new Response(false, null);

    private boolean success;
    private String response = null;

    public Response(boolean success) {
        this(success, null);
    }

    public Response(Collection<?> response) {
        this(response != null && !response.isEmpty(), response);
    }

    public Response(boolean success, Collection<?> response) {
        this.success = success;
        if (success && response != null) {
            this.response = response.stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));
        }
    }

    public Response(boolean success, Object response) {
        this.success = success;
        if (success && response != null) {
            this.response = response.toString();
        }
    }

    public boolean isSuccess() {
        return this.success;
    }

    public String getResponse() {
        return this.response;
    }

    public String toString() {
        StringBuilder str = new StringBuilder();

        if (this.success) {
            str.append('1');
            if (this.response != null) {
                str.append(' ');
                str.append(this.response);
            }
        } else {
            str.append('0');
        }
        str.append('\n');

        return str.toString();
    }
}
