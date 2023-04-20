/*
 * Questa classe, derivata da HttpServletResponseWrapper, estende la normale HttpServletResponse
 * per bufferizzare l'output invece di inviarlo direttamente al client.
 * 
 * This class, derived from HttpServletResponseWrapper, extends the standard HttpServletResponse
 * to buffer the output before sending it to the client.
 *
 */
package it.univaq.f4i.iw.framework.result.filter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 *
 * @author Giuseppe Della Penna
 */
public class OutputBufferingHttpServletResponse extends HttpServletResponseWrapper {

    //il buffer per l'output 
    //the output buffer
    private final ByteArrayOutputStream output_buffer;

    //determiniamo il tipo di output da bufferizzare
    //determine the kind of output to buffer
    private final boolean buffer_binary;
    private final boolean buffer_text;

    //indica se e quale tipo di stream è stato restituito da questa response
    //indicates if a stream has been returned by this response, and its type
    public enum StreamType {
        NONE, TEXT, BINARY;
    }
    private StreamType returnedStreamType = StreamType.NONE;

    //la nostra response è un wrapper attorno alla response originale
    //che delega tutto il lavoro a quest'ultima, ad eccezione della getWriter()
    //our response is a wrapper around the original response 
    //that delegates all work to the latter except for getWriter()
    public OutputBufferingHttpServletResponse(HttpServletResponse response, boolean buffer_text, boolean buffer_binary) {
        super(response);
        output_buffer = new ByteArrayOutputStream(response.getBufferSize());
        this.buffer_binary = buffer_binary;
        this.buffer_text = buffer_text;
    }

    public OutputBufferingHttpServletResponse(HttpServletResponse response) {
        super(response);
        output_buffer = new ByteArrayOutputStream(response.getBufferSize());
        this.buffer_binary = false;
        this.buffer_text = true;
    }

    ///Metodi dell'interfaccia HttpServletResponse
    //Tutti i metodi non sovrascritti sono chiamati sulla HttpServletResponse (originale)
    //passata al costruttore
    //HttpServletResponse interface methods.
    //All the methods not overridden here are redirected to the (original) HttpServletResponse
    //passed to the constructor
    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (returnedStreamType == StreamType.NONE) {
            returnedStreamType = StreamType.BINARY;
            ServletOutputStream output_stream;
            //l'output binario non viene mandato direttamente al client (non bufferizzato), a meno che non sia esplicitmente richiesto
            //binary output is sent directly to the client (not buffered), if not explicitly requested
            if (!buffer_binary) {
                output_stream = super.getOutputStream();
            } else {
                //per l'output binario restituiamo un ServletOutputStream basato sul buffer locale
                //for binary output, we return a locally-buffered  ServletOutputStream
                output_stream = new ServletOutputStream() {
                    @Override
                    public boolean isReady() {
                        return true;
                    }

                    @Override
                    public void setWriteListener(WriteListener writeListener) {
                        //not implemented
                    }

                    @Override
                    public void write(int b) throws IOException {
                        output_buffer.write(b);
                    }
                };
            }
            return output_stream;
        } else {
            throw new IOException("Output channel already created");
        }
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (returnedStreamType == StreamType.NONE) {
            returnedStreamType = StreamType.TEXT;
            PrintWriter output_writer;
            //l'output testuale non viene mandato direttamente al client (non bufferizzato), a meno che non sia esplicitmente richiesto
            //text output is sent directly to the client (not buffered), if not explicitly requested
            if (!buffer_text) {
                output_writer = super.getWriter();
            } else {
                //per l'output testuale, restituiamo un PrintWriter basato sul buffer locale
                //for text output, we return a locally-buffered PrintWriter
                output_writer = new PrintWriter(new OutputStreamWriter(output_buffer, super.getCharacterEncoding()));
            }
            return output_writer;
        } else {
            throw new IOException("Output channel already created");
        }
    }

    public StreamType getReturnedStreamType() {
        return returnedStreamType;
    }

    public String getString() throws IOException {
        output_buffer.flush();
        return output_buffer.toString(super.getCharacterEncoding());
    }

    public byte[] getBytes() throws IOException {
        output_buffer.flush();
        return output_buffer.toByteArray();
    }
}
