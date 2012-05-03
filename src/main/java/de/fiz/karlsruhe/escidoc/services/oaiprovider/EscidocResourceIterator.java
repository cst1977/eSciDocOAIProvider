package de.fiz.karlsruhe.escidoc.services.oaiprovider;

import org.apache.log4j.Logger;

import proai.driver.RemoteIterator;
import proai.error.RepositoryException;

public class EscidocResourceIterator
    implements RemoteIterator<EscidocRecord> {

    private static final Logger logger =
        Logger.getLogger(EscidocResourceIterator.class.getName());

    private RecordsReader m_reader;

    private String m_nextLine;

    private final EscidocMetadataFormat format;

    public EscidocResourceIterator(EscidocMetadataFormat format,
        RecordsReader reader) {
        this.m_reader = reader;
        this.format = format;
        m_nextLine = m_reader.readLine();
    }

    public boolean hasNext() {
        return (m_nextLine != null);
    }

    public EscidocRecord next() throws RepositoryException {
        try {
            return getRecord(m_nextLine);
        }
        finally {
            if (m_nextLine != null)
                m_nextLine = m_reader.readLine();
        }
    }

    public void close() {
        this.m_reader.close();
    }

    /**
     * Ensure resources are freed up at garbage collection time.
     */
    protected void finalize() {
        close();
    }

    /**
     * Construct a record given a line from the reader. 
     */
    private EscidocRecord getRecord(String line) throws RepositoryException {

        logger.debug("Constructing record from search queries result line: "
            + line);

        String oai_id = null;
        String resourceId = null;
        String releaseDate = null;
        String date = null;
        String resourceType = null;
        boolean deleted = false;
        String recordDissemination = format.getDissemination();
        String setSpecs = null;
        String[] specs = null;

        String[] parts = line.split(" ");
        // parse the line into values for constructing an EscidocRecord
        try {
            if (parts.length < 6) {
                throw new Exception(
                    "Expected at least 6 space-separated values");
            }
            oai_id = parts[0];
            resourceId = parts[1];
            date = parts[2];
            releaseDate = parts[3];

            String _deleted = parts[4];
            if (_deleted.equals("true")) {
                deleted = true;
            }
            resourceType = parts[5];

            if (parts.length == 7) {
                setSpecs = parts[6];
            }
            specs = setSpecs.split(",");
        }
        catch (Exception e) {
            throw new RepositoryException("Error parsing search queries "
                + "results: " + e.getMessage() + ".  Input "
                + "line was: " + line, e);
        }

        // if we got here, all the parameters were parsed correctly
        return new EscidocRecord(oai_id, resourceId, format.getPrefix(),
            recordDissemination, date, deleted, specs, releaseDate,
            resourceType);
    }

    public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("remove() not supported");
    }

}