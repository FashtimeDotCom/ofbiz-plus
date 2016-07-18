/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.ofbiz.entity.cache.redis;

import java.util.Iterator;

import org.ofbiz.base.cache.redis.UtilRedisCache;
import org.ofbiz.base.cache.redis.UtilRedisCacheFactory;
import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericEntity.NULL;
//import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.entity.GenericPK;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.model.ModelEntity;

public class EntityCache extends AbstractCache<GenericPK, GenericValue> {
    public static final String module = EntityCache.class.getName();

    public EntityCache(String delegatorName) {
        super(delegatorName, "entity");
    }

    public GenericValue get(GenericPK pk) {
        UtilRedisCache<GenericPK, GenericValue> entityCache = getCache(pk.getEntityName());
        if (entityCache == null) return null;
        GenericValue value= entityCache.get(pk);
        if(value!=null && value instanceof NULL){
        	value = GenericValue.NULL_VALUE;
        }
        return value;
    }

    public GenericValue put(GenericValue entity) {
        if (entity == null) return null;
        return put(entity.getPrimaryKey(), entity);
    }

    public GenericValue put(GenericPK pk, GenericValue entity) {
        if (pk.getModelEntity().getNeverCache()) {
            Debug.logWarning("Tried to put a value of the " + pk.getEntityName() + " entity in the BY PRIMARY KEY cache but this entity has never-cache set to true, not caching.", module);
            return null;
        }

        if (entity == null) {
            entity = GenericValue.NULL_VALUE;
        } else {
            // before going into the cache, make this value immutable
            entity.setImmutable();
        }
        UtilRedisCache<GenericPK, GenericValue> entityCache = getOrCreateCache(pk.getEntityName());
        return entityCache.put(pk, entity);
    }

    public GenericValue remove(GenericValue entity) {
        return remove(entity.getPrimaryKey());
    }

    public GenericValue remove(GenericPK pk) {
    	UtilRedisCache<GenericPK, GenericValue> entityCache = getCache(pk.getEntityName());
        if (Debug.verboseOn()) Debug.logVerbose("Removing from EntityCache with PK [" + pk + "], will remove from this cache: " + (entityCache == null ? "[No cache found to remove from]" : entityCache.getName()), module);
        if (entityCache == null) return null;
        GenericValue retVal = entityCache.remove(pk);
        ModelEntity model = pk.getModelEntity();
        if (model != null) {
            Iterator<String> it = model.getViewConvertorsIterator();
            while (it.hasNext()) {
                String targetEntityName = it.next();
                UtilRedisCacheFactory.clearCache(getCacheName(targetEntityName));
            }
        }
        if (Debug.verboseOn()) Debug.logVerbose("Removing from EntityCache with PK [" + pk + "], found this in the cache: " + retVal, module);
        return retVal;
    }
}
