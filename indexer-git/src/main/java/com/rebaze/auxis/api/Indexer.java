package com.rebaze.auxis.api;

import java.net.URL;

/**
 * Created by tonit on 01.05.17.
 */
public interface Indexer {
    void index(URL path) throws Exception;
}
