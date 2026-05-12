package dev.lucasschottler.update;

import java.net.http.HttpResponse;

public abstract class AbstractMarketplaceProcessor {

    abstract void refreshToken();
    
    abstract boolean doRequest();

    abstract boolean updateItem();

    abstract HttpResponse<String> doRequestForResponse();

}
