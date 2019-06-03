package net.demelor.accounting.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import spark.ResponseTransformer;

public class JsonTransformer implements ResponseTransformer {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String render(Object o) throws Exception {
        return mapper.writeValueAsString(o);
    }
}
