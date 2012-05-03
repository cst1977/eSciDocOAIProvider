package de.fiz.karlsruhe.escidoc.services.oaiprovider;

import java.util.Iterator;
import java.util.Vector;

import proai.SetInfo;
import proai.driver.RemoteIterator;
import proai.error.RepositoryException;

public class EscidocSetInfoIterator
        implements RemoteIterator<SetInfo> {

    private Iterator<SetInfo> m_setsIterator;
    private SetInfo m_next;

    /**
     * Initialize empty.
     */
    public EscidocSetInfoIterator(Vector<SetInfo> sets) {
        this.m_setsIterator = sets.iterator();
    }

   
    public boolean hasNext() throws RepositoryException {
        return (this.m_setsIterator.hasNext());
    }

    public SetInfo next() throws RepositoryException {
        
        if (this.m_setsIterator.hasNext()) {
            return this.m_setsIterator.next();
        } else {
            throw new RepositoryException("No more results available\n");
        }
    }

    public void close() throws RepositoryException {
        //do nothing
    }

    public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("FedoraSetInfoIterator does not support remove().");
    }

}
