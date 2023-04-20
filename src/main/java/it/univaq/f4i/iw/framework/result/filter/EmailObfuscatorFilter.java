/*
 * Questa classe implementa un Servlet Filter, cioè una classe che intercetta
 * e gestisce il traffico di dati in entrata e/o in uscita dalle servlet.
 * Perchè funzioni, deve essere configurata tramite il file web.xml.
 * Questo filtro sostituisce la response che arriverà alla servlet
 * con una OutputBufferingHttpServletResponse che permette di catturare l'output
 * generato dalle servlet ed elaborarlo prima di inviarlo al client.
 * Attenzione: i filtri sono chiamati anche se viene richiesta una risorsa statica (ad esempio un file html)
 * contenuta nella web application. In tal caso, infatti, la risorsa viene servita da una servlet interna
 * (DefaultServlet nel caso di Tomcat). Tuttavia, in genere i contenuti statici non sono restituiti come
 * testuali, ma sempre come binari (DefaultServlet chiama getOutputStream)
 *
 * This class implements a Servlet Filter, i.e., an object that intercepts
 * and handles the data from and/or to the servlet.
 * To make it work, the filter must be declared and configured through the web.xml file.
 * This filter substitutes the response object with a OutputBufferingHttpServletResponse,
 * which allows to capture the servlet output and alter it before sending it to the client
 * Warning: filters are called even if a static resource (e.g., an html file) 
 * contained in the web application is requested. In this case, the resource is served 
 * by an internal servlet (DefaultServlet for Tomcat.) However, static content is typically 
 * not returned as text, but always as binary (DefaultServlet calls getOutputStream)
 *
 */
package it.univaq.f4i.iw.framework.result.filter;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class EmailObfuscatorFilter implements Filter {

    private FilterConfig config = null;
    //Espressione regolare per il riconoscimento degli indirizzi email
    //Regular expression to find email addresses
    private static final String EMAIL_REGEXP = "[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*\\.[A-Za-z]{2,}";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEXP);
    //Espressione regolare per il riconoscimento delle *risorse statiche* a cui questo filtro va aapplicato
    //Regular expression used to detect the *static resources* this filter must be applied to
    private static final String STATIC_RESOURCE_REGEXP = "\\.html$";
    private static final Pattern STATIC_RESOURCE_PATTERN = Pattern.compile(STATIC_RESOURCE_REGEXP);
    //
    private static final String EMAIL_AT_REPLACEMENT = "[AT]";
    private static final String EMAIL_DOT_REPLACEMENT = "[DOT]";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        //questa è la response connessa al browser, che normalmente verrebbe passata alla servlet
        //this is the browser-connected response that would be normally passed to the servlet
        HttpServletResponse original_response = (HttpServletResponse) response;

        //questa è la nostra classe HTTPServletResponse-compatibile che usiamo per catturare l'output della servlet
        //this is our HTTPServletResponse-compatible class we use to capture the servlet output
        OutputBufferingHttpServletResponse local_response;

        //determiniamo se la richiesta è per una risorsa testuale statica
        //determine if the request is for a static text resource        
        Matcher matcher = STATIC_RESOURCE_PATTERN.matcher(((HttpServletRequest) request).getServletPath());
        boolean is_static_resource_to_filter = matcher.find();
        if (is_static_resource_to_filter) {
            //per bufferizzare e filtrare il contenuto di una risorsa statica dobbiamo catturare anche l'output binario
            //to buffer the content of a static resource, we must capture also the binary output
            local_response = new OutputBufferingHttpServletResponse(original_response, true, true);
        } else {
            //altrimenti bufferizziamo solo l'output testuale e inviamo direttamente al client quello binario
            //otherwise our response will buffer only textual output and send directly to the client any binary output
            local_response = new OutputBufferingHttpServletResponse(original_response, true, false);
        }

        //continuiamo l'elaborazione della catena di filtri REQUEST passando l'oggetto OutputBufferingHttpServletResponse come response
        //l'ultimo passo della catena dei filtri sarà la servlet vera e propria, quindi al ritorno da doFilter nella response avremo
        //l'output dell'elaborazione
        //continue processing the REQUEST filter chain passing the OutputBufferingHttpServletResponse object as response
        //the last step of the filter chain will be the actual servlet, so doFilter returns the response will contain 
        //the processing output
        chain.doFilter(request, local_response);

        //A questo punto l'output, se catturato, non è stato ancora mandato al client, ma è bufferizzato nella
        //OutputBufferingHttpServletResponse, quindi possiamo manipolarlo prima di inviarlo al client
        //At this point the output, if captured, has not been sent to the client, but has been buffered in the 
        //OutputBufferingHttpServletResponse, so we can manipulate it before sending it to the client.        
        if ((!is_static_resource_to_filter && local_response.getReturnedStreamType() == OutputBufferingHttpServletResponse.StreamType.TEXT)
                || (is_static_resource_to_filter && local_response.getReturnedStreamType() != OutputBufferingHttpServletResponse.StreamType.NONE)) {
            String processed_text;
            //request flag che permette di disabilitare il filtro (da testare DOPO aver chiamato doFilter)
            //request flag that allows to disable the filter (test it AFTER calling doFilter)
            if (request.getAttribute("filter.email.enable") != null && (Boolean) request.getAttribute("filter.email.enable") == false) {
                processed_text = local_response.getString();
            } else {
                processed_text = postProcessEmailAddresses(local_response.getString());
            }
            //inviamo il contenuto (filtrato) al browser usando l'oggetto response passatoci originariamente
            //send the (filtered) content to the browser using the response object originally passed to the filter
            //reimpostiamo la content-length che potrebbe essere stata modificata dalle nostre manipolazioni
            //reset the content-length that may have changed due to our manipulations
            original_response.setContentLength(processed_text.length());
            if (!is_static_resource_to_filter) {
                original_response.getWriter().print(processed_text);
            } else {
                original_response.getOutputStream().write(processed_text.getBytes(response.getCharacterEncoding()));
            }
        }

    }

    private String postProcessEmailAddresses(String original_text) {
        StringBuffer processed_text = new StringBuffer();
        Matcher email_matcher = EMAIL_PATTERN.matcher(original_text);
        while (email_matcher.find()) {
            String mapped_email = email_matcher.group(0);
            email_matcher.appendReplacement(processed_text, mapped_email.replaceAll("@", EMAIL_AT_REPLACEMENT).replaceAll("\\.", EMAIL_DOT_REPLACEMENT));
        }
        email_matcher.appendTail(processed_text);
        return processed_text.toString();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.config = filterConfig;
    }

    @Override
    public void destroy() {
        config = null;
    }
}
