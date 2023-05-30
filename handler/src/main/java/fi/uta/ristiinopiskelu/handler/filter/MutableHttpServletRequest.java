package fi.uta.ristiinopiskelu.handler.filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.*;

public class MutableHttpServletRequest extends HttpServletRequestWrapper {

    private final Map<String, String> headers;

    public MutableHttpServletRequest(HttpServletRequest request) {
        super(request);
        headers = new HashMap<>();
    }

    public void putHeader(String name, String value) {
        headers.put(name, value);
    }

    @Override
    public String getHeader(String name) {
        String headerValue = headers.get(name);

        if (headerValue != null){
            return headerValue;
        }

        return ((HttpServletRequest) getRequest()).getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        if(headers.get(name) != null) {
            return Collections.enumeration(Collections.singletonList(headers.get(name)));
        }
        return super.getHeaders(name);
    }


    @Override
    public Enumeration<String> getHeaderNames() {
        Set<String> set = new HashSet<>(headers.keySet());

        Enumeration<String> e = ((HttpServletRequest) getRequest()).getHeaderNames();
        while (e.hasMoreElements()) {
            String n = e.nextElement();
            set.add(n);
        }

        return Collections.enumeration(set);
    }
}
