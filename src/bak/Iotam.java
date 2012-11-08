/**
 *   Copyright (c) Rich Hickey. All rights reserved.
 *   The use and distribution terms for this software are covered by the
 *   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 *   which can be found in the file epl-v10.html at the root of this distribution.
 *   By using this software in any fashion, you are agreeing to be bound by
 * 	 the terms of this license.
 *   You must not remove this notice, or any other, from this software.
 **/

/* rich Jan 1, 2009 */

package hara.data;

import java.util.concurrent.atomic.AtomicReference;
import java.util.Map;
import clojure.lang.ARef;
import clojure.lang.RT;
import clojure.lang.IPersistentMap;
import clojure.lang.PersistentList;
import clojure.lang.PersistentHashMap;
import clojure.lang.Keyword;
import clojure.lang.IFn;
import clojure.lang.ISeq;
import clojure.lang.Util;

final public class Iotam extends AIotam{
final AtomicReference state;

public Iotam(Object state){
	this.state = new AtomicReference(state);
}

public Iotam(Object state, IPersistentMap meta){
	super(meta);
	this.state = new AtomicReference(state);
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

public Object deref(){
	return state.get();
}

public Object swap(IFn f) {
	for(; ;)
		{
		Object v = deref();
		Object newv = f.invoke(v);
		validate(newv);
		if(state.compareAndSet(v, newv))
			{
			notifyWatches(v, newv, Keyword.intern(null, "swap"), f, PersistentList.EMPTY);
			return newv;
			}
		}
}

public Object swap(IFn f, Object arg) {
	for(; ;)
		{
		Object v = deref();
		Object newv = f.invoke(v, arg);
		validate(newv);
		if(state.compareAndSet(v, newv))
			{
			notifyWatches(v, newv, Keyword.intern(null, "swap"), f, RT.list(arg));
			return newv;
			}
		}
}

public Object swap(IFn f, Object arg1, Object arg2) {
	for(; ;)
		{
		Object v = deref();
		Object newv = f.invoke(v, arg1, arg2);
		validate(newv);
		if(state.compareAndSet(v, newv))
			{
			notifyWatches(v, newv, Keyword.intern(null, "swap"), f, RT.list(arg1, arg2));
			return newv;
			}
		}
}

public Object swap(IFn f, Object x, Object y, ISeq args) {
	for(; ;)
		{
		Object v = deref();
		Object newv = f.applyTo(RT.listStar(v, x, y, args));
		validate(newv);
		if(state.compareAndSet(v, newv))
			{
			notifyWatches(v, newv, Keyword.intern(null, "swap"), f, RT.listStar(x, y, args));
			return newv;
			}
		}
}

public boolean compareAndSet(Object oldv, Object newv){
	validate(newv);
	boolean ret = state.compareAndSet(oldv, newv);
	if(ret)
		notifyWatches(oldv, newv, Keyword.intern(null, "set"), null, null);
	return ret;
}

public Object reset(Object newval){
	Object oldval = state.get();
	validate(newval);
	state.set(newval);
	notifyWatches(oldval, newval, Keyword.intern(null, "reset"), null, null);
	return newval;
}

}
