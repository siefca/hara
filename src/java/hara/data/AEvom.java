/**
 *   Derived from Source Code from clojure.lang.AAtom
 *   Copyright (c) Rich Hickey. All rights reserved.
 *   The use and distribution terms for this software are covered by the
 *   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 *   which can be found in the file epl-v10.html at the root of this distribution.
 *   By using this software in any fashion, you are agreeing to be bound by
 * 	 the terms of this license.
 **/

package hara.data;

import java.util.Map;
import clojure.lang.IRef;
import clojure.lang.AReference;
import clojure.lang.RT;
import clojure.lang.Keyword;
import clojure.lang.IPersistentMap;
import clojure.lang.PersistentHashMap;
import clojure.lang.IFn;
import clojure.lang.ISeq;
import clojure.lang.Util;

public abstract class AEvom extends AReference implements IRef{
protected volatile IFn validator = null;
private volatile IPersistentMap watches = PersistentHashMap.EMPTY;

public AEvom(){
	super();
}

public AEvom(IPersistentMap meta){
	super(meta);
}

void validate(IFn vf, Object val){
	try
		{
		if(vf != null && !RT.booleanCast(vf.invoke(val)))
			throw new IllegalStateException("Invalid reference state");
		}
	catch(RuntimeException re)
		{
		throw re;
		}
	catch(Exception e)
		{
		throw new IllegalStateException("Invalid reference state", e);
		}
}

void validate(Object val){
	validate(validator, val);
}

public void setValidator(IFn vf){
	try
		{
		validate(vf, deref());
		}
	catch(Exception e)
		{
		throw Util.sneakyThrow(e);
		}
	validator = vf;
}

public IFn getValidator(){
	return validator;
}

public IPersistentMap getWatches(){
	return watches;
}

synchronized public IRef addWatch(Object key, IFn callback){
	watches = watches.assoc(key, callback);
	return this;
}

synchronized public IRef removeWatch(Object key){
	try
		{
		watches = watches.without(key);
		}
	catch(Exception e)
		{
		throw Util.sneakyThrow(e);
		}

	return this;
}

public void notifyWatches(Object oldval, Object newval, Keyword optype, IFn f, ISeq args){
	IPersistentMap ws = watches;
	if(ws.count() > 0)
		{
		for(ISeq s = ws.seq(); s != null; s = s.next())
			{
			Map.Entry e = (Map.Entry) s.first();
			IFn fn = (IFn) e.getValue();
			try
				{
				if(fn != null)
					fn.invoke(e.getKey(), this, oldval, newval, optype, f, args);
				}
			catch(Exception e1)
				{
				throw Util.sneakyThrow(e1);
				}
			}
		}
}
}
