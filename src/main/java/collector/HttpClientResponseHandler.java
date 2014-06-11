package collector;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.entity.ContentType;
import org.apache.tika.io.IOUtils;

import java.io.IOException;
import java.io.StringWriter;

/**
 * Created by alejandrofernandez on 6/9/14.
 */
public class HttpClientResponseHandler implements ResponseHandler {


    private StatusLine statusLine;
    private String content;

    @Override
    public Object handleResponse(HttpResponse httpResponse) throws IOException {
        statusLine = null;
        content = "";
        statusLine = httpResponse.getStatusLine();
        HttpEntity entity = httpResponse.getEntity();

        if (statusLine.getStatusCode() == 200) {

            ContentType contentType = ContentType.getOrDefault(entity);
            if (!contentType.getMimeType().equals(ContentType.TEXT_HTML.getMimeType())) {
                throw new ClientProtocolException("Unexpected content type:" +
                        contentType);
            }
            StringWriter writer = new StringWriter();
            IOUtils.copy(entity.getContent(), writer, "UTF-8");
            content = writer.toString();

            return content;
        }
        return null;
    }

    public StatusLine getStatusLine() {
        return statusLine;
    }

    public String getContent() {
        return content;
    }
}
