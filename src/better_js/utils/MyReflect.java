package better_js.utils;

import better_js.*;
import better_js.reflect.*;
import mindustry.Vars;
import mindustry.android.AndroidRhinoContext.AndroidContextFactory;
import rhino.*;
import sun.misc.Unsafe;

import java.lang.reflect.*;
import java.security.ProtectionDomain;

public class MyReflect {
	public static final Unsafe unsafe = getUnsafe();
	// public static final Lookup lookup = MethodHandles.lookup();

	public static Unsafe getUnsafe() {
		// init
		try {
			Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
			theUnsafe.setAccessible(true);
			return (Unsafe) theUnsafe.get(null);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static final MyClassLoader loader = new MyClassLoader(Test.class.getClassLoader());
	public static ClassLoader IMPL_LOADER;

	// private static final Constructor<?> IMPL_CONS;
	/**
	 * 类访问标识
	 **/
	public static long classFlagsOff;

	static {
		try {
			if (Vars.mobile) {
				classFlagsOff = unsafe.objectFieldOffset(Class.class.getDeclaredField("accessFlags"));
			} else {
				Constructor<?> cons = Class.forName("jdk.internal.reflect.DelegatingClassLoader").getDeclaredConstructor(ClassLoader.class);
				cons.setAccessible(true);
				// IMPL_CONS = cons;
				IMPL_LOADER = (ClassLoader) cons.newInstance(loader);
			}
		} catch (Exception e) {
			classFlagsOff = -1;
		}
	}

	/**
	 * only for android
	 **/
	public static void setPublic(Class<?> cls) {
		if (classFlagsOff == -1) return;
		int flags = unsafe.getInt(cls, classFlagsOff);
		flags &= 0xFFFF;
		flags &= ~Modifier.FINAL;
		flags &= ~Modifier.PRIVATE;
		flags |= Modifier.PUBLIC;
		unsafe.putInt(cls, classFlagsOff, flags & 0xFFFF);
	}

	public static Class<?> defineClass(String name, Class<?> superClass, byte[] bytes) {
		/*try {
			Class.forName(superClass.getName(), false, loader);
		} catch (Throwable e) {
			loader.addChild(superClass.getClassLoader());
			Log.info("ok");
		}*/
		if (Vars.mobile) {
			int mod = superClass.getModifiers();
			if (/*Modifier.isFinal(mod) || */Modifier.isPrivate(mod)) {
				setPublic(superClass);
			}
			try {
				return ((GeneratedClassLoader)((AndroidContextFactory) ContextFactory.getGlobal())
						.createClassLoader(superClass.getClassLoader()))
						.defineClass(name, bytes);
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		} else {
			try {
				return JDKVars.unsafe.defineClass0(null, bytes, 0, bytes.length, superClass.getClassLoader(), null);
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
		// return unsafe.defineAnonymousClass(superClass, bytes, null);
	}

	public static Class<?> defineClass(ClassLoader loader, byte[] bytes, ProtectionDomain pd) {
		try {
			return JDKVars.unsafe.defineClass0(null, bytes, 0, bytes.length,
					loader, pd);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public static Class<?> defineClass(ClassLoader loader, byte[] bytes) {
		return defineClass(loader, bytes, null);
	}

	public static Class<?> getCaller() {
		Thread thread = Thread.currentThread();
		StackTraceElement[] trace = thread.getStackTrace();
		try {
			return Class.forName(trace[3].getClassName(), false, Vars.mods.mainLoader());
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	static void setValue(Object obj, Field f, Object value, boolean force) {
		Class<?> type = f.getType();
		long offset;
		if (Modifier.isStatic(f.getModifiers())) {
			obj = f.getDeclaringClass();
			offset = unsafe.staticFieldOffset(f);
		} else offset = unsafe.objectFieldOffset(f);
		if (force) {
			unsafe.putObject(obj, offset, value);
			return;
		}
		if (int.class.equals(type)) {
			unsafe.putInt(obj, offset, (int) value);
		} else if (float.class.equals(type)) {
			unsafe.putFloat(obj, offset, (float) value);
		} else if (double.class.equals(type)) {
			unsafe.putDouble(obj, offset, (double) value);
		} else if (long.class.equals(type)) {
			unsafe.putLong(obj, offset, (long) value);
		} else if (char.class.equals(type)) {
			unsafe.putChar(obj, offset, (char) value);
		} else if (byte.class.equals(type)) {
			unsafe.putByte(obj, offset, (byte) value);
		} else if (short.class.equals(type)) {
			unsafe.putShort(obj, offset, (short) value);
		} else if (boolean.class.equals(type)) {
			unsafe.putBoolean(obj, offset, (boolean) value);
		} else {
			unsafe.putObject(obj, offset, value);
		}
	}

	public static void setValue(Object obj, Field f, Object value) {
		setValue(obj, f, value, false);
	}
}