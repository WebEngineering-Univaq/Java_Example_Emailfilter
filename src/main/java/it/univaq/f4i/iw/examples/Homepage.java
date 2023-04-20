/*
 * Homepage.java
 *
 * Questo esempio mostra come usare i filtri per modificare al volo l'output delle
 * servlet 
 *
 * This example shows how filters can be used to modify on-the-fly the servlet output
 * 
 */
package it.univaq.f4i.iw.examples;

import it.univaq.f4i.iw.framework.result.HTMLResult;
import it.univaq.f4i.iw.framework.utils.ServletHelpers;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Ingegneria del Web
 */
public class Homepage extends HttpServlet {

    private void action_default(HttpServletRequest request, HttpServletResponse response) throws IOException {
        //true è il valore di default. Impostando questo attribuito della request a false disabilitiamo il filtraggio delle email        
        //true is the default. Setting this request attribute to false disables email filtering
        request.setAttribute("filter.email.enable", true);
        HTMLResult result = new HTMLResult(getServletContext());
        result.setTitle("Example page");
        result.appendToBody("<p>Hello!</p>");
        result.appendToBody("<p>My email address is pinco.pallino@univaq.it or pinco.pallino@di.univaq.it, and NOT pincopallino@a.b.c.d.com.</p>");
        result.appendToBody("<p>This text @.,.,. is not modified!.</p>");
        result.activate(request, response);
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException {
        try {
            action_default(request, response);
        } catch (Exception ex) {
            request.setAttribute("exception", ex);
            ServletHelpers.handleError(request, response, getServletContext());
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     */
    public String getServletInfo() {
        return "Response email filtering";
    }
    // </editor-fold>
}
