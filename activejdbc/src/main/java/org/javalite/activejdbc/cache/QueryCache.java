/*
Copyright 2009-2014 Igor Polevoy

Licensed under the Apache License, Version 2.0 (the "License"); 
you may not use this file except in compliance with the License. 
You may obtain a copy of the License at 

http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software 
distributed under the License is distributed on an "AS IS" BASIS, 
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
See the License for the specific language governing permissions and 
limitations under the License. 
*/


package org.javalite.activejdbc.cache;


import org.javalite.activejdbc.LogFilter;
import org.javalite.activejdbc.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static org.javalite.common.Util.*;

/**
 * This is a main cache facade. It could be architected in the future to add more cache implementations besides OSCache.
 *
 * @author Igor Polevoy
 */
public enum QueryCache {
    INSTANCE;

    private final static Logger logger = LoggerFactory.getLogger(QueryCache.class);

    private final boolean enabled = Registry.instance().getConfiguration().cacheEnabled();

    private final CacheManager cacheManager;

    //singleton

    private QueryCache() {
        cacheManager = Registry.instance().getConfiguration().getCacheManager();
    }


    /**
     * This class is a singleton, get an instance with this method.
     *
     * @return one and only one instance of this class.
     */
    public static QueryCache instance() {
        return INSTANCE;
    }

    /**
     * Adds an item to cache. Expected some lists of objects returned from "select" queries.
     *
     * @param tableName - name of table.
     * @param query     query text
     * @param params    - list of parameters for a query.
     * @param cache     object to cache.
     */
    public void addItem(String tableName, String query, Object[] params, Object cache) {
        if (enabled) {
            cacheManager.addCache(tableName, getKey(tableName, query, params), cache);
        }
    }

    /**
     * Returns an item from cache, or null if nothing found.
     *
     * @param tableName name of table.
     * @param query     query text.
     * @param params    list of query parameters, can be null if no parameters are provided.
     * @return cache object or null if nothing found.
     */
    public Object getItem(String tableName, String query, Object[] params) {

        if (enabled) {
            String key = getKey(tableName, query, params);
            Object item = cacheManager.getCache(tableName, key);
            if (item == null) {
                logAccess(query, params, "MISS");
            } else {
                logAccess(query, params, "HIT");
            }
            return item;
        } else {
            return null;
        }
    }

    static void logAccess(String query, Object[] params, String access) {
        if (logger.isInfoEnabled()) {
            StringBuilder log = new StringBuilder().append(access).append(", ").append('"').append(query).append('"');
            if (!empty(params)) {
                log.append(", with parameters: ").append('<');
                join(log, params, ">, <");
                log.append('>');
            }
            LogFilter.log(logger, log.toString());
        }
    }


    private String getKey(String tableName, String query, Object[] params) {
        return new StringBuilder(tableName).append(query).append(params == null ? null : Arrays.asList(params).toString()).toString();
    }

    /**
     * This method purges (removes) all caches associated with a table, if caching is enabled and
     * a corresponding model is marked cached.
     *
     * @param tableName table name whose caches are to be purged.
     */
    public void purgeTableCache(String tableName) {
        if(enabled  && Registry.instance().getMetaModel(tableName).cached() && cacheManager.purgeEnabled()){
            cacheManager.flush(new CacheEvent(tableName, getClass().getName()));
        }
    }

    public CacheManager getCacheManager(){
        return cacheManager;
    }
}

