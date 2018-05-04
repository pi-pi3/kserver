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

    public Response(Object response) {
        boolean isEmpty = response == null;
        if (!isEmpty && response instanceof Collection<?>) {
            isEmpty = ((Collection<?>) response).isEmpty();
        }
        this.init(!isEmpty, response);
    }

    public Response(boolean success, Object response) {
        this.init(success, response);
    }

    private void init(boolean success, Object response) {
        this.success = success;
        if (success && response != null) {
            if (response instanceof Collection<?>) {
                Collection<?> values = (Collection<?>) response;
                this.response = values.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(","));
            } else {
                this.response = response.toString();
            }
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
