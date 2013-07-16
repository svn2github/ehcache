/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_5;
import net.sf.ehcache.util.lang.VicariousThreadLocal;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Assert;
import junit.framework.TestCase;

public class ThreadLocalTest extends TestCase {

  public void test() throws Throwable {
    // Construct Value instances from a foreign classloader
    ClassLoader otherClassLoader = createClassLoader();

    final AtomicReference<Class<? extends Valueable>> ref = new AtomicReference<Class<? extends Valueable>>();
    ref.set((Class<? extends Valueable>) otherClassLoader.loadClass("org.terracotta.ehcache.tests.Value"));

    VicariousThreadLocal<Valueable> threadLocal = new VicariousThreadLocal<Valueable>() {

      @Override
      protected Valueable initialValue() {
        try {
          Valueable v = ref.get().newInstance();
          v.setValue(5);
          return v;
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }

    };
    int value = threadLocal.get().value();
    Assert.assertEquals(5, value);

    System.out.println("classloader: " + threadLocal.get().getClass().getClassLoader());
    Assert.assertTrue(otherClassLoader == threadLocal.get().getClass().getClassLoader());

    WeakReference<ClassLoader> weakLoader = new WeakReference<ClassLoader>(threadLocal.get().getClass()
        .getClassLoader());

    System.out.println("Weak loader: " + weakLoader.get());

    // release all references to the classloader for gc
    otherClassLoader = null;
    ref.set(null);
    threadLocal = null;
    int count = 0;
    while (true) {
      new PermStress().stress(10000);
      for (int i = 0; i < 10; i++) {
        System.gc();
      }
      // System.out.println("threadLocal.get(): " + threadLocal);
      System.out.println("Weak loader: " + weakLoader.get());
      if (weakLoader.get() == null) {
        break;
      }
      Thread.sleep(1000);
      if (++count >= 60) { throw new AssertionError("Class loader leak"); }
    }
  }

  protected ClassLoader createClassLoader() {
    try {
      byte[] valueBytes = createValueClass();

      Map<String, byte[]> classes = new HashMap<String, byte[]>();
      classes.put("org.terracotta.ehcache.tests.Value", valueBytes);

      return new ByteClassLoader(new URL[] {}, getClass().getClassLoader(), classes);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected byte[] createValueClass() throws Exception {
    ClassWriter cw = new ClassWriter(0);
    FieldVisitor fv;
    MethodVisitor mv;

    cw.visit(V1_5, ACC_PUBLIC + ACC_SUPER, "org/terracotta/ehcache/tests/Value", null, "java/lang/Object",
             new String[] { "org/terracotta/ehcache/tests/Valueable" });

    cw.visitSource("Value.java", null);

    fv = cw.visitField(ACC_PRIVATE, "v", "I", null, null);
    fv.visitEnd();
    {
      mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
      mv.visitCode();
      Label l0 = new Label();
      mv.visitLabel(l0);
      mv.visitLineNumber(9, l0);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
      Label l1 = new Label();
      mv.visitLabel(l1);
      mv.visitLineNumber(11, l1);
      mv.visitInsn(RETURN);
      Label l2 = new Label();
      mv.visitLabel(l2);
      mv.visitLocalVariable("this", "Lorg/terracotta/ehcache/tests/Value;", null, l0, l2, 0);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }
    {
      mv = cw.visitMethod(ACC_PUBLIC, "value", "()I", null, null);
      mv.visitCode();
      Label l0 = new Label();
      mv.visitLabel(l0);
      mv.visitLineNumber(18, l0);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, "org/terracotta/ehcache/tests/Value", "v", "I");
      mv.visitInsn(IRETURN);
      Label l1 = new Label();
      mv.visitLabel(l1);
      mv.visitLocalVariable("this", "Lorg/terracotta/ehcache/tests/Value;", null, l0, l1, 0);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }
    {
      mv = cw.visitMethod(ACC_PUBLIC, "setValue", "(I)V", null, null);
      mv.visitCode();
      Label l0 = new Label();
      mv.visitLabel(l0);
      mv.visitLineNumber(22, l0);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ILOAD, 1);
      mv.visitFieldInsn(PUTFIELD, "org/terracotta/ehcache/tests/Value", "v", "I");
      Label l1 = new Label();
      mv.visitLabel(l1);
      mv.visitLineNumber(23, l1);
      mv.visitInsn(RETURN);
      Label l2 = new Label();
      mv.visitLabel(l2);
      mv.visitLocalVariable("this", "Lorg/terracotta/ehcache/tests/Value;", null, l0, l2, 0);
      mv.visitLocalVariable("v", "I", null, l0, l2, 1);
      mv.visitMaxs(2, 2);
      mv.visitEnd();
    }
    cw.visitEnd();

    return cw.toByteArray();
  }

  public static class ByteClassLoader extends URLClassLoader {
    private final Map<String, byte[]> extraClassDefs;

    public ByteClassLoader(URL[] urls, ClassLoader parent, Map<String, byte[]> extraClassDefs) {
      super(urls, parent);
      this.extraClassDefs = new HashMap<String, byte[]>(extraClassDefs);
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
      byte[] classBytes = this.extraClassDefs.remove(name);
      if (classBytes != null) { return defineClass(name, classBytes, 0, classBytes.length); }
      return super.findClass(name);
    }
  }
}
