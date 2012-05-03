package de.fiz.karlsruhe.escidoc.services.oaiprovider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Reads lines from a file, containing a result of search queries, into an
 * iterator, which can be used to build EscidocRecords.
 * 
 * @author ROF
 * 
 */

public class RecordsReader {

    private File m_f;

    private boolean m_deleteOnClose;

    private BufferedReader m_r;

    /**
     * Initialize with a BufferedReader containing the input data.
     * 
     * @param r
     *            The result of the search queries. Must be provided.
     * 
     */
    public RecordsReader(BufferedReader r) {
        m_r = r;

    }

    /**
     * Initialize with a File containing the input data, optionally deleting
     * them when the RecordsReader is closed.
     * 
     * @param f
     *            The result of the search queries. Must be provided.
     * 
     * @param deleteOnClose
     *            Whether to delete a provided file when this RecordsReader is
     *            closed.
     */
    public RecordsReader(File f, boolean deleteOnClose)
        throws FileNotFoundException {
        m_f = f;
        m_deleteOnClose = deleteOnClose;
        m_r =
            new BufferedReader(new InputStreamReader(new FileInputStream(m_f)));

    }

    /**
     * Get the next line of output, or null if we've reached the end.
     */
    public String readLine() {
        String line = nextLine(m_r);
        if (line == null) {
            close();
            return null;
        }
        else {
            return line;
        }
    }

    /**
     * Get the next line, skipping any that are blank.
     */
    private static String nextLine(BufferedReader r) {
        try {
            String line = r.readLine();
            while (line != null && (line.trim().equals(""))) {
                line = r.readLine();
            }
            if (line == null) {
                return null;
            }
            else {
                return line.trim();
            }
        }
        catch (IOException e) {
            System.err.println("WARNING: " + e.getMessage());
            return null;
        }
    }

    /**
     * Close the input readers and delete the associated files if the combiner
     * was constructed with the option to do so.
     */
    public void close() {
        try {
            m_r.close();
        }
        catch (Throwable th) {
        }
        if (m_deleteOnClose) {
            m_f.delete();
        }
    }

    /**
     * Ensure resources are freed up at garbage collection time.
     */
    protected void finalize() {
        close();
    }

}
