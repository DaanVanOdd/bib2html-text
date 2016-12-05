package client.model;

import java.util.Collection;

/**
 * @author Maximilian Schirm (denkbares GmbH)
 * @created 05.12.2016
 *
 * Identifies an Entry precisely
 */

public class EntryIdentifier {

    private final String clientID, bibFileId;
    private final Collection<String> cslFileIds, templateIds;
    private boolean hasErrors = false;

    public EntryIdentifier(String clientID, String bibFileId, Collection<String> cslFileIds, Collection<String> templateIds) {
        this.clientID = clientID;
        this.bibFileId = bibFileId;
        this.cslFileIds = cslFileIds;
        this.templateIds = templateIds;
    }

    public boolean isHasErrors() {
        return hasErrors;
    }

    public void setHasErrors(boolean hasErrors) {
        this.hasErrors = hasErrors;
    }

    public String getClientID() {
        return clientID;
    }

    public String getBibFileId() {
        return bibFileId;
    }

    public Collection<String> getCslFileId() {
        return cslFileIds;
    }

    public Collection<String> getTemplateId() {
        return templateIds;
    }
}